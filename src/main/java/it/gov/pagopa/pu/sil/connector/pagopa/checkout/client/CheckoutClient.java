package it.gov.pagopa.pu.sil.connector.pagopa.checkout.client;

import it.gov.pagopa.nodo.checkout.controller.ApiClient;
import it.gov.pagopa.nodo.checkout.controller.generated.DefaultApi;
import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.config.CheckoutApiClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Service
@Slf4j
public class CheckoutClient {

  private final DefaultApi checkoutApiClient;

  public CheckoutClient(CheckoutApiClientConfig clientConfig,
                        RestTemplateBuilder restTemplateBuilder) {
    RestTemplate restTemplate = restTemplateBuilder.build();

    if (clientConfig.isPrintBodyWhenError()) {
      restTemplate.setErrorHandler(RestTemplateConfig.bodyPrinterWhenError("PAGOPA-CHECKOUT"));
    }

    ApiClient apiClient = new ApiClient(restTemplate);
    apiClient.setBasePath(clientConfig.getBaseUrl());
    apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
    apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
    this.checkoutApiClient = new DefaultApi(apiClient);
  }

  public String checkoutCart(CartRequest cartRequest) {
    ResponseEntity<Void> responseEntity = this.checkoutApiClient.postCartsWithHttpInfo(cartRequest);
    URI location = responseEntity.getHeaders().getLocation();
    return location != null ? location.toString() : null;
  }

}
