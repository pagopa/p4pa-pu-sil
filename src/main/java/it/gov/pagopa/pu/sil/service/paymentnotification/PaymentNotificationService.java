package it.gov.pagopa.pu.sil.service.paymentnotification;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.paymentnotification.LegacyPaymentNotificationService;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.service.SilAccessTokenService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import it.gov.pagopa.pu.sil.service.debtpositions.DebtPositionFacadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationService {

  private final OrgSilServiceComponent orgSilServiceComponent;
  private final LegacyPaymentNotificationService legacyPaymentNotificationService;
  private final SilAccessTokenService silAccessTokenService;
  private final OrganizationService organizationService;
  private final PagatiMapper pagatiMapper;
  private final ReceiptService receiptService;
  private final DebtPositionFacadeService debtPositionFacadeService;

  public void notifyPayment(Long orgSilServiceId, String nav, UserInfo loggedUser, String accessToken) {
    OrgSilService orgSilService = orgSilServiceComponent.getOrgSilServiceById(orgSilServiceId, accessToken)
      .orElseThrow(() -> new IllegalArgumentException("Organization service not found"));
    AuthorizationService.validateUserForOrganizationId(orgSilService.getOrganizationId(), loggedUser);

    Organization organization = organizationService.getOrganizationById(orgSilService.getOrganizationId(), accessToken)
      .orElse(null);

    List<InstallmentDTO> installments = debtPositionFacadeService.getInstallmentsByOrganizationIdAndNav(orgSilService.getOrganizationId(), nav, accessToken);

    PaymentNotification paymentNotification = buildPaymentNotification(installments, organization, accessToken);

    String silAccessToken = silAccessTokenService.getSilAccessToken(orgSilService, accessToken);

    legacyPaymentNotificationService.notifyPayment(
      silAccessToken,
      orgSilService.getServiceUrl(),
      paymentNotification
    );
  }

  private PaymentNotification buildPaymentNotification(List<InstallmentDTO> installmentDTOList,
                                                       Organization organization, String accessToken) {
    InstallmentDTO installmentDTO = installmentDTOList.getFirst();
    byte[] encodedPagati = pagatiMapper.mapDebtPositionsToEncodedPagati(installmentDTO, organization, accessToken);
    byte[] encodedReceipt = receiptService.getReceiptById(installmentDTO.getReceiptId(), organization.getOrganizationId(), accessToken);
    return PaymentNotification.builder()
      .rt(Base64.getEncoder().encodeToString(encodedReceipt))
      .esito(Base64.getEncoder().encodeToString(encodedPagati))
      .build();
  }
}
