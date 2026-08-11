package it.gov.pagopa.pu.sil.connector.sil.paymentnotification.config;

import it.gov.pagopa.pu.sil.config.json.JsonConfig;
import it.gov.pagopa.pu.sil.config.rest.agid.AgidDataIntegrityInterceptor;
import it.gov.pagopa.pu.sil.config.rest.agid.PuIntegrityDataConfig;
import it.gov.pagopa.pu.sil.connector.BaseApiHolderTest;
import it.gov.pagopa.pu.sil.util.CertUtilsTest;
import it.gov.pagopa.sil.paymentnotification.dto.generated.PaymentDataDTO;
import it.gov.pagopa.sil.paymentnotification.dto.generated.PaymentNotificationRequest;
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
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.ArrayList;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationApisHolderTest extends BaseApiHolderTest {
  @Mock
  private RestTemplateBuilder restTemplateBuilderMock;

  private PaymentNotificationApisHolder apisHolder;

  @BeforeEach
  void setUp() {
    when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
    when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());

    ArrayList<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
    when(restTemplateMock.getInterceptors())
      .thenReturn(interceptors);

    PaymentNotificationApiClientConfig clientConfig = new PaymentNotificationApiClientConfig();
    PuIntegrityDataConfig puIntegrityDataConfig = PuIntegrityDataConfig.builder()
      .privateKey(CertUtilsTest.PRIVATE_KEY)
      .build();

    apisHolder = new PaymentNotificationApisHolder(clientConfig, puIntegrityDataConfig, restTemplateBuilderMock, new JsonConfig().objectMapperJackson3());

    verifyHttpClientErrorJsonBodyHandlerConfiguration(apisHolder.getPaymentNotificationNativeApi("accessToken", "http://example.com"));

    Assertions.assertEquals(1, interceptors.size());
    Assertions.assertInstanceOf(AgidDataIntegrityInterceptor.class, interceptors.getFirst());
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(restTemplateBuilderMock, restTemplateMock);
  }

  @Test
  void whenGetPaymentNotificationNativeApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> {
        apisHolder.getPaymentNotificationNativeApi(accessToken, "http://example.com")
          .paymentNotification(new PaymentNotificationRequest("RT123", new PaymentDataDTO()));
        return voidMock;
      },
      new ParameterizedTypeReference<>() {},
      apisHolder::unload,
      AUTH_TYPE.NO_AUTH);
  }
}
