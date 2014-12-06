package li.vin.bt;

import android.content.Context;

import rx.Observable;

/**
 * Created by kyle on 6/13/14.
 */
public final class VinliDevices {
  private static final long DEFAULT_SCAN_PERIOD = 10000;

  public static Observable<Device> createDeviceObservable(final Context context) {
    return createDeviceObservable(context, DEFAULT_SCAN_PERIOD);
  }

  public static Observable<Device> createDeviceObservable(final Context context,
      final long scanTimeout) {
    if (context == null) {
      throw new IllegalArgumentException("context is null");
    }
    if (scanTimeout <= 0) {
      throw new IllegalArgumentException("scan timeout must be greater than zero");
    }

    final Context appContext = context.getApplicationContext();

    return Observable.create(new BtLeDeviceScanner(appContext, scanTimeout));
  }

  private VinliDevices() {
  }
}
