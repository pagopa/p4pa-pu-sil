package it.gov.pagopa.pu.sil.util;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusModifyingClientHttpResponseWrapperTest {

  @Mock
  private ClientHttpResponse mockResponse;

  private StatusModifyingClientHttpResponseWrapper wrapper;

  @BeforeEach
  void setup() throws IOException {
    when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
    wrapper = new StatusModifyingClientHttpResponseWrapper(mockResponse);
  }

  @Test
  void getBodyReturnsWrappedBody() throws IOException {
    when(mockResponse.getBody()).thenReturn(new ByteArrayInputStream("response body".getBytes()));
    ByteArrayInputStream body = wrapper.getBody();
    assertNotNull(body);
    assertEquals("response body", new String(body.readAllBytes()));
  }

  @Test
  void getBodyReturnsCachedBodyOnSubsequentCalls() throws IOException {
    when(mockResponse.getBody()).thenReturn(new ByteArrayInputStream("response body".getBytes()));
    ByteArrayInputStream firstCall = wrapper.getBody();
    ByteArrayInputStream secondCall = wrapper.getBody();
    assertSame(firstCall, secondCall);
  }

  @Test
  void getHeadersReturnsUpdatedContentLengthWhenBodyIsSet() {
    when(mockResponse.getHeaders()).thenReturn(new HttpHeaders());
    wrapper.setBody(new ByteArrayInputStream("new body".getBytes()));
    HttpHeaders headers = wrapper.getHeaders();
    assertEquals("new body".getBytes().length, headers.getContentLength());
  }

  @Test
  void getStatusTextReturnsReasonPhraseForHttpStatus() throws IOException {
    assertEquals(HttpStatus.OK.getReasonPhrase(), wrapper.getStatusText());
  }

  @Test
  void getStatusTextReturnsToStringForCustomHttpStatusCode() throws IOException {
    HttpStatusCode status = HttpStatusCode.valueOf(999);
    wrapper.setStatusCode(status);
    assertEquals(status.toString(), wrapper.getStatusText());
  }

  @Test
  void closeClosesWrappedResponse() {
    wrapper.close();
    verify(mockResponse).close();
  }
}
