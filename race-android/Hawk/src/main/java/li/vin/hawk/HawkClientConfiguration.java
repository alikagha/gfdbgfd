/*
 *    Copyright 2013 Weald Technology Trading Limited
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


/**
 * Configuration for a Hawk client. The Hawk client has a number of
 * configuration parameters. These are as follows:
 * <ul>
 * <li>pathPrefix: the path prefix for which the client should add authentication.  Defaults to <code>null</code> for everything</li>
 * <li>payloadValidation: if payload validation should take place.  Defaults to <code>NEVER</code></li>
 * </ul>
 * This is configured as a standard Jackson object and can be realized as part
 * of a ConfigurationSource.
 */
public final class HawkClientConfiguration implements Comparable<HawkClientConfiguration> {
  private String pathPrefix = null;

  /**
   * Create a client configuration with default values
   */
  public HawkClientConfiguration() {
    // 0-configuration
  }

  /**
   * Create a configuration with specified values for all options.
   * Note that this should not be called directly, and the Builder should be
   * used for instantiation.
   *
   * @param pathPrefix
   *          which requests to authenticate, or <code>null</code> for the default
   */
  private HawkClientConfiguration(final String pathPrefix) {
    if (this.pathPrefix == null || this.pathPrefix.startsWith("/")) {
      throw new IllegalArgumentException("Path prefix must start with \"/\" if present");
    }

    this.pathPrefix = pathPrefix;
  }

  public String getPathPrefix() {
    return this.pathPrefix;
  }

  // Standard object methods follow
  @Override public String toString() {
    return super.toString() + '{'
        + "pathPrefix=" + this.getPathPrefix() + '}';
  }

  @Override public boolean equals(final Object that) {
    return (that instanceof HawkClientConfiguration)
       && (this.compareTo((HawkClientConfiguration) that) == 0);
  }

  @Override public int hashCode() {
    return Arrays.hashCode(new Object[] {this.getPathPrefix()});
  }

  @Override public int compareTo(final HawkClientConfiguration that) {
    if (this == that) {
      return 0;
    }

    final String thisPrefix = this.getPathPrefix();
    final String thatPrefix = that.getPathPrefix();
    if (thisPrefix != null || thatPrefix != null) {
      if (thisPrefix == null) {
        return -1;
      }
      if (thatPrefix == null) {
        return 1;
      }
      final int prefixCompare = thisPrefix.compareTo(thatPrefix);
      if (prefixCompare != 0) {
        return prefixCompare;
      }
    }

    return 0;
  }

  public static class Builder {
    private String pathPrefix;

    /**
     * Generate a new builder.
     */
    public Builder() {
    }

    /**
     * Generate build with all values set from a prior configuration.
     * @param prior the prior configuration
     */
    public Builder(final HawkClientConfiguration prior) {
      this.pathPrefix = prior.pathPrefix;
    }

    /**
     * Override the default path prefix
     * @param pathPrefix the new path prefix value
     * @return The builder
     */
    public Builder pathPrefix(final String pathPrefix) {
      this.pathPrefix = pathPrefix;
      return this;
    }

    /**
     * Create a new Hawk client configuration from the defaults
     * and overrides provided.
     * @return The Hawk client configuration
     */
    public HawkClientConfiguration build() {
      return new HawkClientConfiguration(this.pathPrefix);
    }
  }
}
