package li.vin.bt;

import rx.Observable;

/**
 * Created by kyle on 6/20/14.
 */
public interface DeviceInterface {
  ObdPair getLatest(Pid<?> pid);

  <T> Observable<T> observe(Pid<T> pid);

  Observable<String> readChipId();
  Observable<String> readDtcCodes();
  Observable<String> readVin();
}
