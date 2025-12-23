package it.gov.pagopa.pu.sil.connector.fileshare.config;

import it.gov.pagopa.pu.fileshare.controller.ApiClient;
import it.gov.pagopa.pu.fileshare.controller.BaseApi;
import it.gov.pagopa.pu.fileshare.controller.generated.ReceiptApi;
import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FileShareApisHolder {

  private final ReceiptApi receiptApi;

  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public FileShareApisHolder(
    FileShareApiClientConfig clientConfig,
    RestTemplateBuilder restTemplateBuilder
  ) {
    RestTemplate restTemplate = restTemplateBuilder.build();
    ApiClient apiClient = new ApiClient(restTemplate);
    apiClient.setBasePath(clientConfig.getBaseUrl());
    apiClient.setBearerToken(bearerTokenHolder::get);
    apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
    apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
    if (clientConfig.isPrintBodyWhenError()) {
      restTemplate.setErrorHandler(RestTemplateConfig.bodyPrinterWhenError("FILE-SHARE"));
    }

    this.receiptApi = new ReceiptApi(apiClient);
  }

  @PreDestroy
  public void unload() {
    bearerTokenHolder.remove();
  }

  public ReceiptApi getReceiptApi(String accessToken) {
    return getApi(accessToken, receiptApi);
  }

  private <T extends BaseApi> T getApi(String accessToken, T api) {
    bearerTokenHolder.set(accessToken);
    return api;
  }
}
