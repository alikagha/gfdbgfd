package li.vin.bt;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import rx.Observer;

/**
 * Created by kyle on 7/9/14.
 */
/*package*/ final class CharacteristicOperatorDtc extends CharacteristicOperator<String> {
  @Override
  protected void parseCharacteristic(BluetoothGattCharacteristic characteristic, Observer<? super String> observer)
  {
    final String value = getValue(characteristic, ValType.DTC).replaceAll(STRIP_TRAILING_ZEROS_REGEX_PATTERN,"");// Make a call to the Characterisitic Operator superclass

    if (value == null || value.contains("DATA")) {
      Log.d(mTag, "no DTC Codes");
      observer.onCompleted();
      return;
    }
    
    final int valLen = value.length()-TRAILING_ERROR_CODE_LENGTH;
    final List<String> codes = new ArrayList<String>((valLen) / 2);
    for (int i = 0, lastPos = valLen; i < lastPos; i++) {
      final StringBuilder code = new StringBuilder();
      appendFirst(code, value.charAt(i++));
      code.append(value.charAt(i++));
      code.append(value.charAt(i++));
      code.append(value.charAt(i));

      final String codeStr = code.toString();
      if (EMPTY_CODE.equals(codeStr)) {
        break;
      }

      codes.add(code.toString());
      observer.onNext(code.toString());
    }
    observer.onCompleted();
  }
  private static final int TRAILING_ERROR_CODE_LENGTH = 4; // 0101 seems to trail all of
  // our input regardless of protocol.
  // This length specifies how much of it is trimmed off.
  private static final String EMPTY_CODE = "P0000";
  private static final int SKIP = 4; // skip the first 2 'bytes' because they just have the data type
  private static final String STRIP_TRAILING_ZEROS_REGEX_PATTERN = "0*$";



  private static void appendFirst(StringBuilder dtc, char c) {
    switch (c) {
      case '0': dtc.append("P0"); break;
      case '1': dtc.append("P1"); break;
      case '2': dtc.append("P2"); break;
      case '3': dtc.append("P3"); break;
      case '4': dtc.append("C0"); break;
      case '5': dtc.append("C1"); break;
      case '6': dtc.append("C2"); break;
      case '7': dtc.append("C3"); break;
      case '8': dtc.append("B0"); break;
      case '9': dtc.append("B1"); break;
      case 'A': dtc.append("B2"); break;
      case 'B': dtc.append("B3"); break;
      case 'C': dtc.append("U0"); break;
      case 'D': dtc.append("U1"); break;
      case 'E': dtc.append("U2"); break;
      case 'F': dtc.append("U3"); break;
      default: throw new RuntimeException("should not get here");
    }
  }
}
