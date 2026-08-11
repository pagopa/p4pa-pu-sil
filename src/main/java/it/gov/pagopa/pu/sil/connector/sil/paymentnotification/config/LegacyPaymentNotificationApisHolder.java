package it.gov.pagopa.pu.sil.connector.sil.paymentnotification.config;

import it.gov.pagopa.pu.sil.config.rest.HttpClientErrorJsonBodyHandler;
import it.gov.pagopa.sil.paymentnotificationlegacy.client.generated.DefaultApi;
import it.gov.pagopa.sil.paymentnotificationlegacy.dto.generated.PaymentNotificationError;
import it.gov.pagopa.sil.paymentnotificationlegacy.generated.ApiClient;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LegacyPaymentNotificationApisHolder {
  private final PaymentNotificationApiClientConfig clientConfig;
  private final RestTemplate restTemplate;
  private final Map<String, DefaultApi> legacyPaymentNotificationApisMap = new ConcurrentHashMap<>();
  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public LegacyPaymentNotificationApisHolder(
    PaymentNotificationApiClientConfig clientConfig,
    RestTemplateBuilder restTemplateBuilder,
    JsonMapper jsonMapper) {
    this.restTemplate = restTemplateBuilder.build();
    this.clientConfig = clientConfig;

    restTemplate.setErrorHandler(new HttpClientErrorJsonBodyHandler<>(jsonMapper, "SIL-PAYMENT-NOTIFICATION-LEGACY", clientConfig.isPrintBodyWhenError(),
      PaymentNotificationError.class, (PaymentNotificationError t) -> t.getCode().getValue(), PaymentNotificationError::getMessage)
    );
  }

  @PreDestroy
  public void unload(){
    bearerTokenHolder.remove();
  }

  public DefaultApi getPaymentNotificationLegacyApi(String accessToken, String serviceUrl) {
    bearerTokenHolder.set(accessToken);
    return legacyPaymentNotificationApisMap.computeIfAbsent(serviceUrl, url -> {
      ApiClient apiClient = new ApiClient(restTemplate);
      apiClient.setBasePath(serviceUrl);
      apiClient.setBearerToken(bearerTokenHolder::get);
      apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
      apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
      return new DefaultApi(apiClient);
    });
  }
}
