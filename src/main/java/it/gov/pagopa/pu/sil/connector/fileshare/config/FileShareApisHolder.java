package it.gov.pagopa.pu.sil.connector.fileshare.config;

import it.gov.pagopa.pu.fileshare.generated.ApiClient;
import it.gov.pagopa.pu.fileshare.generated.BaseApi;
import it.gov.pagopa.pu.fileshare.client.generated.ReceiptApi;
import it.gov.pagopa.pu.fileshare.dto.generated.FileshareErrorDTO;
import it.gov.pagopa.pu.sil.config.rest.HttpClientErrorJsonBodyHandler;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

@Service
public class FileShareApisHolder {

  private final ReceiptApi receiptApi;

  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public FileShareApisHolder(
    FileShareApiClientConfig clientConfig,
    RestTemplateBuilder restTemplateBuilder,
    JsonMapper jsonMapper
  ) {
    RestTemplate restTemplate = restTemplateBuilder.build();
    ApiClient apiClient = new ApiClient(restTemplate);
    apiClient.setBasePath(clientConfig.getBaseUrl());
    apiClient.setBearerToken(bearerTokenHolder::get);
    apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
    apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
    restTemplate.setErrorHandler(new HttpClientErrorJsonBodyHandler<>(jsonMapper, "FILE-SHARE", clientConfig.isPrintBodyWhenError(),
      FileshareErrorDTO.class, FileshareErrorDTO::getCode, FileshareErrorDTO::getMessage));

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
