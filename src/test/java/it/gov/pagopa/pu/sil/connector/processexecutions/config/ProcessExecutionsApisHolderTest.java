package it.gov.pagopa.pu.sil.connector.processexecutions.config;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
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
class ProcessExecutionsApisHolderTest extends BaseApiHolderTest {
  @Mock
  private RestTemplateBuilder restTemplateBuilderMock;

  private ProcessExecutionsApisHolder processExecutionsApisHolder;
  private ProcessExecutionsApiClientConfig apiClientConfig;

  @BeforeEach
  void setUp() {
    when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
    when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
    apiClientConfig = ProcessExecutionsApiClientConfig.builder()
      .baseUrl("http://example.com")
      .maxAttempts(3)
      .build();
    processExecutionsApisHolder = new ProcessExecutionsApisHolder(apiClientConfig, restTemplateBuilderMock, new JsonConfig().objectMapperJackson3());

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
      accessToken -> processExecutionsApisHolder.getIngestionFlowFileControllerApi(accessToken)
        .createIngestionFlowFileReservation(new IngestionFlowFileRequestDTO()),
      new ParameterizedTypeReference<>() {
      }
    );
  }

  @Test
  void whenGetIngestionFlowFileControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> processExecutionsApisHolder.getIngestionFlowFileControllerApi(accessToken)
          .createIngestionFlowFileReservation(new IngestionFlowFileRequestDTO()),
      new ParameterizedTypeReference<>() {},
      processExecutionsApisHolder::unload);
  }

  @Test
  void whenGetIngestionFlowFileEntityControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken ->
        processExecutionsApisHolder.getIngestionFlowFileEntityControllerApi(accessToken)
          .crudGetIngestionflowfile("123"),
      new ParameterizedTypeReference<>() {},
      processExecutionsApisHolder::unload);
  }

  @Test
  void whenGetExportFileControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken ->
        processExecutionsApisHolder.getExportFileControllerApi(accessToken)
          .getExportFileTypeVersions("exportFileType1"),
      new ParameterizedTypeReference<>() {},
      processExecutionsApisHolder::unload);
  }

  @Test
  void whenGetExportFileEntityControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken ->
        processExecutionsApisHolder.getExportFileEntityControllerApi(accessToken)
          .crudGetExportfile("123"),
      new ParameterizedTypeReference<>() {},
      processExecutionsApisHolder::unload);
  }
}
