package it.gov.pagopa.pu.sil.connector.pagopa.checkout.client;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.config.CheckoutApiClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URI;
import java.util.function.Supplier;

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
    Mockito.when(restTemplateBuilderMock.additionalInterceptors((ClientHttpRequestInterceptor) Mockito.any())).thenReturn(restTemplateBuilderMock);
    Mockito.when(restTemplateBuilderMock.requestFactory((Supplier<ClientHttpRequestFactory>) Mockito.any())).thenReturn(restTemplateBuilderMock);
    Mockito.when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
    checkoutClient = new CheckoutClient(apiClientConfig, restTemplateBuilderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      restTemplateBuilderMock,
      restTemplateMock
    );
  }

  @Test
  void givenValidDebtPositionWhenPaCreatePositionThenOk(){
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

}
