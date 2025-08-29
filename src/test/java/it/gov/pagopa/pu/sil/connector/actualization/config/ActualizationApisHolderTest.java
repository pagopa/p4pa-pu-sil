package it.gov.pagopa.pu.sil.connector.actualization.config;

import it.gov.pagopa.actualization.dto.generated.Payment;
import it.gov.pagopa.pu.sil.config.rest.agid.PuIntegrityDataConfig;
import it.gov.pagopa.pu.sil.connector.BaseApiHolderTest;
import it.gov.pagopa.pu.sil.util.CertUtilsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.util.DefaultUriBuilderFactory;

@ExtendWith(MockitoExtension.class)
class ActualizationApisHolderTest extends BaseApiHolderTest {
  @Mock
  private RestTemplateBuilder restTemplateBuilderMock;

  private ActualizationApisHolder apisHolder;

  @BeforeEach
  void setUp() {
    Mockito.when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
    Mockito.when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
    ActualizationApiClientConfig clientConfig = new ActualizationApiClientConfig();
    PuIntegrityDataConfig puIntegrityDataConfig = PuIntegrityDataConfig.builder()
      .privateKey(CertUtilsTest.PRIVATE_KEY)
      .build();
    apisHolder = new ActualizationApisHolder(clientConfig, puIntegrityDataConfig, restTemplateBuilderMock);
  }

  @Test
  void whenGetInstallmentApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> apisHolder.getActualizationNativeApi(accessToken, "http://example.com")
        .actualization(new Payment()),
      new ParameterizedTypeReference<>() {},
      apisHolder::unload,
      AUTH_TYPE.NO_AUTH);
  }
}
