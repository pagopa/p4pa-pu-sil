package it.gov.pagopa.pu.sil.connector.amountupdates.config;

import it.gov.pagopa.amountupdates.legacy.controller.ApiClient;
import it.gov.pagopa.amountupdates.legacy.controller.BaseApi;
import it.gov.pagopa.amountupdates.legacy.controller.generated.DefaultApi;
import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AmountUpdatesApisHolder {
  private final DefaultApi amountUpdatesLegacyApi;
  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public AmountUpdatesApisHolder(AmountUpdatesApiClientConfig clientConfig,
                                 RestTemplateBuilder restTemplateBuilder) {
    RestTemplate restTemplate = restTemplateBuilder.build();
    ApiClient apiClient = new ApiClient(restTemplate);
    apiClient.setBearerToken(bearerTokenHolder::get);
    apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
    apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());

    if (clientConfig.isPrintBodyWhenError()) {
      restTemplate.setErrorHandler(RestTemplateConfig.bodyPrinterWhenError("AMOUNT-UPDATES"));
    }
    this.amountUpdatesLegacyApi = new DefaultApi(apiClient);
  }

  @PreDestroy
  public void unload() {
    bearerTokenHolder.remove();
  }

  public DefaultApi getAmountUpdatesLegacyApi(String accessToken, String serviceUrl) {
    return getApi(accessToken, serviceUrl, amountUpdatesLegacyApi);
  }

  private <T extends BaseApi> T getApi(String accessToken, String serviceUrl, T api) {
    bearerTokenHolder.set(accessToken);
    api.getApiClient().setBasePath(serviceUrl);
    return api;
  }
}
