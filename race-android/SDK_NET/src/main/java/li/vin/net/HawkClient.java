package li.vin.net;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import li.vin.hawk.Hawk;
import li.vin.hawk.HawkCredentials;
import retrofit.client.Client;
import retrofit.client.Header;
import retrofit.client.OkClient;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.mime.TypedOutput;

/**
 * Created by kyle on 6/22/14.
 * See <a href="https://github.com/square/retrofit/issues/185">here</a> for discussion on why this
 * is the best way to add Hawk to Retrofit.
 */
/*package*/ class HawkClient implements Client {
  private static final Client WRAPPED_CLIENT = new OkClient();

  private final HawkCredentials mHawkCredentials;
  private final li.vin.hawk.HawkClient mHawkClient;

  public HawkClient(String id, String key) {
    mHawkCredentials = new HawkCredentials.Builder()
      .keyId(id)
      .key(key)
      .algorithm(HawkCredentials.Algorithm.SHA256)
      .build();
    mHawkClient = new li.vin.hawk.HawkClient.Builder()
      .credentials(mHawkCredentials)
      .build();
  }

  @Override
  public Response execute(Request request) throws IOException {
    final TypedOutput toBody = request.getBody();
    String body = typedOutputToString(toBody);
    if (body != null) {
      body = Hawk.calculateBodyMac(mHawkCredentials, toBody.mimeType(), body);
    }

    final String authHeader = mHawkClient.generateAuthorizationHeader(
        URI.create(request.getUrl()), request.getMethod(), body, null, null, null);

    final List<Header> existingHeaders = request.getHeaders();
    final List<Header> newHeaders = new ArrayList<Header>(existingHeaders.size() + 1);
    newHeaders.addAll(existingHeaders);
    newHeaders.add(new Header("Authorization", authHeader));

    return WRAPPED_CLIENT.execute(
        new Request(request.getMethod(), request.getUrl(), newHeaders, toBody));
  }

  private static String typedOutputToString(TypedOutput to) throws IOException {
    if (to == null) {
      return null;
    }

    final StringOutputStream body = new StringOutputStream();
    to.writeTo(body);
    return body.toString();
  }

  private static final class StringOutputStream extends OutputStream {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final StringBuilder mSb = new StringBuilder();

    @Override public void write(byte[] buffer, int offset, int count) throws IOException {
      mSb.append(new String(buffer, offset, count, UTF8));
    }

    @Override public void write(int oneByte) throws IOException {
      throw new UnsupportedOperationException("don't write one byte at a time");
    }

    @Override public String toString() {
      return mSb.toString();
    }
  }
}
