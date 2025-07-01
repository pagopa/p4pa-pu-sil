package it.gov.pagopa.pu.sil.connector.paymentnotification.config;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.sil.connector.BaseApiHolderTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.util.DefaultUriBuilderFactory;

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
    PaymentNotificationApiClientConfig clientConfig = new PaymentNotificationApiClientConfig();
    apisHolder = new PaymentNotificationApisHolder(clientConfig, restTemplateBuilderMock);
  }

  @Test
  void whenGetPaymentNotificationApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> {
        apisHolder.getPaymentNotificationLegacyApi(accessToken, "http://example.com")
          .paymentNotification(new PaymentNotification("RT123", "OK"));
        return voidMock;
      },
      new ParameterizedTypeReference<>() {},
      () -> {},
      AUTH_TYPE.NO_AUTH);
  }
}
