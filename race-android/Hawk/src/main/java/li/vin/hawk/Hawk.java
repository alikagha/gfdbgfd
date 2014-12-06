/*
 *    Copyright 2012, 2013 Weald Technology Trading Limited
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

import android.util.Base64;

import java.net.URI;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import li.vin.hawk.HawkCredentials.Algorithm;

/**
 * The Hawk class provides helper methods for calculating the MAC, required by
 * both clients and servers.
 */
public final class Hawk {
  public static final String HAWKVERSION = "1";

  protected static final long MILLISECONDS_IN_SECONDS = 1000L;

  private static final int DEFAULT_HTTP_PORT = 80;
  private static final int DEFAULT_HTTPS_PORT = 443;
  private static final Charset UTF8 = Charset.forName("UTF-8");

  /**
   * Calculate and return a MAC. The MAC is used to sign the method and
   * parameters passed as part of a request. It forms the basis to allow the
   * server to verify that the request has not been tampered with.
   * <p>
   * Note that there is no validation of the parameters except to confirm that
   * mandatory parameters are not null.
   *
   * @param credentials
   *          Hawk credentials of the requestor
   * @param authType
   *          The type of the MAC to calculate
   * @param timestamp
   *          timestamp of the request
   * @param uri
   *          URI of the request, including query parameters if appropriate
   * @param nonce
   *          nonce a random string used to uniquely identify the request
   * @param method
   *          the HTTP method of the request
   * @param hash
   *          a hash of the request's payload, or <code>null</code> if payload
   *          authentication is not required
   * @param ext
   *          optional extra data, as supplied by the requestor to differentiate
   *          the request if required
   * @param app
   *          application ID, used for Oz
   * @param dlg
   *          delegator, used for Oz
   * @return the MAC
   */
  // CHECKSTYLE.OFF: ParameterNumber - maintain legacy API
  public static String calculateMAC(final HawkCredentials credentials, final AuthType authType,
      final Long timestamp, final URI uri, final String nonce, final String method,
      final String hash, final String ext, final String app, final String dlg) {
    // CHECKSTYLE.ON: ParameterNumber
    // Check that required parameters are present
    if (credentials == null) {
        throw new IllegalArgumentException("Credentials are required but not supplied");
    }
    if (timestamp == null) {
      throw new IllegalArgumentException("Timestamp is required but not supplied");
    }
    if (uri == null) {
      throw new IllegalArgumentException("URI is required but not supplied");
    }
    if (authType == null) {
      throw new IllegalArgumentException("Authentication type is required but not supplied");
    }

    if (authType.equals(AuthType.HEADER)) {
      // Additional parameters for core authentications
      if (nonce == null) {
        throw new IllegalArgumentException("Nonce is required but not supplied");
      }
      if (method == null) {
        throw new IllegalArgumentException("Method is required but not supplied");
      }
    }

    final StringBuilder sb = new StringBuilder(1024);
    sb.append("hawk.");
    sb.append(HAWKVERSION);
    sb.append('.');
    sb.append(authType.toString());
    sb.append('\n');
    sb.append(timestamp);
    sb.append('\n');
    if (authType.equals(AuthType.HEADER)) {
      sb.append(nonce);
    }
    sb.append('\n');
    if (authType.equals(AuthType.BEWIT)) {
      sb.append("GET");
    } else {
      sb.append(method.toUpperCase(Locale.ENGLISH));
    }
    sb.append('\n');
    sb.append(uri.getRawPath());
    if (uri.getQuery() != null) {
      sb.append('?');
      sb.append(uri.getRawQuery());
    }
    sb.append('\n');
    sb.append(uri.getHost().toLowerCase(Locale.ENGLISH));
    sb.append('\n');
    sb.append(getPort(uri));
    sb.append('\n');
    if (authType.equals(AuthType.HEADER) && hash != null) {
      sb.append(hash);
    }
    sb.append('\n');
    final String checkedExt = ext == null ? "" : ext;
    sb.append(checkedExt.replace("\\", "\\\\").replace("\n", "\\n"));
    sb.append('\n');
    if (app != null) {
      sb.append(app);
      sb.append('\n');
      final String checkedDlg = dlg == null ? "" : dlg;
      sb.append(checkedDlg);
      sb.append('\n');
    }

    return calculateMac(credentials, sb.toString());
  }

  /**
   * Obtain the port of a URI.
   * @param uri the URI
   * @return The port.
   */
  private static int getPort(final URI uri) {
    int port = uri.getPort();
    if (port == -1) {
      // Default port
      if ("http".equals(uri.getScheme())) {
        port = DEFAULT_HTTP_PORT;
      } else if ("https".equals(uri.getScheme())) {
        port = DEFAULT_HTTPS_PORT;
      } else {
        throw new IllegalArgumentException("Unknown URI scheme \"" + uri.getScheme() + "\"");
      }
    }
    return port;
  }

  public static String calculateTSMac(final long curtime) {
    final HawkCredentials credentials = new HawkCredentials.Builder()
      .keyId("dummy")
      .key("dummy")
      .algorithm(Algorithm.SHA256)
      .build();
    return calculateMac(credentials, String.valueOf(curtime));
  }

  /**
   * Generate the MAC for a body with a specific content-type
   *
   * @param credentials
   *          Hawk credentials of the requestor
   * @param contentType
   *          the MIME content type
   * @param body
   *          the body
   * @return the MAC
   */
  public static String calculateBodyMac(final HawkCredentials credentials, final String contentType,
      final String body) {
    // Check that required parameters are present
    if (contentType == null) {
      throw new IllegalArgumentException("Content type is required but not supplied");
    }
    if (body == null) {
      throw new IllegalArgumentException("Body is required but not supplied");
    }

    final StringBuilder sb = new StringBuilder(1024);
    sb.append("hawk.");
    sb.append(HAWKVERSION);
    sb.append(".payload\n");
    if (contentType.indexOf(';') != -1) {
      sb.append(contentType.substring(0, contentType.indexOf(';')).toLowerCase(Locale.ENGLISH));
    } else {
      sb.append(contentType.toLowerCase(Locale.ENGLISH));
    }
    sb.append('\n');
    sb.append(body);
    sb.append('\n');

    return calculateMac(credentials, sb.toString());
  }

  /**
   * Internal method to generate the MAC given the compiled string to sign
   *
   * @param credentials
   *          Hawk credentials of the requestor
   * @param text
   *          the compiled string
   * @return the MAC
   * @throws HawkError
   *           if there is an issue with the data that prevents creation of the
   *           MAC
   */
  public static String calculateMac(final HawkCredentials credentials, final String text)
      throws HawkError {
    if (credentials == null) {
      throw new IllegalArgumentException("Credentials are required but not supplied");
    }
    if (text == null) {
      throw new IllegalArgumentException("text is required but not supplied");
    }

    try {
      Mac mac = Mac.getInstance(credentials.getJavaAlgorithm());
      mac.init(new SecretKeySpec(credentials.getKey().getBytes(UTF8), credentials.getJavaAlgorithm()));
      return new String(Base64.encode(mac.doFinal(text.getBytes(UTF8)), Base64.NO_WRAP), UTF8);
    } catch (NoSuchAlgorithmException nsae) {
      throw new HawkError("Unknown encryption algorithm", nsae);
    } catch (InvalidKeyException e) {
      throw new HawkError("Invalid key", e);
    }
  }

  /**
   * Calculate and return a bewit. The bewit is used to allow access to a resource
   * when passed to a suitable Hawk server.
   *
   * @param credentials
   *          Hawk credentials of the requestor
   * @param uri
   *          URI of the request, including query parameters if appropriate
   * @param ttl
   *          the time to live for the bewit, in seconds
   * @param ext
   *          optional extra data, as supplied by the requestor to differentiate
   *          the request if required
   * @return the MAC
   */
  public static String generateBewit(final HawkCredentials credentials, final URI uri,
      final Long ttl, final String ext) {
    if (credentials == null) {
      throw new IllegalArgumentException("Credentials are required but not supplied");
    }
    if (uri == null) {
      throw new IllegalArgumentException("URI is required but not supplied");
    }
    if (ttl == null) {
      throw new IllegalArgumentException("TTL is required but not supplied");
    }
    if (ttl > 0) {
      throw new IllegalArgumentException("TTL must be a positive value");
    }


    // Calculate expiry from ttl and current time
    Long expiry = System.currentTimeMillis() / MILLISECONDS_IN_SECONDS + ttl;
    final String mac = Hawk.calculateMAC(credentials, AuthType.BEWIT, expiry, uri, null, null,
       null, ext, null, null);

    final StringBuilder sb = new StringBuilder(256);
    sb.append(credentials.getKeyId());
    sb.append('\\');
    sb.append(String.valueOf(expiry));
    sb.append('\\');
    sb.append(mac);
    sb.append('\\');
    if (ext != null) {
      sb.append(ext);
    }

    return new String(Base64.encode(sb.toString().getBytes(UTF8), Base64.NO_WRAP), UTF8);
  }

  public static enum AuthType {
    /**
     * Authentication via an Authentication HTTP header
     */
    HEADER,
    /**
     * Authentication via a bewit query parameter
     */
    BEWIT;

    @Override public String toString() {
      return super.toString().toLowerCase(Locale.ENGLISH);
    }

    public static AuthType parse(final String authType) {
      try {
        return valueOf(authType.toUpperCase(Locale.ENGLISH));
      } catch (IllegalArgumentException iae) {
        // N.B. we don't pass the iae as the cause of this exception because
        // this happens during invocation, and in that case the enum handler
        // will report the root cause exception rather than the one we throw.
        throw new HawkError("Hawk authentication type \"" + authType + "\" is invalid", iae);
      }
    }
  }

  private Hawk() {
  }
}
