package it.gov.pagopa.pu.sil.connector.pagopapayments.config;

import it.gov.pagopa.pu.pagopapayments.client.generated.PrintPaymentNoticeApi;
import it.gov.pagopa.pu.pagopapayments.dto.generated.PagoPaPaymentsErrorDTO;
import it.gov.pagopa.pu.pagopapayments.generated.ApiClient;
import it.gov.pagopa.pu.pagopapayments.generated.BaseApi;
import it.gov.pagopa.pu.sil.config.rest.HttpClientErrorJsonBodyHandler;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

@Lazy
@Service
public class PagoPaPaymentsApisHolder {
  private final PrintPaymentNoticeApi printPaymentNoticeApi;
  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public PagoPaPaymentsApisHolder(
    PagoPaPaymentsApiClientConfig clientConfig,
    RestTemplateBuilder restTemplateBuilder,
    JsonMapper jsonMapper
  ) {
    RestTemplate restTemplate = restTemplateBuilder.build();
    ApiClient apiClient = new ApiClient(restTemplate);
    apiClient.setBasePath(clientConfig.getBaseUrl());
    apiClient.setBearerToken(bearerTokenHolder::get);
    apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
    apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
    restTemplate.setErrorHandler(new HttpClientErrorJsonBodyHandler<>(jsonMapper, "PAGOPA-PAYMENTS", clientConfig.isPrintBodyWhenError(),
      PagoPaPaymentsErrorDTO.class, PagoPaPaymentsErrorDTO::getCode, PagoPaPaymentsErrorDTO::getMessage));

    this.printPaymentNoticeApi = new PrintPaymentNoticeApi(apiClient);
  }

  @PreDestroy
  public void unload() {
    bearerTokenHolder.remove();
  }

  /**
   * It will return a {@link PrintPaymentNoticeApi} instrumented with the provided accessToken. Use null if auth is not required
   */
  public PrintPaymentNoticeApi getPrintPaymentNoticeApi(String accessToken) {
    return getApi(accessToken, printPaymentNoticeApi);
  }

  private <T extends BaseApi> T getApi(String accessToken, T api) {
    bearerTokenHolder.set(accessToken);
    return api;
  }
}
