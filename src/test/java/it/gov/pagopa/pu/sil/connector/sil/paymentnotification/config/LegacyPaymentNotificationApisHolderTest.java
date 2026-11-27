package it.gov.pagopa.pu.sil.connector.sil.paymentnotification.config;

import it.gov.pagopa.pu.sil.config.json.JsonConfig;
import it.gov.pagopa.pu.sil.connector.BaseApiHolderTest;
import it.gov.pagopa.sil.paymentnotificationlegacy.dto.generated.PaymentNotification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyPaymentNotificationApisHolderTest extends BaseApiHolderTest {
  @Mock
  private RestTemplateBuilder restTemplateBuilderMock;

  private LegacyPaymentNotificationApisHolder apisHolder;
  private PaymentNotificationApiClientConfig apiClientConfig;

  @BeforeEach
  void setUp() {
    when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
    when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());

    apiClientConfig = PaymentNotificationApiClientConfig.builder()
      .baseUrl("http://example.com")
      .maxAttempts(3)
      .build();
    apisHolder = new LegacyPaymentNotificationApisHolder(apiClientConfig, restTemplateBuilderMock, new JsonConfig().objectMapperJackson3());

    verifyHttpClientErrorJsonBodyHandlerConfiguration(apisHolder.getPaymentNotificationLegacyApi(null, "http://serviceexample.com"));
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(restTemplateBuilderMock, restTemplateMock);
  }

  @Test
  void testRetryConfiguration() {
    assertRetry(apiClientConfig,
      accessToken -> {
        apisHolder.getPaymentNotificationLegacyApi(accessToken, "http://example.com")
          .paymentNotification(new PaymentNotification("RT123", "OK"));
        return voidMock;
      },
      new ParameterizedTypeReference<>() {}
    );
  }

  @Test
  void whenGetPaymentNotificationLegacyApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> {
        apisHolder.getPaymentNotificationLegacyApi(accessToken, "http://example.com")
          .paymentNotification(new PaymentNotification("RT123", "OK"));
        return voidMock;
      },
      new ParameterizedTypeReference<>() {},
      apisHolder::unload,
      AUTH_TYPE.NO_AUTH);
  }
}
