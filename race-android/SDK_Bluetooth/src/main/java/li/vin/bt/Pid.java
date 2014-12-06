package li.vin.bt;

/**
 * Created by kyle on 7/3/14.
 */
public abstract class Pid<T> {
  public static final Pid<Float> AIR_FLOW = new Pid<Float>("10") {
    @Override public Float parseVal(final String val) {
      final int a = Integer.parseInt(val.substring(0, 2), HEX);
      final int b = Integer.parseInt(val.substring(2, 4), HEX);

      return (a * 256f + b) / 100f;
    }
  };

  public static final Pid<Float> COOLANT_TEMP_C = new Pid<Float>("05") {
    @Override public Float parseVal(final String val) {
      final int a = Integer.parseInt(val, HEX);

      return a - 40f;
    }
  };

  public static final Pid<Float> COOLANT_TEMP_F = new Pid<Float>("05") {
    @Override public Float parseVal(final String val) {
      final int a = Integer.parseInt(val, HEX);

      return (a - 40) * 1.8f + 32;
    }
  };

  public static final Pid<Integer> ENGINE_RUNTIME = new Pid<Integer>("1F") {
    @Override public Integer parseVal(final String val) {
      final int a = Integer.parseInt(val.substring(0, 2), HEX);
      final int b = Integer.parseInt(val.substring(2, 4), HEX);

      return (a * 256) + b;
    }
  };

  public static final Pid<Float> FUEL_LEVEL = new Pid<Float>("2F") {
    @Override public Float parseVal(final String val) {
      final int a = Integer.valueOf(val, HEX);

      return (a * 100f) / 255f;
    }
  };

  public static final Pid<Float> ENGINE_LOAD = new Pid<Float>("04") {
    @Override public Float parseVal(final String val) {
      final int a = Integer.valueOf(val, HEX);

      return (a * 100f) / 255f;
    }
  };

  public static final Pid<Float> RPM = new Pid<Float>("0C") {
    @Override public Float parseVal(final String val) {
      final int a = Integer.parseInt(val.substring(0, 2), HEX);
      final int b = Integer.parseInt(val.substring(2, 4), HEX);

      return (a * 256f + b) / 4f;
    }
  };

  public static final Pid<Integer> SPEED_KPH = new Pid<Integer>("0D") {
    @Override public Integer parseVal(final String val) {
      return Integer.valueOf(val, HEX);
    }
  };

  public static final Pid<Integer> SPEED_MPH = new Pid<Integer>("0D") {
    private static final float KPH_TO_MPH = 0.621371f;

    @Override public Integer parseVal(final String val) {
      return Math.round(Integer.parseInt(val, HEX) * KPH_TO_MPH);
    }
  };

  protected static final int HEX = 16;

  private final String mCode;

  private Pid(String code) {
    mCode = code;
  }

  public final String getCode() {
    return mCode;
  }

  public final Boolean matches(final String code) {
    return mCode.equals(code) ? Boolean.TRUE : Boolean.FALSE;
  }

  public abstract T parseVal(final String val);
}
