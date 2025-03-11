package it.gov.pagopa.pu.sil.connector.send_notification.config;

import it.gov.pagopa.pu.sendnotification.controller.ApiClient;
import it.gov.pagopa.pu.sendnotification.controller.BaseApi;
import it.gov.pagopa.pu.sendnotification.controller.generated.NotificationApi;
import it.gov.pagopa.pu.sil.config.RestTemplateConfig;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SendNotificationApisHolder {

    private final NotificationApi notificationApi;

    private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

    public SendNotificationApisHolder(
        SendNotificationApiClientConfig clientConfig,
        RestTemplateBuilder restTemplateBuilder
    ) {
        RestTemplate restTemplate = restTemplateBuilder.build();
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(clientConfig.getBaseUrl());
        apiClient.setBearerToken(bearerTokenHolder::get);
        apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
        apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
        if (clientConfig.isPrintBodyWhenError()) {
          restTemplate.setErrorHandler(RestTemplateConfig.bodyPrinterWhenError("AUTH"));
        }

        this.notificationApi = new NotificationApi(apiClient);
    }

    @PreDestroy
    public void unload(){
        bearerTokenHolder.remove();
    }

    /** It will return a {@link NotificationApi} instrumented with the provided accessToken. Use null if auth is not required */
    public NotificationApi getNotificationApi(String accessToken){
        return getApi(accessToken, notificationApi);
    }

    private <T extends BaseApi> T getApi(String accessToken, T api) {
        bearerTokenHolder.set(accessToken);
        return api;
    }
}
