package it.gov.pagopa.pu.sil.connector.send_notification.config;

import it.gov.pagopa.pu.sendnotification.controller.ApiClient;
import it.gov.pagopa.pu.sendnotification.controller.BaseApi;
import it.gov.pagopa.pu.sendnotification.controller.generated.NotificationApi;
import it.gov.pagopa.pu.sendnotification.controller.generated.SendApi;
import it.gov.pagopa.pu.sendnotification.dto.generated.SendNotificationErrorDTO;
import it.gov.pagopa.pu.sil.config.rest.HttpClientErrorJsonBodyHandler;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

@Service
public class SendNotificationApisHolder {

  private final NotificationApi notificationApi;
  private final SendApi sendApi;

  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public SendNotificationApisHolder(
    SendNotificationApiClientConfig clientConfig,
    RestTemplateBuilder restTemplateBuilder,
    JsonMapper jsonMapper
  ) {
    RestTemplate restTemplate = restTemplateBuilder.build();
    ApiClient apiClient = new ApiClient(restTemplate);
    apiClient.setBasePath(clientConfig.getBaseUrl());
    apiClient.setBearerToken(bearerTokenHolder::get);
    apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
    apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
    restTemplate.setErrorHandler(new HttpClientErrorJsonBodyHandler<>(jsonMapper, "SEND_NOTIFICATION", clientConfig.isPrintBodyWhenError(),
      SendNotificationErrorDTO.class, SendNotificationErrorDTO::getCode, SendNotificationErrorDTO::getMessage));

    this.notificationApi = new NotificationApi(apiClient);
    this.sendApi = new SendApi(apiClient);
  }

  @PreDestroy
  public void unload() {
    bearerTokenHolder.remove();
  }

  /**
   * It will return a {@link NotificationApi} instrumented with the provided accessToken. Use null if auth is not required
   */
  public NotificationApi getNotificationApi(String accessToken) {
    return getApi(accessToken, notificationApi);
  }

  /**
   * It will return a {@link SendApi} instrumented with t
   * he provided accessToken. Use null if auth is not required
   */
  public SendApi getSendApi(String accessToken) {
    return getApi(accessToken, sendApi);
  }

  private <T extends BaseApi> T getApi(String accessToken, T api) {
    bearerTokenHolder.set(accessToken);
    return api;
  }
}
