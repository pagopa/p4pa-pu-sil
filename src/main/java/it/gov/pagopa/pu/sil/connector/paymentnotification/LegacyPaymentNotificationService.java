package it.gov.pagopa.pu.sil.connector.paymentnotification;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;

public interface LegacyPaymentNotificationService {
  void notifyPayment(RegistryContextData contextData, String accessToken, String serviceUrl, PaymentNotification paymentNotification);
}
