package it.gov.pagopa.pu.sil.connector.sil.paymentnotification;

import it.gov.pagopa.sil.paymentnotificationlegacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;

public interface LegacyPaymentNotificationService {
  void notifyPayment(String orgFiscalCode, OrgSilServiceDTO orgSilServiceDTO, String nav, UserInfo loggedUser, String accessToken, PaymentNotification paymentNotification);
}
