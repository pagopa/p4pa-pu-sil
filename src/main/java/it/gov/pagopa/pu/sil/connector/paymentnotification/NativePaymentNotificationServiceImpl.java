package it.gov.pagopa.pu.sil.connector.paymentnotification;

import it.gov.pagopa.paymentnotification.dto.generated.PaymentNotificationRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.paymentnotification.client.NativePaymentNotificationClient;
import org.springframework.stereotype.Component;

@Component
public class NativePaymentNotificationServiceImpl implements NativePaymentNotificationService {
  private final NativePaymentNotificationClient nativePaymentNotificationClient;

  public NativePaymentNotificationServiceImpl(NativePaymentNotificationClient NativePaymentNotificationClient) {
    this.nativePaymentNotificationClient = NativePaymentNotificationClient;
  }

  @Override
  public void notifyPayment(OrgSilServiceDTO orgSilServiceDTO, UserInfo loggedUser, String accessToken, PaymentNotificationRequest paymentNotificationRequest) {
    nativePaymentNotificationClient.notifyPayment(orgSilServiceDTO, loggedUser, accessToken, paymentNotificationRequest);
  }
}
