/*
 *    Copyright 2012 Weald Technology Trading Limited
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package li.vin.hawk;

import java.security.SecureRandom;

/*package*/ final class StringUtils {
  private static final SecureRandom RANDOM_SOURCE = new SecureRandom();
  private static final String CANDIDATES =
     "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final int CANDIDATES_LEN = CANDIDATES.length();

  /**
   * Generate a random string of alphanumeric characters.
   * <p>
   * The string returned will contain characters randomly
   * selected from upper- and lower-case a through z as
   * well as the digits 0 through 9.
   * @param length the length of the string to generate
   * @return a string of random alphanumeric characters of the requested length
   */
  public static String generateRandomString(int length) {
    final StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(CANDIDATES.charAt(RANDOM_SOURCE.nextInt(CANDIDATES_LEN)));
    }
    return sb.toString();
  }

  private StringUtils() {
  }
}
