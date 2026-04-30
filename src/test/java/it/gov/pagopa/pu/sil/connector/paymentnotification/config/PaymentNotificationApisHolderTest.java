package it.gov.pagopa.pu.sil.connector.paymentnotification.config;

import it.gov.pagopa.paymentnotification.dto.generated.PaymentDataDTO;
import it.gov.pagopa.paymentnotification.dto.generated.PaymentNotificationRequest;
import it.gov.pagopa.pu.sil.config.rest.agid.PuIntegrityDataConfig;
import it.gov.pagopa.pu.sil.connector.BaseApiHolderTest;
import it.gov.pagopa.pu.sil.util.CertUtilsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.restclient.RestTemplateBuilder;
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
    PuIntegrityDataConfig puIntegrityDataConfig = PuIntegrityDataConfig.builder()
      .privateKey(CertUtilsTest.PRIVATE_KEY)
      .build();
    apisHolder = new PaymentNotificationApisHolder(clientConfig, puIntegrityDataConfig, restTemplateBuilderMock);
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
