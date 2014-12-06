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

import java.util.Arrays;
import java.util.Locale;

/**
 * HawkCredentials contains the information required to authenticate requests
 * requests between a client and server.
 * <p/>
 * Note that the key in the Hawk credentials is a shared secret, and should be
 * protected accordingly.
 */
public final class HawkCredentials implements Comparable<HawkCredentials> {
  public static enum Algorithm {
    SHA1("HmacSHA1"),
    SHA256("HmacSHA256");

    private final String mJavaAlgo;

    private Algorithm(String algo) {
      mJavaAlgo = algo;
    }

    @Override public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH).replaceAll("_", "-");
    }

    public static Algorithm parse(final String algorithm) {
      try {
        return valueOf(algorithm.toUpperCase(Locale.ENGLISH).replaceAll("-", "_"));
      } catch (IllegalArgumentException iae) {
        // N.B. we don't pass the iae as the cause of this exception because
        // this happens during invocation, and in that case the enum handler
        // will report the root cause exception rather than the one we throw.
        throw new HawkError("Hawk algorithm \"" + algorithm + "\" is invalid", iae);
      }
    }

    public String getJavaAlgorithm() {
      return mJavaAlgo;
    }
  }

  private final String keyId;
  private final String key;
  private final Algorithm algorithm;

  private HawkCredentials(final String keyId, final String key, final Algorithm algorithm) {
    if (keyId == null) {
      throw new IllegalArgumentException("The key ID is required");
    }
    if (key == null) {
      throw new IllegalArgumentException("The key is required");
    }
    if (algorithm == null) {
      throw new IllegalArgumentException("The algorithm is required");
    }

    this.keyId = keyId;
    this.key = key;
    this.algorithm = algorithm;
  }

  /**
   * Obtain the key ID.
   *
   * @return the key ID
   */
  public String getKeyId() {
    return this.keyId;
  }

  /**
   * Obtain the key.
   *
   * @return the Key ID. Note that the key ID is a shared secret, and should be
   *         protected accordingly
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Obtain the algorithm used to calculate the MAC
   *
   * @return the algorithm used to calculate the MAC
   */
  public Algorithm getAlgorithm() {
    return this.algorithm;
  }

  /**
   * Obtain the algorithm used to calculate the MAC, using the name as known by
   * Java cryptography functions.
   *
   * @return the algorithm used to calculate the MAC
   */
  public String getJavaAlgorithm() {
    return this.algorithm.getJavaAlgorithm();
  }

  // Standard object methods follow
  @Override public String toString() {
    return super.toString() + '{'
        + "keyId=" + this.getKeyId()
        + " key=" + this.getKey()
        + " algorithm=" + this.getAlgorithm() + '}';
  }

  @Override public boolean equals(final Object that) {
    return (that instanceof HawkCredentials) && (this.compareTo((HawkCredentials) that) == 0);
  }

  @Override public int hashCode() {
    return Arrays.hashCode(new Object[] {this.getKeyId(), this.getKey(), this.getAlgorithm()});
  }

  @Override public int compareTo(final HawkCredentials that) {
    final int idCompare = this.getKeyId().compareTo(that.getKeyId());
    if (idCompare != 0) {
      return idCompare;
    }

    final int keyCompare = this.getKey().compareTo(that.getKey());
    if (keyCompare != 0) {
      return keyCompare;
    }

    return this.getAlgorithm().compareTo(that.getAlgorithm());
  }

  /**
   * Builder class to create a set of Hawk credentials.
   */
  public static final class Builder {
    private String keyId;
    private String key;
    private Algorithm algorithm;

    /**
     * Start a new builder.
     */
    public Builder() {
    }

    /**
     * Start a new builder, initializing the values with those from an existing
     * set of credentials.
     *
     * @param prior
     *          the prior credentials
     */
    public Builder(final HawkCredentials prior) {
      this.keyId = prior.getKeyId();
      this.key = prior.getKey();
      this.algorithm = prior.getAlgorithm();
    }

    /**
     * Set the key ID.
     *
     * @param keyId
     *          the key ID
     * @return the builder
     */
    public Builder keyId(final String keyId) {
      this.keyId = keyId;
      return this;
    }

    /**
     * Set the key.
     *
     * @param key
     *          the key
     * @return the builder
     */
    public Builder key(final String key) {
      this.key = key;
      return this;
    }

    /**
     * Set the algorithm used to calculate the MAC.
     *
     * @param algorithm
     *          the algorithm used to calculate the MAC
     * @return the builder
     */
    public Builder algorithm(final Algorithm algorithm) {
      this.algorithm = algorithm;
      return this;
    }

    /**
     * Build the Hawk credentials.
     *
     * @return the Hawk credentials
     */
    public HawkCredentials build() {
      return new HawkCredentials(this.keyId, this.key, this.algorithm);
    }
  }
}
