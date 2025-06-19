package it.gov.pagopa.pu.sil.connector.debtpositions.config;

import it.gov.pagopa.pu.debtpositions.controller.ApiClient;
import it.gov.pagopa.pu.debtpositions.controller.BaseApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.DebtPositionTypeEntityControllerApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.DebtPositionTypeOrgSearchControllerApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.InstallmentApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.InstallmentNoPiiSearchControllerApi;
import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DebtPositionsApisHolder {

  private final DebtPositionTypeOrgSearchControllerApi debtPositionTypeOrgSearchControllerApi;

  private final DebtPositionTypeEntityControllerApi debtPositionTypeEntityControllerApi;

  private final InstallmentApi installmentApi;

  private final InstallmentNoPiiSearchControllerApi installmentNoPiiSearchControllerApi;

  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public DebtPositionsApisHolder(
    DebtPositionsApiClientConfig clientConfig,
    RestTemplateBuilder restTemplateBuilder
  ) {
    RestTemplate restTemplate = restTemplateBuilder.build();
    ApiClient apiClient = new ApiClient(restTemplate);
    apiClient.setBasePath(clientConfig.getBaseUrl());
    apiClient.setBearerToken(bearerTokenHolder::get);
    apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
    apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
    if (clientConfig.isPrintBodyWhenError()) {
      restTemplate.setErrorHandler(RestTemplateConfig.bodyPrinterWhenError("DEBT-POSITIONS"));
    }

    this.installmentApi = new InstallmentApi(apiClient);
    this.debtPositionTypeOrgSearchControllerApi = new DebtPositionTypeOrgSearchControllerApi(apiClient);
    this.debtPositionTypeEntityControllerApi = new DebtPositionTypeEntityControllerApi(apiClient);
    this.installmentNoPiiSearchControllerApi = new InstallmentNoPiiSearchControllerApi(apiClient);
  }

  @PreDestroy
  public void unload() {
    bearerTokenHolder.remove();
  }

  public InstallmentApi getInstallmentApi(String accessToken) {
    return getApi(accessToken, installmentApi);
  }

  public InstallmentNoPiiSearchControllerApi getInstallmentNoPiiSearchControllerApi(String accessToken) {
    return getApi(accessToken, installmentNoPiiSearchControllerApi);
  }

  public DebtPositionTypeOrgSearchControllerApi getDebtPositionTypeOrgSearchControllerApi(String accessToken) {
    return getApi(accessToken, debtPositionTypeOrgSearchControllerApi);
  }

  public DebtPositionTypeEntityControllerApi getDebtPositionTypeEntityControllerApi(String accessToken) {
    return getApi(accessToken, debtPositionTypeEntityControllerApi);
  }

  private <T extends BaseApi> T getApi(String accessToken, T api) {
    bearerTokenHolder.set(accessToken);
    return api;
  }
}
