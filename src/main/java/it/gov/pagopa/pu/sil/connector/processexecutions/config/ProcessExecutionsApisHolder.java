package it.gov.pagopa.pu.sil.connector.processexecutions.config;

import it.gov.pagopa.pu.processexecutions.generated.ApiClient;
import it.gov.pagopa.pu.processexecutions.generated.BaseApi;
import it.gov.pagopa.pu.processexecutions.client.generated.ExportFileControllerApi;
import it.gov.pagopa.pu.processexecutions.client.generated.ExportFileEntityControllerApi;
import it.gov.pagopa.pu.processexecutions.client.generated.IngestionFlowFileControllerApi;
import it.gov.pagopa.pu.processexecutions.client.generated.IngestionFlowFileEntityControllerApi;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.sil.config.rest.HttpClientErrorJsonBodyHandler;
import it.gov.pagopa.pu.sil.connector.processexecutions.mapper.ProcessExecutionsErrorDTOMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ProcessExecutionsApisHolder {

  private final IngestionFlowFileControllerApi ingestionFlowFileControllerApi;
  private final IngestionFlowFileEntityControllerApi ingestionFlowFileEntityControllerApi;
  private final ExportFileControllerApi exportFileControllerApi;
  private final ExportFileEntityControllerApi exportFileEntityControllerApi;

  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public ProcessExecutionsApisHolder(
    ProcessExecutionsApiClientConfig clientConfig,
    RestTemplateBuilder restTemplateBuilder,
    JsonMapper jsonMapper
  ) {
    RestTemplate restTemplate = restTemplateBuilder.build();
    ApiClient apiClient = new ApiClient(restTemplate);
    apiClient.setBasePath(clientConfig.getBaseUrl());
    apiClient.setBearerToken(bearerTokenHolder::get);
    apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
    apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
    restTemplate.setErrorHandler(new HttpClientErrorJsonBodyHandler<>(jsonMapper, "PROCESS-EXECUTIONS", clientConfig.isPrintBodyWhenError(),
      ProcessExecutionsErrorDTO.class, ProcessExecutionsErrorDTOMapper::map));

    this.ingestionFlowFileControllerApi = new IngestionFlowFileControllerApi(apiClient);
    this.ingestionFlowFileEntityControllerApi = new IngestionFlowFileEntityControllerApi(apiClient);
    this.exportFileControllerApi = new ExportFileControllerApi(apiClient);
    this.exportFileEntityControllerApi = new ExportFileEntityControllerApi(apiClient);
  }

  @PreDestroy
  public void unload() {
    bearerTokenHolder.remove();
  }

  /**
   * It will return a {@link IngestionFlowFileControllerApi} instrumented with
   * the provided accessToken. Use null if auth is not required
   */
  public IngestionFlowFileControllerApi getIngestionFlowFileControllerApi(
    String accessToken) {
    return getApi(accessToken, ingestionFlowFileControllerApi);
  }

  public IngestionFlowFileEntityControllerApi getIngestionFlowFileEntityControllerApi(
    String accessToken) {
    return getApi(accessToken, ingestionFlowFileEntityControllerApi);
  }

  public ExportFileControllerApi getExportFileControllerApi(
    String accessToken) {
    return getApi(accessToken, exportFileControllerApi);
  }

  public ExportFileEntityControllerApi getExportFileEntityControllerApi(
    String accessToken) {
    return getApi(accessToken, exportFileEntityControllerApi);
  }

  private <T extends BaseApi> T getApi(String accessToken, T api) {
    bearerTokenHolder.set(accessToken);
    return api;
  }
}
