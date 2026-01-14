package it.gov.pagopa.pu.sil.connector.debtpositions.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.pu.debtpositions.controller.ApiClient;
import it.gov.pagopa.pu.debtpositions.controller.BaseApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.DebtPositionApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.DebtPositionSearchControllerApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.DebtPositionTypeEntityControllerApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.DebtPositionTypeOrgSearchControllerApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.InstallmentApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.InstallmentNoPiiSearchControllerApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.ReceiptApi;
import it.gov.pagopa.pu.sil.exception.responseerrorhandler.DebtPositionsResponseErrorHandler;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DebtPositionsApisHolder {

  private final DebtPositionTypeOrgSearchControllerApi debtPositionTypeOrgSearchControllerApi;
  private final DebtPositionTypeEntityControllerApi debtPositionTypeEntityControllerApi;

  private final InstallmentApi installmentApi;
  private final InstallmentNoPiiSearchControllerApi installmentNoPiiSearchControllerApi;

  private final DebtPositionApi debtPositionApi;
  private final DebtPositionSearchControllerApi debtPositionSearchControllerApi;

  private final ReceiptApi receiptApi;

  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public DebtPositionsApisHolder(
    DebtPositionsApiClientConfig clientConfig,
    RestTemplateBuilder restTemplateBuilder,
    ObjectMapper objectMapper
  ) {

    RestTemplate restTemplate = restTemplateBuilder.build();
    ApiClient apiClient = new ApiClient(restTemplate);
    apiClient.setBasePath(clientConfig.getBaseUrl());
    apiClient.setBearerToken(bearerTokenHolder::get);
    apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
    apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());

    restTemplate.setErrorHandler(new DebtPositionsResponseErrorHandler(clientConfig, objectMapper));

    this.debtPositionTypeOrgSearchControllerApi = new DebtPositionTypeOrgSearchControllerApi(apiClient);
    this.debtPositionTypeEntityControllerApi = new DebtPositionTypeEntityControllerApi(apiClient);

    this.installmentApi = new InstallmentApi(apiClient);
    this.installmentNoPiiSearchControllerApi = new InstallmentNoPiiSearchControllerApi(apiClient);

    this.debtPositionApi = new DebtPositionApi(apiClient);
    this.debtPositionSearchControllerApi = new DebtPositionSearchControllerApi(apiClient);

    this.receiptApi = new ReceiptApi(apiClient);
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

  public DebtPositionApi getDebtPositionApi(String accessToken) {
    return getApi(accessToken, debtPositionApi);
  }

  public DebtPositionSearchControllerApi getDebtPositionSearchControllerApi(String accessToken) {
    return getApi(accessToken, debtPositionSearchControllerApi);
  }

  public ReceiptApi getReceiptApi(String accessToken) {
    return getApi(accessToken, receiptApi);
  }

  private <T extends BaseApi> T getApi(String accessToken, T api) {
    bearerTokenHolder.set(accessToken);
    return api;
  }
}
