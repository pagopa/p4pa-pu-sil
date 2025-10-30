package it.gov.pagopa.pu.sil.exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

public class BufferedClientHttpResponse implements ClientHttpResponse {

  private final ClientHttpResponse originalResponse;
  private final byte[] body;

  public BufferedClientHttpResponse(ClientHttpResponse originalResponse) throws IOException {
    this.originalResponse = originalResponse;

    try (InputStream is = originalResponse.getBody()) {
      this.body = is.readAllBytes();
    }
  }

  @Override
  public InputStream getBody() throws IOException {
    return new ByteArrayInputStream(this.body);
  }


  @Override
  public HttpStatusCode getStatusCode() throws IOException {
    return this.originalResponse.getStatusCode();
  }

  @Override
  public String getStatusText() throws IOException {
    return this.originalResponse.getStatusText();
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.originalResponse.getHeaders();
  }

  @Override
  public void close() {
    this.originalResponse.close();
  }
}
