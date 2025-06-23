package it.gov.pagopa.pu.sil.connector.actualization.config;

import it.gov.pagopa.actualization.legacy.controller.ApiClient;
import it.gov.pagopa.actualization.legacy.controller.generated.DefaultApi;
import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActualizationApisHolder {
  private final ActualizationApiClientConfig clientConfig;
  private final RestTemplate restTemplate;
  private final Map<String, DefaultApi> actualizationApisMap = new ConcurrentHashMap<>();

  public ActualizationApisHolder(ActualizationApiClientConfig clientConfig,
                                 RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate = restTemplateBuilder.build();
    this.clientConfig = clientConfig;

    if (clientConfig.isPrintBodyWhenError()) {
      restTemplate.setErrorHandler(RestTemplateConfig.bodyPrinterWhenError("ACTUALIZATION"));
    }
  }

  public DefaultApi getAmountUpdatesLegacyApi(String accessToken, String serviceUrl) {
    return actualizationApisMap.computeIfAbsent(serviceUrl, url -> {
      ApiClient apiClient = new ApiClient(restTemplate);
      apiClient.setBasePath(serviceUrl);
      apiClient.setBearerToken(accessToken);
      apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
      apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
      return new DefaultApi(apiClient);
    });
  }
}
