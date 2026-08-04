package it.gov.pagopa.pu.sil.connector.workflow.config;

import it.gov.pagopa.pu.sil.config.json.JsonConfig;
import it.gov.pagopa.pu.sil.config.rest.HttpClientErrorJsonBodyHandler;
import it.gov.pagopa.pu.sil.connector.BaseApiHolderTest;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowApisHolderTest extends BaseApiHolderTest {
  @Mock
  private RestTemplateBuilder restTemplateBuilderMock;

  private WorkflowApisHolder workflowApisHolder;
  private WorkflowApiClientConfig apiClientConfig;

  @BeforeEach
  void setUp() {
    when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
    when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
    apiClientConfig = WorkflowApiClientConfig.builder()
      .baseUrl("http://example.com")
      .maxAttempts(3)
      .build();
    workflowApisHolder = new WorkflowApisHolder(apiClientConfig, restTemplateBuilderMock, new JsonConfig().objectMapperJackson3());

    verify(restTemplateMock)
      .setErrorHandler(Mockito.any(HttpClientErrorJsonBodyHandler.class));
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      restTemplateBuilderMock,
      restTemplateMock
    );
  }

  @Test
  void testRetryConfiguration() {
    assertRetry(apiClientConfig,
      accessToken -> workflowApisHolder.getWorkflowApi(accessToken)
        .waitWorkflowCompletion("1234", 1, 1000),
      new ParameterizedTypeReference<>() {
      }
    );
  }

  @Test
  void whenDebtPositionApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> workflowApisHolder.getWorkflowApi(accessToken)
        .waitWorkflowCompletion("1234", 1, 1000),
      new ParameterizedTypeReference<>() {
      },
      workflowApisHolder::unload);
  }

}
