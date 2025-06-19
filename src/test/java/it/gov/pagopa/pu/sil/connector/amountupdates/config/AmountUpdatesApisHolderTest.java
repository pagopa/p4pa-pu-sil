package it.gov.pagopa.pu.sil.connector.amountupdates.config;

import it.gov.pagopa.amountupdates.legacy.dto.generated.Credentials;
import it.gov.pagopa.pu.sil.connector.BaseApiHolderTest;
import org.junit.jupiter.api.AfterEach;
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
class AmountUpdatesApisHolderTest extends BaseApiHolderTest {
  @Mock
  private RestTemplateBuilder restTemplateBuilderMock;

  private AmountUpdatesApisHolder apisHolder;

  @BeforeEach
  void setUp() {
    Mockito.when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
    Mockito.when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
    AmountUpdatesApiClientConfig clientConfig = AmountUpdatesApiClientConfig.builder()
        .baseUrl("http://example.com")
        .build();
    apisHolder = new AmountUpdatesApisHolder(clientConfig, restTemplateBuilderMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(
        restTemplateBuilderMock,
        restTemplateMock
    );
  }

  @Test
  void whenGetInstallmentApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      authUrl -> apisHolder.getAmountUpdatesLegacyApiClientByBaseUrl(authUrl)
            .login(new Credentials()),
        new ParameterizedTypeReference<>() {},
        ()-> {});
  }
}
