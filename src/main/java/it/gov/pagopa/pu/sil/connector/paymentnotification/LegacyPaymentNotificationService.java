package it.gov.pagopa.pu.sil.connector.paymentnotification;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;

public interface LegacyPaymentNotificationService {
  boolean notifyPayment(String accessToken, String serviceUrl, PaymentNotification paymentNotification);
}
