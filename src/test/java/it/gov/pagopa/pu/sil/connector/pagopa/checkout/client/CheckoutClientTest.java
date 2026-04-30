package it.gov.pagopa.pu.sil.connector.pagopa.checkout.client;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.config.CheckoutApiClientConfig;
import it.gov.pagopa.pu.sil.util.StatusModifyingClientHttpResponseWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;
import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CheckoutClientTest {

  @Mock
  private RestTemplateBuilder restTemplateBuilderMock;

  @Mock
  private RestTemplate restTemplateMock;

  private CheckoutClient checkoutClient;

  @BeforeEach
  void setUp() {
    CheckoutApiClientConfig apiClientConfig = new CheckoutApiClientConfig();
    apiClientConfig.setBaseUrl("http://example.com");
    Mockito.when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
    checkoutClient = new CheckoutClient(apiClientConfig, restTemplateBuilderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      restTemplateBuilderMock
    );
  }

  @Test
  void givenValidRequestWhenCheckoutCartThenOk(){
    //given
    String expectedResponse = "http://location";
    CartRequest request = new CartRequest();
    Mockito.when(restTemplateMock.exchange(Mockito.any(), Mockito.any(ParameterizedTypeReference.class)))
      .thenReturn(ResponseEntity.status(CheckoutClient.FAKE_REDIRECT_STATUS).location(URI.create(expectedResponse)).build());
    Mockito.when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());

    //when
    String response = checkoutClient.checkoutCart(request);

    //verify
    Assertions.assertSame(expectedResponse, response);
  }

  @Test
  void givenRedirectUrlNullWhenCheckoutCartThenException(){
    //given
    CartRequest request = new CartRequest();
    Mockito.when(restTemplateMock.exchange(Mockito.any(), Mockito.any(ParameterizedTypeReference.class)))
      .thenReturn(ResponseEntity.status(CheckoutClient.FAKE_REDIRECT_STATUS).build());
    Mockito.when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());

    //when
    String response = checkoutClient.checkoutCart(request);

    //verify
    Assertions.assertNull(response);
  }

  @Test
  void givenNoRedirectionWhenCheckoutCartThenException(){
    //given
    CartRequest request = new CartRequest();
    Mockito.when(restTemplateMock.exchange(Mockito.any(), Mockito.any(ParameterizedTypeReference.class)))
      .thenReturn(ResponseEntity.status(HttpStatus.OK).build());
    Mockito.when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());

    //when
    String response = checkoutClient.checkoutCart(request);

    //verify
    Assertions.assertNull(response);
  }

  @Test
  void returnsModifiedResponseFor3xxStatus() throws IOException {
    HttpRequest request = mock(HttpRequest.class);
    ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    ClientHttpResponse originalResponse = mock(ClientHttpResponse.class);

    Mockito.when(originalResponse.getStatusCode()).thenReturn(HttpStatus.FOUND);
    Mockito.when(execution.execute(any(), any())).thenReturn(originalResponse);

    ClientHttpResponse result = checkoutClient.redirectInterceptor(request, new byte[0], execution);

    Assertions.assertTrue(result instanceof StatusModifyingClientHttpResponseWrapper);
    Assertions.assertEquals(CheckoutClient.FAKE_REDIRECT_STATUS, ((StatusModifyingClientHttpResponseWrapper) result).getStatusCode());
  }

  @Test
  void returnsOriginalResponseForNon3xxStatus() throws IOException {
    HttpRequest request = mock(HttpRequest.class);
    ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    ClientHttpResponse originalResponse = mock(ClientHttpResponse.class);

    Mockito.when(originalResponse.getStatusCode()).thenReturn(HttpStatus.OK);
    Mockito.when(execution.execute(any(), any())).thenReturn(originalResponse);

    ClientHttpResponse result = checkoutClient.redirectInterceptor(request, new byte[0], execution);

    Assertions.assertSame(originalResponse, result);
  }

}
