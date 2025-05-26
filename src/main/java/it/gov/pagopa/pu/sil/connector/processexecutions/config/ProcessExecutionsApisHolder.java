package it.gov.pagopa.pu.sil.connector.processexecutions.config;

import it.gov.pagopa.pu.processexecutions.controller.BaseApi;
import it.gov.pagopa.pu.processexecutions.controller.ApiClient;
import it.gov.pagopa.pu.processexecutions.controller.generated.IngestionFlowFileControllerApi;
import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ProcessExecutionsApisHolder {

  private final IngestionFlowFileControllerApi ingestionFlowFileControllerApi;

  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public ProcessExecutionsApisHolder(
    ProcessExecutionsApiClientConfig clientConfig,
    RestTemplateBuilder restTemplateBuilder
  ) {
    RestTemplate restTemplate = restTemplateBuilder.build();
    ApiClient apiClient = new ApiClient(restTemplate);
    apiClient.setBasePath(clientConfig.getBaseUrl());
    apiClient.setBearerToken(bearerTokenHolder::get);
    apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
    apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
    if (clientConfig.isPrintBodyWhenError()) {
      restTemplate.setErrorHandler(RestTemplateConfig.bodyPrinterWhenError("PROCESS-EXECUTIONS"));
    }

    this.ingestionFlowFileControllerApi = new IngestionFlowFileControllerApi(apiClient);
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

  private <T extends BaseApi> T getApi(String accessToken, T api) {
    bearerTokenHolder.set(accessToken);
    return api;
  }
}
