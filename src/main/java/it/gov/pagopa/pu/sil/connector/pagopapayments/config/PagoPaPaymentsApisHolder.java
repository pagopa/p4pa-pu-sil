package it.gov.pagopa.pu.sil.connector.pagopapayments.config;

import it.gov.pagopa.pu.pagopapayments.client.generated.PrintPaymentNoticeApi;
import it.gov.pagopa.pu.pagopapayments.generated.ApiClient;
import it.gov.pagopa.pu.pagopapayments.generated.BaseApi;
import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Lazy
@Service
public class PagoPaPaymentsApisHolder {
    private final PrintPaymentNoticeApi printPaymentNoticeApi;
    private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

    public PagoPaPaymentsApisHolder(
            PagoPaPaymentsApiClientConfig clientConfig,
            RestTemplateBuilder restTemplateBuilder
    ) {
        RestTemplate restTemplate = restTemplateBuilder.build();
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(clientConfig.getBaseUrl());
        apiClient.setBearerToken(bearerTokenHolder::get);
        apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
        apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
        if (clientConfig.isPrintBodyWhenError()) {
            restTemplate.setErrorHandler(RestTemplateConfig.bodyPrinterWhenError("PAGOPA-PAYMENTS"));
        }

        this.printPaymentNoticeApi = new PrintPaymentNoticeApi(apiClient);
    }

    @PreDestroy
    public void unload(){
        bearerTokenHolder.remove();
    }

    /** It will return a {@link PrintPaymentNoticeApi} instrumented with the provided accessToken. Use null if auth is not required */
    public PrintPaymentNoticeApi getPrintPaymentNoticeApi(String accessToken) {
        return getApi(accessToken, printPaymentNoticeApi);
    }

    private <T extends BaseApi> T getApi(String accessToken, T api) {
        bearerTokenHolder.set(accessToken);
        return api;
    }
}
