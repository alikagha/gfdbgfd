package li.vin.hawk;

/**
 * Created by kyle on 6/24/14.
 */
public class HawkError extends RuntimeException {
  public HawkError(String s) {
    super(s);
  }

  public HawkError(String s, Throwable throwable) {
    super(s, throwable);
  }
}
