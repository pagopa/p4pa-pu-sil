package it.gov.pagopa.pu.sil.connector.paymentnotification;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.sil.connector.paymentnotification.client.LegacyPaymentNotificationClient;
import org.springframework.stereotype.Component;

@Component
public class LegacyPaymentNotificationServiceImpl implements LegacyPaymentNotificationService {
  private final LegacyPaymentNotificationClient legacyPaymentNotificationClient;

  public LegacyPaymentNotificationServiceImpl(LegacyPaymentNotificationClient legacyPaymentNotificationClient) {
    this.legacyPaymentNotificationClient = legacyPaymentNotificationClient;
  }

  @Override
  public void notifyPayment(String accessToken, String serviceUrl, PaymentNotification paymentNotification) {
    legacyPaymentNotificationClient.notifyPayment(accessToken, serviceUrl, paymentNotification);
  }
}
