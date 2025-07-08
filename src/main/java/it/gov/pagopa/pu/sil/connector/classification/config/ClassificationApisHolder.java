package it.gov.pagopa.pu.sil.connector.classification.config;

import it.gov.pagopa.pu.classification.client.generated.AssessmentsBalanceViewSearchControllerApi;
import it.gov.pagopa.pu.classification.client.generated.PaymentsReportingSearchControllerApi;
import it.gov.pagopa.pu.classification.client.generated.TreasurySearchControllerApi;
import it.gov.pagopa.pu.classification.generated.ApiClient;
import it.gov.pagopa.pu.classification.generated.BaseApi;
import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ClassificationApisHolder {
  private final TreasurySearchControllerApi treasurySearchControllerApi;
  private final PaymentsReportingSearchControllerApi paymentsReportingSearchControllerApi;
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

    this.treasurySearchControllerApi = new TreasurySearchControllerApi(apiClient);
    this.paymentsReportingSearchControllerApi = new PaymentsReportingSearchControllerApi(apiClient);
    this.assessmentsBalanceViewSearchControllerApi = new AssessmentsBalanceViewSearchControllerApi(apiClient);
  }

  @PreDestroy
  public void unload() {
    bearerTokenHolder.remove();
  }

  public TreasurySearchControllerApi getTreasurySearchControllerApi(String accessToken) {
    return getApi(accessToken, treasurySearchControllerApi);
  }

  public PaymentsReportingSearchControllerApi getPaymentsReportingSearchControllerApi(String accessToken) {
    return getApi(accessToken, paymentsReportingSearchControllerApi);
  }

  public AssessmentsBalanceViewSearchControllerApi getAssessmentsBalanceViewSearchControllerApi(String accessToken) {
    return getApi(accessToken, assessmentsBalanceViewSearchControllerApi);
  }

  private <T extends BaseApi> T getApi(String accessToken, T api) {
    bearerTokenHolder.set(accessToken);
    return api;
  }
}
