package li.vin.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * Created by kyle on 6/20/14.
 */
/*package*/ final class BtLeDeviceScanner implements Observable.OnSubscribe<Device>,
    BluetoothAdapter.LeScanCallback, Runnable {
  private static final String TAG = BtLeDeviceScanner.class.getSimpleName();
  private final Context mAppContext;
  private final long mScanTimeout;

  private final Handler mHandler = new Handler();
  private final List<Subscriber<? super Device>> mSubscribers =
      new ArrayList<Subscriber<? super Device>>();

  private final List<Device> mDevices = new ArrayList<Device>();

  private BluetoothAdapter mAdapter;

  public BtLeDeviceScanner(Context appContext, long scanTimeout) {
    mAppContext = appContext;
    mScanTimeout = scanTimeout;
  }

  @Override public void call(final Subscriber<? super Device> subscriber) {
    Log.d(TAG, "call()");

    if (mSubscribers.isEmpty()) { // this is the first subscriber, so set of the BT connection
      final BluetoothManager manager =
          (BluetoothManager) mAppContext.getSystemService(Context.BLUETOOTH_SERVICE);

      if (manager == null) {
        subscriber.onError(new BluetoothException("failed to get the Android bluetooth service"));
        return;
      }

      mAdapter = manager.getAdapter();

      if (mAdapter == null || !mAdapter.isEnabled()) {
        mAdapter = null;
        subscriber.onError(new BluetoothDisabledException("bluetooth is disabled"));
        return;
      }

      mAdapter.startLeScan(this);
      mHandler.postDelayed(this, mScanTimeout);
    } else if (!mDevices.isEmpty()) {
      for (Device d : mDevices) {
        subscriber.onNext(d);
      }
    }

    mSubscribers.add(subscriber);

    subscriber.add(Subscriptions.create(new Action0() {
      @Override
      public void call() {
        mSubscribers.remove(this);
        if (mSubscribers.isEmpty()) {
          mAdapter.stopLeScan(BtLeDeviceScanner.this);
          mHandler.removeCallbacks(BtLeDeviceScanner.this);
          mDevices.clear();
        }
      }
    }));
  }

  @Override public void onLeScan(BluetoothDevice device, int i, byte[] advertisedData) {
    Log.d(TAG, "Found device " + device + " UUIDs: " + Arrays.toString(device.getUuids()));

    if (isVinliDevice(advertisedData)) {
      final Device d = new Device(device);

      for (Subscriber<? super Device> subscriber : mSubscribers) {
        if (!subscriber.isUnsubscribed()) {
          subscriber.onNext(d);
        }
      }

      mDevices.add(d);
    }
  }

  @Override public void run() {
    mAdapter.stopLeScan(this);

    for (Subscriber<? super Device> subscriber : mSubscribers) {
      if (!subscriber.isUnsubscribed()) {
        subscriber.onCompleted();
      }
    }

    mSubscribers.clear();
    mDevices.clear();
  }

  /**
   * Work-around for device filtering not working for 128-bit UUIDs
   * <a href="http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation/21986475#21986475">Implementation found here</a>
   */
  private static boolean isVinliDevice(final byte[] advertisedData) {
    int offset = 0;
    while (offset < (advertisedData.length - 2)) {
      int len = advertisedData[offset++];
      if (len == 0) {
        break;
      }

      int type = advertisedData[offset++];
      switch (type) {
        case 0x02: // Partial list of 16-bit UUIDs
        case 0x03: // Complete list of 16-bit UUIDs
          while (len > 1) {
            offset += 2;
            len -= 2;
          }
          break;
        case 0x06:// Partial list of 128-bit UUIDs
        case 0x07:// Complete list of 128-bit UUIDs
          // Loop through the advertised 128-bit UUID's.
          while (len >= 16) {
            try {
              // Wrap the advertised bits and order them.
              final ByteBuffer buffer = ByteBuffer.wrap(advertisedData,
                 offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
              final long mostSignificantBit = buffer.getLong();
              final long leastSignificantBit = buffer.getLong();
              final UUID uuid = new UUID(leastSignificantBit, mostSignificantBit);
              if (uuid.equals(Uuids.SERVICE)) {
                return true;
              }
            } catch (IndexOutOfBoundsException e) {
              Log.e(TAG, "Failed to filter out only Vinli devices", e);
            } finally {
              // Move the offset to read the next uuid.
              offset += 15;
              len -= 16;
            }
          }
          break;
        default:
          offset += (len - 1);
          break;
      }
    }

    return false;
  }
}
