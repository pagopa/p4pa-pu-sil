package it.gov.pagopa.pu.sil.connector.paymentnotification.config;

import it.gov.pagopa.paymentnotification.legacy.controller.ApiClient;
import it.gov.pagopa.paymentnotification.legacy.controller.generated.DefaultApi;
import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PaymentNotificationApisHolder {
  private final PaymentNotificationApiClientConfig clientConfig;
  private final RestTemplate restTemplate;
  private final Map<String, DefaultApi> paymentNotificationApisMap = new ConcurrentHashMap<>();
  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public PaymentNotificationApisHolder(PaymentNotificationApiClientConfig clientConfig,
                                       RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate = restTemplateBuilder.build();
    this.clientConfig = clientConfig;

    if (clientConfig.isPrintBodyWhenError()) {
      restTemplate.setErrorHandler(RestTemplateConfig.bodyPrinterWhenError("PAYMENT_NOTIFICATION"));
    }
  }

  @PreDestroy
  public void unload(){
    bearerTokenHolder.remove();
  }

  public DefaultApi getPaymentNotificationLegacyApi(String accessToken, String serviceUrl) {
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
