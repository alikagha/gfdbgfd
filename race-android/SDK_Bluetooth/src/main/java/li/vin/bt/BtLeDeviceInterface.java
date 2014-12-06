package li.vin.bt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.Subscriptions;

/**
 * Created by kyle on 6/20/14.
 */
/*package*/ final class BtLeDeviceInterface implements DeviceInterface {
  private final Context mContext;
  private final BluetoothDevice mDevice;
  private final BtCallback.OnDisconnected mOnDisconnected = new BtCallback.OnDisconnected() {
    @Override public void onDisconnected() {
      mBtCallback = null;
    }
  };

  private BtCallback mBtCallback;

  public BtLeDeviceInterface(Context context, BluetoothDevice device) {
    mContext = context.getApplicationContext();
    mDevice = device;
  }

  @Override public ObdPair getLatest(final Pid<?> pid) {
    if (pid == null) {
      throw new IllegalArgumentException("pid == null");
    }

    return getBtCallback().getLatest(pid);
  }

  @Override public Observable<String> readDtcCodes() {
    return getBtCallback().readCharacteristic(Uuids.DTC_CODES, new CharacteristicOperatorDtc());
  }

  @Override public Observable<String> readChipId() {
    return getBtCallback().readCharacteristic(Uuids.CHIP_ID, new CharacteristicOperatorString());
  }

  @Override public Observable<String> readVin() {
    return getBtCallback().readCharacteristic(Uuids.VIN, new CharacteristicOperatorVin());
  }

  @Override public <T> Observable<T> observe(final Pid<T> pid) {
    if (pid == null) {
      throw new IllegalArgumentException("pid == null");
    }

    return getBtCallback().observeCharacteristic(pid);
  }

  private BtCallback getBtCallback() {
    if (mBtCallback == null) {
      mBtCallback = new BtCallback(mContext, mDevice, mOnDisconnected);
    }

    return mBtCallback;
  }

  private static final class BtCallback extends BluetoothGattCallback {
    private static final String TAG = BtLeDeviceInterface.class.getSimpleName()
       + '$' + BtCallback.class.getSimpleName();
    private static final int STREAMING_VAL_START = 9;

    public static interface OnDisconnected {
      void onDisconnected();
    }

    private Observable<ObdPair> mStreamingObservable;
    private Subject<ObdPair, ObdPair> mStreamingSubject;
    private final Map<Pid<?>, Observable<?>> mStreamingObservables = new IdentityHashMap<Pid<?>, Observable<?>>();
    private final Map<String, ObdPair> mLatestStreamingValues = new HashMap<String, ObdPair>();

    private final Queue<BtWorker> mBtWorkers = new ArrayDeque<BtWorker>();

    private boolean mIsWorking = false; // protect with mBtWorkers
    private final ServiceObservable mServiceSubject;
    private final OnDisconnected mOnDisconnected;

    public BtCallback(Context context, BluetoothDevice device, OnDisconnected cb) {
      mServiceSubject = ServiceObservable.create(context, device, this);
      mOnDisconnected = cb;
    }

    @Override public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      Log.i(TAG, String.format("device(%s) onConnectionStateChange status(%s) newState(%s)",
         gatt.getDevice(), gattStatus(status), btState(newState)));

      if (BluetoothProfile.STATE_CONNECTED == newState) {
        gatt.discoverServices();
      } else if (BluetoothProfile.STATE_DISCONNECTED == newState) {
        mOnDisconnected.onDisconnected(); // TODO: this needs to be on main thread
        mServiceSubject.onCompleted();
        synchronized (mBtWorkers) {
          mBtWorkers.clear();
        }
      }
    }

    @Override public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      Log.i(TAG, String.format("device(%s) onServicesDiscovered status(%s)", gatt.getDevice(),
         gattStatus(status)));

      if (status == BluetoothGatt.GATT_SUCCESS) {
        final BluetoothGattService service = gatt.getService(Uuids.SERVICE);
        if (service == null) {
          // TODO: better error
          mServiceSubject.onError(new RuntimeException("service not found: " + Uuids.SERVICE));
        } else {
          mServiceSubject.onNext(service);
//          mServiceSubject.onCompleted(); // Do we need to complete here?
        }
      } else {
        mServiceSubject.onError(new RuntimeException("failed to find services")); // TODO: better error
      }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
        int status) {
      Log.i(TAG, String.format("device(%s) onCharacteristicRead characteristic(%s) status(%s)",
         gatt.getDevice(), characteristic.getUuid(), gattStatus(status)));

      synchronized (mBtWorkers) {
        if (mBtWorkers.isEmpty()) {
          Log.i(TAG, String.format("device(%s) onCharacteristicRead: no readers for characteristic(%s)",
             gatt.getDevice(), characteristic.getUuid()));
        } else {
          final BtWorker worker = mBtWorkers.remove();
          if (worker instanceof Reader) {
            final Reader reader = (Reader) worker;

            if (status == BluetoothGatt.GATT_SUCCESS) {
              reader.onResult(characteristic);
            } else {
              reader.onError(gattStatus(status));
            }
          } else {
            Log.d(TAG, "expected next worker to be a reader but was " + worker);
          }

          mIsWorking = false;
          startNextWork();
        }
      }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
      if (mStreamingSubject == null) {
        throw new IllegalStateException("streaming subject is null");
      }

      final byte[] val = characteristic.getValue();
      if (val == null || val[0] != '0' || val[1] != '1') {
        return; // throw away values that don't exist or that we don't care about
      }

      int valLen = 0;
      for (int i = STREAMING_VAL_START; i < val.length; i++) {
        if (val[i] == '\r') {
          break;
        }
        ++valLen;
      }

      if (valLen == 0) {
        return; // we failed to find a value
      }

      final String key = new String(val, 2, 2, CharacteristicOperator.ASCII);
      final String value = new String(val, STREAMING_VAL_START, valLen, CharacteristicOperator.ASCII);

//      Log.i(TAG, String.format("device(%s) onCharacteristicChanged key(%s) value(%s)",
//         gatt.getDevice(), key, value));

      final ObdPair existing = mLatestStreamingValues.get(key);
      if (existing == null || !value.equals(existing.getValue())) {
        final ObdPair pair = new ObdPair(key, value);
        mLatestStreamingValues.put(key, pair);
        mStreamingSubject.onNext(pair);
      }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
      Log.i(TAG, String.format("device(%s) onDescriptorWrite descriptor(%s) status(%s)",
         gatt.getDevice(), descriptor, gattStatus(status)));

      synchronized (mBtWorkers) {
        if (mBtWorkers.isEmpty()) {
          Log.i(TAG, String.format("device(%s) onDescriptorWrite: no worker for descriptor(%s)",
            gatt.getDevice(), descriptor.getUuid()));
        } else {
          final BtWorker worker = mBtWorkers.remove();
          if (worker instanceof StreamingWriter) {
            final StreamingWriter sw = (StreamingWriter) worker;

            if (status == BluetoothGatt.GATT_SUCCESS) {
              sw.onResult(descriptor);
            } else {
              sw.onError(gattStatus(status));
            }
          } else {
            Log.d(TAG, "expected next worker to be a StreamingWriter but was " + worker);
          }

          mIsWorking = false;
          startNextWork();
        }
      }
    }

    private static final class FilterCharacteristicOperator implements
        Observable.Operator<BluetoothGattCharacteristic, BluetoothGattService> {

      private final UUID mUuid;

      public FilterCharacteristicOperator(UUID uuid) {
        mUuid = uuid;
      }

      @Override
      public Subscriber<? super BluetoothGattService> call(final Subscriber<? super BluetoothGattCharacteristic> subscriber) {
        return new Subscriber<BluetoothGattService>(subscriber) {
          @Override public void onCompleted() {
            subscriber.onCompleted();
          }

          @Override public void onError(Throwable e) {
            subscriber.onError(e);
          }

          @Override public void onNext(BluetoothGattService service) {
            final BluetoothGattCharacteristic characteristic = service.getCharacteristic(mUuid);
            if (characteristic == null) {
              // TODO: better error
              subscriber.onError(new RuntimeException("no such characteristic: " + mUuid));
            } else {
              subscriber.onNext(characteristic);
            }
          }
        };
      }
    }

    private static interface BtWorker<T> {
      void start();
      void onResult(T result);
      void onError(String gattError);
    }

    private final class Reader implements BtWorker<BluetoothGattCharacteristic> {
      private final BluetoothGattCharacteristic mCharacteristic;
      public final Subject<BluetoothGattCharacteristic, BluetoothGattCharacteristic> subject;

      public Reader(BluetoothGattCharacteristic characteristic) {
        mCharacteristic = characteristic;
        subject = PublishSubject.create();
      }

      @Override public void start() {
        mServiceSubject.getGatt().readCharacteristic(mCharacteristic);
      }

      @Override public void onResult(BluetoothGattCharacteristic characteristic) {
        if (mCharacteristic.getUuid().equals(characteristic.getUuid())) {
          subject.onNext(characteristic);
          subject.onCompleted();
        } else {
          subject.onError(new RuntimeException(String.format(
            "expected to read %s but instead read %s",
            mCharacteristic.getUuid(),
            characteristic.getUuid())));
        }
      }

      @Override public void onError(String gattError) {
        subject.onError(new RuntimeException(String.format(
          "failed to read characteristic %s: %s",
          mCharacteristic,
          gattError)));
      }
    }

    private class StreamingWriter implements BtWorker<BluetoothGattDescriptor> {
      private final BluetoothGattCharacteristic mCharacteristic;

      public StreamingWriter(BluetoothGattCharacteristic characteristic) {
        mCharacteristic = characteristic;
      }

      @Override public void start() {
        final BluetoothGatt gatt = mServiceSubject.getGatt();
        if (gatt.setCharacteristicNotification(mCharacteristic, true)) {
          final BluetoothGattDescriptor descriptor =
            mCharacteristic.getDescriptor(Uuids.GATT_UUID_CHAR_CLIENT_CONFIG);
          descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

          // NOTE: Android example uses BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE but
          //  BluetoothGattDescriptor.ENABLE_INDICATION_VALUE is the only one that works on my
          //  Nexus 4

          if (!gatt.writeDescriptor(descriptor)) {
            // TODO: better error
            mStreamingSubject.onError(new RuntimeException("failed to enable streaming notifications"));
          }
        } else {
          // TODO: better error
          mStreamingSubject.onError(new RuntimeException("failed to start streaming notifications"));
        }
      }

      @Override public void onResult(BluetoothGattDescriptor descriptor) {
        if (Uuids.GATT_UUID_CHAR_CLIENT_CONFIG.equals(descriptor.getUuid())) {
          if (mStreamingSubject == null) {
            throw new IllegalStateException("streaming subject is null");
          }
        }
      }

      @Override public void onError(String gattError) {
        mStreamingSubject.onError(new RuntimeException("failed to start streaming: " + gattError));
      }
    }

    private final Func1<BluetoothGattCharacteristic, Observable<BluetoothGattCharacteristic>> readCharacteristic = new Func1<BluetoothGattCharacteristic, Observable<BluetoothGattCharacteristic>>() {
      @Override
      public Observable<BluetoothGattCharacteristic> call(BluetoothGattCharacteristic characteristic) {
        final Reader r = new Reader(characteristic);
        synchronized (mBtWorkers) {
          mBtWorkers.add(r);
          startNextWork();
        }
        return r.subject;
      }
    };

    public <T> Observable<T> readCharacteristic(final UUID uuid, CharacteristicOperator<T> operator) {
      return mServiceSubject.lift(new FilterCharacteristicOperator(uuid))
        .flatMap(readCharacteristic)
        .lift(operator);
    }

    private void startNextWork() {
      synchronized (mBtWorkers) {
        if (mIsWorking || mBtWorkers.isEmpty()) {
          return;
        }

        mBtWorkers.peek().start();
        mIsWorking = true;
      }
    }

    public <T> Observable<T> observeCharacteristic(final Pid<T> pid) {
      if (mStreamingObservables.containsKey(pid)) {
        @SuppressWarnings("unchecked")
        final Observable<T> pidObservable = (Observable<T>) mStreamingObservables.get(pid);
        return pidObservable;
      }

      if (mStreamingObservable == null) {
        final StreamingSubscriptionTracker subTracker = new StreamingSubscriptionTracker();

        mStreamingObservable = mServiceSubject
          .lift(new FilterCharacteristicOperator(Uuids.STREAMING))
          .flatMap(new Func1<BluetoothGattCharacteristic, Observable<ObdPair>>() {
            @Override
            public Observable<ObdPair> call(BluetoothGattCharacteristic characteristic) {
              if (mStreamingSubject == null) {
                mStreamingSubject = PublishSubject.create();
                subTracker.setCharacteristic(characteristic);

                final StreamingWriter sw = new StreamingWriter(characteristic);
                synchronized (mBtWorkers) {
                  mBtWorkers.add(sw);
                  startNextWork();
                }
              }

              return mStreamingSubject;
            }
          }).lift(subTracker);
      }

      final Observable<T> pidObservable = mStreamingObservable
        .filter(new Func1<ObdPair, Boolean>() {
          @Override public Boolean call(ObdPair pair) {
            return pid.matches(pair.getKey());
          }
        }).map(new Func1<ObdPair, T>() {
          @Override public T call(ObdPair pair) {
            return pid.parseVal(pair.getValue());
          }
        });

      mStreamingObservables.put(pid, pidObservable);

      return pidObservable;
    }

    public ObdPair getLatest(Pid<?> pid) {
      return mLatestStreamingValues.get(pid.getCode());
    }

    private final class StreamingSubscriptionTracker implements Action0, Observable.Operator<ObdPair, ObdPair> {
      final AtomicInteger mStreamingSubscribers = new AtomicInteger(0);

      private BluetoothGattCharacteristic mCharacteristic;

      @Override public void call() {
        final int subscriberCount = mStreamingSubscribers.decrementAndGet();
        Log.i(TAG, "removing streaming subscriber. count: " + subscriberCount);
        if (subscriberCount == 0) {
          if (mCharacteristic == null) {
            throw new IllegalStateException("the streaming characteristic has not been set");
          }
          mServiceSubject.getGatt().setCharacteristicNotification(mCharacteristic, false);
          mStreamingSubject = null; // TODO: stop getting streaming notifications
          mStreamingObservable = null;
          mStreamingObservables.clear();
        }
      }

      @Override public Subscriber<? super ObdPair> call(Subscriber<? super ObdPair> subscriber) {
        final int subscriberCount = mStreamingSubscribers.incrementAndGet();
        Log.i(TAG, "adding streaming subscriber. count: " + subscriberCount);
        subscriber.add(Subscriptions.create(this));
        return subscriber;
      }

      public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        mCharacteristic = characteristic;
      }
    }

    private static String btState(final int state) {
      switch (state) {
        case BluetoothProfile.STATE_CONNECTED: return "STATE_CONNECTED";
        case BluetoothProfile.STATE_CONNECTING: return "STATE_CONNECTING";
        case BluetoothProfile.STATE_DISCONNECTED: return "STATE_DISCONNECTED";
        case BluetoothProfile.STATE_DISCONNECTING: return "STATE_DISCONNECTING";
        default: return "Unrecognized Gatt State: " + state;
      }
    }

    private static String gattStatus(final int status) {
      switch (status) {
        case BluetoothGatt.GATT_FAILURE: return "GATT_FAILURE";
        case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION: return "GATT_INSUFFICIENT_AUTHENTICATION";
        case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION: return "GATT_INSUFFICIENT_ENCRYPTION";
        case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH: return "GATT_INVALID_ATTRIBUTE_LENGTH";
        case BluetoothGatt.GATT_INVALID_OFFSET: return "GATT_INVALID_OFFSET";
        case BluetoothGatt.GATT_READ_NOT_PERMITTED: return "GATT_READ_NOT_PERMITTED";
        case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED: return "GATT_REQUEST_NOT_SUPPORTED";
        case BluetoothGatt.GATT_SUCCESS: return "GATT_SUCCESS";
        case BluetoothGatt.GATT_WRITE_NOT_PERMITTED: return "GATT_WRITE_NOT_PERMITTED";
        default: return "Unrecognized Gatt Status: " + status;
      }
    }
  }

  private static final class ServiceObservable extends Subject<BluetoothGattService, BluetoothGattService> {
    private static final String TAG = ServiceObservable.class.getSimpleName();

    public static ServiceObservable create(Context context, BluetoothDevice device, BluetoothGattCallback callback) {
      BehaviorSubject<BluetoothGattService> subject = BehaviorSubject.create();
      ServiceObservableOnSubscribe onSubscribe =
         new ServiceObservableOnSubscribe(subject, context, device, callback);
      return new ServiceObservable(subject, onSubscribe);
    }

    private final BehaviorSubject<BluetoothGattService> mSubject;
    private final ServiceObservableOnSubscribe mOnSubscribe;

    private ServiceObservable(BehaviorSubject<BluetoothGattService> subject,
        ServiceObservableOnSubscribe onSubscribe) {
      super(onSubscribe);
      mSubject = subject;
      mOnSubscribe = onSubscribe;
    }

    @Override public void onCompleted() {
      Log.i(TAG, "onCompleted");
      mSubject.onCompleted();
    }

    @Override public void onError(Throwable e) {
      Log.i(TAG, "onError", e);
      mSubject.onError(e);
    }

    @Override public void onNext(BluetoothGattService bluetoothGattService) {
      Log.i(TAG, "onNext: " + bluetoothGattService);
      mSubject.onNext(bluetoothGattService);
    }

    public BluetoothGatt getGatt() {
      return mOnSubscribe.getGatt();
    }

    private static final class ServiceObservableOnSubscribe implements OnSubscribe<BluetoothGattService> {
      private static final String TAG = ServiceObservable.TAG
         + '$' + ServiceObservableOnSubscribe.class.getSimpleName();

      private final Context mContext;
      private final BluetoothDevice mDevice;
      private final BluetoothGattCallback mCallback;
      private final BehaviorSubject<BluetoothGattService> mSubject;
      private final AtomicInteger mSubscriberCount = new AtomicInteger(0);
      public final Object mGattLock = new Object();
      public BluetoothGatt mBluetoothGatt;

      private final Action0 mCloseAction = new Action0() {
        @Override public void call() {
          final int subscriberCount = mSubscriberCount.decrementAndGet();
          Log.i(TAG, "removing service subscriber. count: " + subscriberCount);
          if (subscriberCount == 0) {
            synchronized (mGattLock) {
              if (mBluetoothGatt != null) {
                Log.i(TAG, "closing connection to the bluetooth device");
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mBluetoothGatt = null;
              } else {
                throw new IllegalStateException("bluetooth gatt should exist");
              }
            }
          }
        }
      };

      public ServiceObservableOnSubscribe(BehaviorSubject<BluetoothGattService> subject,
          Context context, BluetoothDevice device, BluetoothGattCallback callback) {
        mSubject = subject;
        mContext = context;
        mDevice = device;
        mCallback = callback;
      }

      @Override public void call(final Subscriber<? super BluetoothGattService> subscriber) {
        final int subscriberCount = mSubscriberCount.incrementAndGet();
        Log.i(TAG, "adding service subscriber. count: " + subscriberCount);
        if (subscriberCount == 1) { // connect to the device on first subscriber
          synchronized (mGattLock) {
            if (mBluetoothGatt == null) {
              Log.i(TAG, "connecting to bluetooth device");
              mBluetoothGatt = mDevice.connectGatt(mContext, false, mCallback);
            } else {
              throw new IllegalStateException("bluetooth gatt should not exist yet");
            }
          }
        }

        subscriber.add(Subscriptions.create(mCloseAction));
        mSubject.subscribe(subscriber);
      }

      public BluetoothGatt getGatt() {
        synchronized (mGattLock) {
          if (mBluetoothGatt == null) {
            throw new IllegalStateException("the bluetooth gatt service was never created");
          }
          return mBluetoothGatt;
        }
      }
    }
  }
}
