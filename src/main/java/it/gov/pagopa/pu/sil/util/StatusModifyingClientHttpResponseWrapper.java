package it.gov.pagopa.pu.sil.util;

import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class StatusModifyingClientHttpResponseWrapper implements ClientHttpResponse {
  private final ClientHttpResponse response;
  @Setter
  private ByteArrayInputStream body;

  @Setter
  private HttpStatusCode statusCode;

  public StatusModifyingClientHttpResponseWrapper(ClientHttpResponse response) throws IOException {
    this.response = response;
    this.statusCode = response.getStatusCode();
    this.body = null;
  }

  @Override
  public ByteArrayInputStream getBody() throws IOException {
    if (this.body != null) {
      return this.body;
    }
    this.body = new ByteArrayInputStream(this.response.getBody().readAllBytes());
    return this.body;
  }

  @Override
  public HttpHeaders getHeaders() {
    if (this.body == null) {
      return this.response.getHeaders();
    }

    HttpHeaders out = new HttpHeaders(this.response.getHeaders());
    out.setContentLength(this.body.available());
    return out;
  }

  @Override
  public HttpStatusCode getStatusCode() throws IOException {
    return statusCode;
  }

  @Override
  public String getStatusText() throws IOException {
    return (this.statusCode instanceof HttpStatus status) ? status.getReasonPhrase() : this.statusCode.toString();
  }

  @Override
  public void close() {
    this.response.close();
  }
}
