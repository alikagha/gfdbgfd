package li.vin.bt;

import java.util.AbstractMap;

/**
 * Created by kyle on 7/3/14.
 */
public final class ObdPair extends AbstractMap.SimpleImmutableEntry<String, String> {

  public ObdPair(String theKey, String theValue) {
    super(theKey, theValue);
  }

}
