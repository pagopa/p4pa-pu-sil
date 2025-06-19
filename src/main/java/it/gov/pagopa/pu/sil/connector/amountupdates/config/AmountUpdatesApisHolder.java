package it.gov.pagopa.pu.sil.connector.amountupdates.config;

import it.gov.pagopa.amountupdates.legacy.controller.ApiClient;
import it.gov.pagopa.amountupdates.legacy.controller.generated.DefaultApi;
import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AmountUpdatesApisHolder {

  private final RestTemplate restTemplate;
  private final AmountUpdatesApiClientConfig clientConfig;

  private final Map<String, DefaultApi> amountUpdatesLegacyApiMap = new ConcurrentHashMap<>();

  public AmountUpdatesApisHolder(AmountUpdatesApiClientConfig clientConfig,
                                 RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate = restTemplateBuilder.build();
    this.clientConfig = clientConfig;

    if (clientConfig.isPrintBodyWhenError()) {
      restTemplate.setErrorHandler(RestTemplateConfig.bodyPrinterWhenError("AMOUNT-UPDATES"));
    }
  }

  public DefaultApi getAmountUpdatesLegacyApiClientByBaseUrl(String baseUrl) {
    return amountUpdatesLegacyApiMap.computeIfAbsent(baseUrl, url -> {
      ApiClient apiClient = new ApiClient(restTemplate);
      apiClient.setBasePath(url);
      apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
      apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
      return new DefaultApi(apiClient);
    });
  }
}



