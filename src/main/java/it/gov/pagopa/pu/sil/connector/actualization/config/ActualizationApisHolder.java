package it.gov.pagopa.pu.sil.connector.actualization.config;

import it.gov.pagopa.actualization.controller.ApiClient;
import it.gov.pagopa.actualization.controller.generated.DefaultApi;
import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import it.gov.pagopa.pu.sil.config.rest.agid.AgidDataIntegrityInterceptor;
import it.gov.pagopa.pu.sil.config.rest.agid.PuIntegrityDataConfig;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActualizationApisHolder {
  private final ActualizationApiClientConfig clientConfig;
  private final RestTemplate restTemplate;
  private final Map<String, DefaultApi> actualizationApisMap = new ConcurrentHashMap<>();
  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public ActualizationApisHolder(ActualizationApiClientConfig clientConfig,
                                 PuIntegrityDataConfig puIntegrityDataConfig,
                                 RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate = restTemplateBuilder.build();
    this.clientConfig = clientConfig;

    restTemplate.getInterceptors().add(new AgidDataIntegrityInterceptor(puIntegrityDataConfig));

    if (clientConfig.isPrintBodyWhenError()) {
      restTemplate.setErrorHandler(RestTemplateConfig.bodyPrinterWhenError("ACTUALIZATION"));
    }
  }

  @PreDestroy
  public void unload(){
    bearerTokenHolder.remove();
  }

  public DefaultApi getActualizationNativeApi(String accessToken, String serviceUrl) {
    bearerTokenHolder.set(accessToken);
    return actualizationApisMap.computeIfAbsent(serviceUrl, url -> {
      ApiClient apiClient = new ApiClient(restTemplate);
      apiClient.setBasePath(serviceUrl);
      apiClient.setBearerToken(bearerTokenHolder::get);
      apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
      apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
      return new DefaultApi(apiClient);
    });
  }
}
