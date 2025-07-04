package it.gov.pagopa.pu.sil.connector.paymentnotification.client;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.sil.connector.paymentnotification.config.PaymentNotificationApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LegacyPaymentNotificationClient {
  private final PaymentNotificationApisHolder paymentNotificationApisHolder;

  public LegacyPaymentNotificationClient(PaymentNotificationApisHolder paymentNotificationApisHolder) {
    this.paymentNotificationApisHolder = paymentNotificationApisHolder;
  }

  public void notifyPayment(String accessToken, String serviceUrl, PaymentNotification paymentNotification) {
    log.info("Sending payment notification to service URL: {}", serviceUrl);
    paymentNotificationApisHolder.getPaymentNotificationLegacyApi(accessToken, serviceUrl.replace("/payment-notification", ""))
      .paymentNotification(paymentNotification);
  }
}
