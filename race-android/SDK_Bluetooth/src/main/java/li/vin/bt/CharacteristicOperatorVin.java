package li.vin.bt;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import rx.Observer;

/**
 * Created by kyle on 7/9/14.
 */
/*package*/ final class CharacteristicOperatorVin extends CharacteristicOperator<String> {
  private static final int SKIP = 6; // skip the first 6 characters because they are just type info
  private static final int HEX = 16;

  @Override
  protected void parseCharacteristic(BluetoothGattCharacteristic characteristic, Observer<? super String> observer) {
    final String value = getValue(characteristic,ValType.VIN);

    if (value == null) {
      Log.d(mTag, "no VIN number");
      observer.onCompleted();
      return;
    }

    final StringBuilder vin = new StringBuilder();
    for (int i = SKIP, last = value.length() - 1; i < last; i += 2) {
      vin.append((char) Integer.parseInt(value.substring(i, i + 2), HEX));
    }

    observer.onNext(vin.toString());
    observer.onCompleted();
  }
}
