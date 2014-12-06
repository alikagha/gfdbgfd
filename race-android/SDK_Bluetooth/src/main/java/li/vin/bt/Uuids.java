package li.vin.bt;

import java.util.UUID;

/**
 * Created by kyle on 6/17/14.
 */
/*package*/ final class Uuids {
  public static final UUID SERVICE = UUID.fromString("e4888211-50f0-412d-9c9a-75015eb36586");

  // Characteristics
  public static final UUID CLEAR_DTC = UUID.fromString("44444444-4444-4444-4444-444444444444");

  public static final UUID DTC_CODES = UUID.fromString("22222222-2222-2222-2222-222222222222");

  public static final UUID CHIP_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

  public static final UUID VIN = UUID.fromString("33333333-3333-3333-3333-333333333333");

  public static final UUID STREAMING = UUID.fromString("11111111-1111-1111-1111-111111111111");

  public static final UUID GATT_UUID_CHAR_CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

  private Uuids() {
  }
}
