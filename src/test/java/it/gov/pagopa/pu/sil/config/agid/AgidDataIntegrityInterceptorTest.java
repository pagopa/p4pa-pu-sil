package it.gov.pagopa.pu.sil.config.agid;

import it.gov.pagopa.pu.sil.config.rest.agid.AgidDataIntegrityInterceptor;
import it.gov.pagopa.pu.sil.config.rest.agid.PuIntegrityDataConfig;
import it.gov.pagopa.pu.sil.util.AgidUtils;
import it.gov.pagopa.pu.sil.util.CertUtilsTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgidDataIntegrityInterceptorTest {

  @Mock
  private HttpRequest mockRequest;
  @Mock
  private ClientHttpRequestExecution mockExecution;

  private MockedStatic<AgidUtils> agidUtilsMockedStatic;

  @BeforeEach
  void setUp() {
    // Registering a static mock for UserService before each test
    agidUtilsMockedStatic = Mockito.mockStatic(AgidUtils.class);
  }

  @AfterEach
  void tearDown() {
    // Closing the mockStatic after each test
    agidUtilsMockedStatic.close();
  }

  @Test
  void givenRequestWhenInterceptThenUpdateHeaders() throws IOException {
    //given
    byte[] mockBody = "test body".getBytes();
    ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
    HttpHeaders mockHeaders = new HttpHeaders();

    AgidDataIntegrityInterceptor agidDataIntegrityInterceptor = new AgidDataIntegrityInterceptor(PuIntegrityDataConfig.builder()
      .privateKey(CertUtilsTest.PRIVATE_KEY)
      .build());

    when(mockExecution.execute(any(), eq(mockBody))).thenReturn(mockResponse);
    agidUtilsMockedStatic.when(() -> AgidUtils.buildDigest(new String(mockBody))).thenReturn("test-digest");
    agidUtilsMockedStatic.when(() -> AgidUtils.buildAgidJwtSignature(eq("test-digest"), any(), any())).thenReturn("test-signature");
    when(mockRequest.getHeaders()).thenReturn(mockHeaders);

    //when
    ClientHttpResponse actualResponse = agidDataIntegrityInterceptor.intercept(mockRequest, mockBody, mockExecution);

    //then
    Assertions.assertEquals(mockResponse, actualResponse);
    Assertions.assertEquals("test-digest", mockRequest.getHeaders().getFirst("Digest"));
    Assertions.assertEquals("test-signature", mockRequest.getHeaders().getFirst("Agid-JWT-Signature"));
  }

  @Test
  void testHeadersAreMutable() {
    HttpHeaders headers = new HttpHeaders();
    when(mockRequest.getHeaders()).thenReturn(headers);
    headers.add("X-Test-Header", "test-value");
    // Mutate headers after retrieval
    headers.set("X-Test-Header", "new-value");
    Assertions.assertEquals("new-value", mockRequest.getHeaders().getFirst("X-Test-Header"));
  }

}
