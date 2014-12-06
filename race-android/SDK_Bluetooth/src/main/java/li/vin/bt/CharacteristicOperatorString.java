package li.vin.bt;

import android.bluetooth.BluetoothGattCharacteristic;

import rx.Observer;

/**
 * Created by kyle on 7/9/14.
 */
/*package*/ final class CharacteristicOperatorString extends CharacteristicOperator<String> {

  @Override
  protected void parseCharacteristic(BluetoothGattCharacteristic characteristic, Observer<? super String> observer) {
    final byte[] val = characteristic.getValue();
    for (int i = 0; i < val.length; i++) {
      if (val[i] == 0) {
        observer.onNext(new String(val, 0, i, ASCII));
        break;
      }
    }

    observer.onCompleted();
  }

}
