package it.gov.pagopa.pu.sil.connector.sil.paymentnotification;

import it.gov.pagopa.sil.paymentnotification.dto.generated.PaymentNotificationRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;

public interface NativePaymentNotificationService {
  void notifyPayment(OrgSilServiceDTO orgSilServiceDTO, UserInfo loggedUser, String accessToken, PaymentNotificationRequest paymentNotificationRequest);
}
