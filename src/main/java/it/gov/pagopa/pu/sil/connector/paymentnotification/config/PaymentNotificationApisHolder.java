package it.gov.pagopa.pu.sil.connector.paymentnotification.config;

import it.gov.pagopa.paymentnotification.controller.ApiClient;
import it.gov.pagopa.paymentnotification.controller.generated.DefaultApi;
import it.gov.pagopa.pu.sil.config.rest.agid.AgidDataIntegrityInterceptor;
import it.gov.pagopa.pu.sil.config.rest.agid.PuIntegrityDataConfig;
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
                                       PuIntegrityDataConfig puIntegrityDataConfig,
                                       RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate = restTemplateBuilder.build();
    this.clientConfig = clientConfig;

    restTemplate.getInterceptors().add(new AgidDataIntegrityInterceptor(puIntegrityDataConfig));

    if (clientConfig.isPrintBodyWhenError()) {
      restTemplate.setErrorHandler(RestTemplateConfig.bodyPrinterWhenError("PAYMENT_NOTIFICATION"));
    }
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
