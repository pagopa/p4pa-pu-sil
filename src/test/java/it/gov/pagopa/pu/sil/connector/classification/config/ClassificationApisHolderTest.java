package it.gov.pagopa.pu.sil.connector.classification.config;

import it.gov.pagopa.pu.sil.connector.BaseApiHolderTest;
import it.gov.pagopa.pu.sil.exception.responseerrorhandler.ClassificationResponseErrorHandler;
import it.gov.pagopa.pu.sil.exception.responseerrorhandler.DebtPositionsResponseErrorHandler;
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
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ClassificationApisHolderTest extends BaseApiHolderTest {
  @Mock
  private RestTemplateBuilder restTemplateBuilderMock;

  private ClassificationApisHolder apisHolder;

  @BeforeEach
  void setUp() {
    Mockito.when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
    Mockito.when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
    Mockito.doNothing().when(restTemplateMock).setErrorHandler(Mockito.any(
      ClassificationResponseErrorHandler.class));
    ClassificationApiClientConfig clientConfig = ClassificationApiClientConfig.builder()
        .baseUrl("http://example.com")
        .build();
    apisHolder = new ClassificationApisHolder(clientConfig, restTemplateBuilderMock, new JsonMapper());
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
        restTemplateBuilderMock,
        restTemplateMock
    );
  }

  @Test
  void whenGetAssessmentsBalanceViewSearchControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
        accessToken -> apisHolder.getAssessmentsBalanceViewSearchControllerApi(accessToken)
            .crudAssessmentsBalanceViewFindClosedByOrganizationIdAndIuf("1", "IUF1"),
        new ParameterizedTypeReference<>() {},
        apisHolder::unload);
  }
}
