package it.gov.pagopa.pu.sil.connector.paymentnotification.config;

import it.gov.pagopa.paymentnotification.dto.generated.PaymentNotificationRequest;
import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.sil.config.agid.PuIntegrityDataConfig;
import it.gov.pagopa.pu.sil.connector.BaseApiHolderTest;
import it.gov.pagopa.pu.sil.util.CertUtilsTest;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.util.DefaultUriBuilderFactory;
import uk.co.jemos.podam.api.PodamFactory;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationApisHolderTest extends BaseApiHolderTest {
  @Mock
  private RestTemplateBuilder restTemplateBuilderMock;

  private PaymentNotificationApisHolder apisHolder;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

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

  @Test
  void whenGetPaymentNotificationNativeApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> {
        apisHolder.getPaymentNotificationNativeApi(accessToken, "http://example.com")
          .paymentNotification(podamFactory.manufacturePojo(PaymentNotificationRequest.class));
        return voidMock;
      },
      new ParameterizedTypeReference<>() {},
      apisHolder::unload,
      AUTH_TYPE.NO_AUTH);
  }
}
