package it.gov.pagopa.pu.sil.connector.classification.config;

import it.gov.pagopa.pu.classification.client.generated.AssessmentsBalanceViewSearchControllerApi;
import it.gov.pagopa.pu.classification.generated.ApiClient;
import it.gov.pagopa.pu.classification.generated.BaseApi;
import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ClassificationApisHolder {
  private final AssessmentsBalanceViewSearchControllerApi assessmentsBalanceViewSearchControllerApi;

  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public ClassificationApisHolder(ClassificationApiClientConfig clientConfig,
                                  RestTemplateBuilder restTemplateBuilder) {
    RestTemplate restTemplate = restTemplateBuilder.build();
    ApiClient apiClient = new ApiClient(restTemplate);
    apiClient.setBasePath(clientConfig.getBaseUrl());
    apiClient.setBearerToken(bearerTokenHolder::get);
    apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
    apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
    if (clientConfig.isPrintBodyWhenError()) {
      restTemplate.setErrorHandler(RestTemplateConfig.bodyPrinterWhenError("CLASSIFICATION"));
    }

    this.assessmentsBalanceViewSearchControllerApi = new AssessmentsBalanceViewSearchControllerApi(apiClient);
  }

  @PreDestroy
  public void unload() {
    bearerTokenHolder.remove();
  }

  public AssessmentsBalanceViewSearchControllerApi getAssessmentsBalanceViewSearchControllerApi(String accessToken) {
    return getApi(accessToken, assessmentsBalanceViewSearchControllerApi);
  }

  private <T extends BaseApi> T getApi(String accessToken, T api) {
    bearerTokenHolder.set(accessToken);
    return api;
  }
}
