package it.gov.pagopa.pu.sil.service.paymentnotification;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.connector.paymentnotification.LegacyPaymentNotificationService;
import it.gov.pagopa.pu.sil.service.AccessTokenService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentNotificationService {
  private final OrgSilServiceComponent orgSilServiceComponent;
  private final LegacyPaymentNotificationService legacyPaymentNotificationService;
  private final AccessTokenService accessTokenService;

  public PaymentNotificationService(OrgSilServiceComponent orgSilServiceComponent,
                                    LegacyPaymentNotificationService legacyPaymentNotificationService,
                                    AccessTokenService accessTokenService) {
    this.orgSilServiceComponent = orgSilServiceComponent;
    this.legacyPaymentNotificationService = legacyPaymentNotificationService;
    this.accessTokenService = accessTokenService;
  }

  public void notifyPayment(Long orgSilServiceId, String nav, UserInfo loggedUser, String accessToken) {
    OrgSilService orgSilService = orgSilServiceComponent.getOrgSilServiceById(orgSilServiceId, accessToken)
      .orElseThrow(() -> new IllegalArgumentException("Organization service not found"));
    AuthorizationService.validateUserForOrganizationId(orgSilService.getOrganizationId(), loggedUser);

    //TODO add integration logic here of https://pagopa.atlassian.net/browse/P4ADEV-3282

    String silAccessToken = accessTokenService.getAccessToken(orgSilService, accessToken);

    legacyPaymentNotificationService.notifyPayment(
      silAccessToken,
      orgSilService.getServiceUrl(),
      PaymentNotification.builder()
        .rt("")
        .esito("")
        .build()
    );
  }
}
