package it.gov.pagopa.pu.sil.connector.sil.paymentnotification.config;

import it.gov.pagopa.sil.paymentnotification.generated.ApiClient;
import it.gov.pagopa.sil.paymentnotification.client.generated.DefaultApi;
import it.gov.pagopa.sil.paymentnotification.dto.generated.Error;
import it.gov.pagopa.pu.sil.config.rest.HttpClientErrorJsonBodyHandler;
import it.gov.pagopa.pu.sil.config.rest.agid.AgidDataIntegrityInterceptor;
import it.gov.pagopa.pu.sil.config.rest.agid.PuIntegrityDataConfig;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PaymentNotificationApisHolder {
  private final PaymentNotificationApiClientConfig clientConfig;
  private final RestTemplate restTemplate;
  private final Map<String, DefaultApi> paymentNotificationApisMap = new ConcurrentHashMap<>();
  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public PaymentNotificationApisHolder(
    PaymentNotificationApiClientConfig clientConfig,
    PuIntegrityDataConfig puIntegrityDataConfig,
    RestTemplateBuilder restTemplateBuilder,
    JsonMapper jsonMapper
  ) {
    this.restTemplate = restTemplateBuilder.build();
    this.clientConfig = clientConfig;

    restTemplate.getInterceptors().add(new AgidDataIntegrityInterceptor(puIntegrityDataConfig));

    restTemplate.setErrorHandler(new HttpClientErrorJsonBodyHandler<>(jsonMapper, "SIL-PAYMENT-NOTIFICATION", clientConfig.isPrintBodyWhenError(),
      Error.class, Error::getFaultCode, Error::getFaultDescription)
    );
  }

  @PreDestroy
  public void unload(){
    bearerTokenHolder.remove();
  }

  public DefaultApi getPaymentNotificationNativeApi(String accessToken, String serviceUrl) {
    bearerTokenHolder.set(accessToken);
    return paymentNotificationApisMap.computeIfAbsent(serviceUrl, url -> {
      ApiClient apiClient = new ApiClient(restTemplate);
      apiClient.setBasePath(serviceUrl);
      apiClient.setBearerToken(bearerTokenHolder::get);
      apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
      apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
      return new DefaultApi(apiClient);
    });
  }
}
