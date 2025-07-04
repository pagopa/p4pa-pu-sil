package it.gov.pagopa.pu.sil.connector.paymentnotification.client;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.paymentnotification.config.PaymentNotificationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class LegacyPaymentNotificationClient {
  private final PaymentNotificationApisHolder paymentNotificationApisHolder;
  private final RegistryLogger registryLogger;

  public LegacyPaymentNotificationClient(PaymentNotificationApisHolder paymentNotificationApisHolder, RegistryLogger registryLogger) {
    this.paymentNotificationApisHolder = paymentNotificationApisHolder;
    this.registryLogger = registryLogger;
  }

  public void notifyPayment(RegistryContextData contextData, String accessToken, String serviceUrl, PaymentNotification paymentNotification) {
    log.info("Sending payment notification to service URL: {}", serviceUrl);
    registryLogger.execute(
      contextData,
      paymentNotification,
      () -> {
        paymentNotificationApisHolder.getPaymentNotificationLegacyApi(accessToken, serviceUrl)
          .paymentNotification(paymentNotification);
        return Triple.of(Void.TYPE,
          null,
          RegistryOutcome.OK
        );
      },
      null
    );
  }
}
