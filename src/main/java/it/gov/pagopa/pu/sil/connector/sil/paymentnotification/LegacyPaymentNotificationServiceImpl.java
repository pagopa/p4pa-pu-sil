package it.gov.pagopa.pu.sil.connector.sil.paymentnotification;

import it.gov.pagopa.sil.paymentnotificationlegacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.sil.paymentnotification.client.LegacyPaymentNotificationClient;
import org.springframework.stereotype.Component;

@Component
public class LegacyPaymentNotificationServiceImpl implements LegacyPaymentNotificationService {
  private final LegacyPaymentNotificationClient legacyPaymentNotificationClient;

  public LegacyPaymentNotificationServiceImpl(LegacyPaymentNotificationClient legacyPaymentNotificationClient) {
    this.legacyPaymentNotificationClient = legacyPaymentNotificationClient;
  }

  @Override
  public void notifyPayment(String orgFiscalCode, OrgSilServiceDTO orgSilServiceDTO, String nav, UserInfo loggedUser, String accessToken, PaymentNotification paymentNotification) {
    legacyPaymentNotificationClient.notifyPayment(orgFiscalCode, orgSilServiceDTO, nav, loggedUser, accessToken, paymentNotification);
  }
}
