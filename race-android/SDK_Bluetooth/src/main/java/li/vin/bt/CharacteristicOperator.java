package li.vin.bt;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;

/**
 * Created by kyle on 7/9/14.
 */
/*package*/ abstract class CharacteristicOperator<T> implements Observable.Operator<T, BluetoothGattCharacteristic> {
  private static final String TAG = CharacteristicOperator.class.getSimpleName();
  public static final Charset ASCII = Charset.forName("ASCII");

  protected static enum ValType {DTC, VIN};
  protected static enum ProtocolType {CAN,KWP};
  protected final String mTag;
  public CharacteristicOperator() {mTag = getClass().getSimpleName();}

  @Override
  public Subscriber<? super BluetoothGattCharacteristic> call(final Subscriber<? super T> subscriber) {
    return new Subscriber<BluetoothGattCharacteristic>(subscriber) {
      @Override
      public void onCompleted() {
        Log.d(mTag, "onCompleted");
        subscriber.onCompleted();
      }
      @Override
      public void onError(Throwable e) {
        Log.d(mTag, "onError", e);
        subscriber.onError(e);
      }
      @Override
      public void onNext(BluetoothGattCharacteristic characteristic) {
        parseCharacteristic(characteristic, subscriber);
      }
    };
  }

  protected abstract void parseCharacteristic(BluetoothGattCharacteristic characteristic, Observer<? super T> observer);

  protected static final String getValue(BluetoothGattCharacteristic characteristic, ValType vt) {
    // A characteristic's value is a byte array containing an ASCII string split between frames.
    // Each frame starts with a ':' and ends with a '\r'.
    final byte[] bytes = characteristic.getValue();

    if (bytes == null) { // TODO: better error
      throw new RuntimeException("no value for characteristic " + characteristic.getUuid());
    }

    if (bytes[0] == 0) { // TODO: better error. A runntime Exception like this does not call onError in our observable chain.
      Log.d(TAG, Arrays.toString(bytes));
      throw new RuntimeException("value is empty for characteristic " + characteristic.getUuid());
    }

    ByteArrayOutputStream value = null;
    int spacesSkippedEachFrame = 2;
    int indexWhereCursorStarts = 1;

    switch (vt) {
      case VIN:
        // Search for either first occurrence of : or '4902' char array
        boolean recognized = false;
        for (int i = 4; i < bytes.length; i++) {
          Log.d("Vin", "||| " + String.valueOf((char) bytes[i]));
          if (bytes[i] == 0 | bytes[i] == '>') {
            break;
          }
          if (recognized) {
            if (bytes[i] == '\r') {
              recognized = false;
            } else
              value.write(bytes[i]);
          } else {
            if (bytes[i] == ':') {
              recognized = true;
              if (value == null) {
                value = new ByteArrayOutputStream();
              }
            } else if (bytes[i - 3] == '4' && bytes[i - 2] == '9' && bytes[i - 1] == '0' && bytes[i] == '2') {
              i++;
              recognized = true;
              if (value == null) {
                value = new ByteArrayOutputStream();
              }
            }
          }
        }
        break;
      case DTC:
        int starting = 1;
        //if the fourth bit is a 0. Then assume 00E on the line.  Skip 2.
        //This block of code determines where we start the framing.
        if (bytes[3] == '0') {
          Log.d("sss", "Is 0");
          indexWhereCursorStarts += 5;
        } else {
          indexWhereCursorStarts++;
        }
        for (byte x : bytes) {
          Log.d("sss", "|||||    " + String.valueOf((char) x));
        }
        //This block of code dictates framing.
        for (int i = indexWhereCursorStarts; i < bytes.length; i++) {
          if (bytes[i] == 0 | bytes[i] == '>') {
            break;
          }
          if (bytes[i] == '\r') {
            i += spacesSkippedEachFrame;
            Log.d("sss", "e <----$");
          } else {
            if (value == null) {
              value = new ByteArrayOutputStream();
            }
            Log.d("sss", "e <--" + (char) bytes[i]);
            value.write(bytes[i]);
          }
        }
        break;
      default:
        throw new RuntimeException("unknown ValType " + vt);
    }
    // if frames were found, convert the byte array into an ASCII string
    return value == null ? null : new String(value.toByteArray(), ASCII);
  }
}
