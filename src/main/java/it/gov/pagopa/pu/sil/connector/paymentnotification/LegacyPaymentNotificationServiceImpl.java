package it.gov.pagopa.pu.sil.connector.paymentnotification;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.sil.connector.paymentnotification.client.LegacyPaymentNotificationClient;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import org.springframework.stereotype.Component;

@Component
public class LegacyPaymentNotificationServiceImpl implements LegacyPaymentNotificationService {
  private final LegacyPaymentNotificationClient legacyPaymentNotificationClient;

  public LegacyPaymentNotificationServiceImpl(LegacyPaymentNotificationClient legacyPaymentNotificationClient) {
    this.legacyPaymentNotificationClient = legacyPaymentNotificationClient;
  }

  @Override
  public void notifyPayment(RegistryContextData contextData, String accessToken, String serviceUrl, PaymentNotification paymentNotification) {
    legacyPaymentNotificationClient.notifyPayment(contextData, accessToken, serviceUrl, paymentNotification);
  }
}
