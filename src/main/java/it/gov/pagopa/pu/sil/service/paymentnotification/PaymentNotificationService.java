package it.gov.pagopa.pu.sil.service.paymentnotification;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPosition;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.paymentnotification.LegacyPaymentNotificationService;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.SilAccessTokenService;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

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
  private final DebtPositionService debtPositionService;

  public void notifyPayment(Long orgSilServiceId, InstallmentDTO installmentDTO, UserInfo loggedUser, String accessToken) {
    OrgSilServiceDTO orgSilService = orgSilServiceComponent.getOrgSilServiceById(orgSilServiceId, accessToken)
      .orElseThrow(() -> new IllegalArgumentException("Organization service having id " + orgSilServiceId + " not found"));
    AuthorizationService.validateAdminRole(orgSilService.getOrganizationId(), loggedUser);

    Organization organization = organizationService.getOrganizationById(orgSilService.getOrganizationId(), accessToken)
      .orElseThrow(() -> new IllegalArgumentException("Organization having id " + orgSilService.getOrganizationId() + " not found"));

    DebtPosition debtPosition = debtPositionService.getDebtPositionByInstallmentId(installmentDTO.getInstallmentId(), accessToken);
    if(debtPosition == null){
      throw new IllegalArgumentException("DebtPosition related to installmentId " + installmentDTO.getInstallmentId() + " not found");
    }
    if(!debtPosition.getOrganizationId().equals(organization.getOrganizationId())){
      throw new IllegalArgumentException("The installment provided is not related to the organization requested. requested " + organization.getOrganizationId() + " provided " + debtPosition.getOrganizationId());
    }

    PaymentNotification paymentNotification = buildPaymentNotification(installmentDTO, organization, accessToken);

    String silAccessToken = silAccessTokenService.getSilAccessToken(organization.getOrgFiscalCode(), installmentDTO.getNav(), loggedUser, orgSilService, accessToken);

    legacyPaymentNotificationService.notifyPayment(
      organization.getOrgFiscalCode(),
      orgSilService,
      installmentDTO.getNav(),
      loggedUser,
      silAccessToken,
      paymentNotification
    );
  }

  private PaymentNotification buildPaymentNotification(InstallmentDTO installmentDTO,
                                                       Organization organization, String accessToken) {
    byte[] encodedPagati = pagatiMapper.mapDebtPositionsToEncodedPagati(installmentDTO, organization, accessToken);
    byte[] encodedReceipt = receiptService.getReceiptById(installmentDTO.getReceiptId(), organization.getOrganizationId(), accessToken);
    return PaymentNotification.builder()
      .rt(Base64.getEncoder().encodeToString(encodedReceipt))
      .esito(Base64.getEncoder().encodeToString(encodedPagati))
      .build();
  }
}
