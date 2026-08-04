package it.gov.pagopa.pu.sil.service.outbound.paymentnotification;

import it.gov.pagopa.paymentnotification.dto.generated.PaymentDataDTO;
import it.gov.pagopa.paymentnotification.dto.generated.PaymentNotificationRequest;
import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPosition;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.paymentnotification.LegacyPaymentNotificationService;
import it.gov.pagopa.pu.sil.connector.paymentnotification.NativePaymentNotificationService;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.mapper.PaymentNotificationMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.outbound.SilAccessTokenService;
import it.gov.pagopa.pu.sil.service.inbound.payments.receipt.ReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationService {

  private final OrgSilServiceComponent orgSilServiceComponent;
  private final LegacyPaymentNotificationService legacyPaymentNotificationService;
  private final NativePaymentNotificationService nativePaymentNotificationService;
  private final SilAccessTokenService silAccessTokenService;
  private final OrganizationService organizationService;
  private final PagatiMapper pagatiMapper;
  private final ReceiptService receiptService;
  private final DebtPositionService debtPositionService;
  private final DebtPositionTypeService debtPositionTypeService;
  private final PaymentNotificationMapper paymentNotificationMapper;

  public void notifyPayment(Long orgSilServiceId, InstallmentDTO installmentDTO, UserInfo loggedUser, String accessToken) {
    OrgSilServiceDTO orgSilService = orgSilServiceComponent.getOrgSilServiceById(orgSilServiceId, accessToken)
      .orElseThrow(() -> new IllegalArgumentException("[ORG_SIL_SERVICE_NOT_FOUND] OrgSilService with id %s not found".formatted(orgSilServiceId)));
    AuthorizationService.validateAdminRole(orgSilService.getOrganizationId(), loggedUser);

    Organization organization = organizationService.getOrganizationById(orgSilService.getOrganizationId(), accessToken)
      .orElseThrow(() -> new IllegalArgumentException("[ORGANIZATION_NOT_FOUND] Organization with id " + orgSilService.getOrganizationId() + " not found"));

    DebtPosition debtPosition = debtPositionService.getDebtPositionByInstallmentId(installmentDTO.getInstallmentId(), accessToken);
    if (debtPosition == null) {
      throw new IllegalArgumentException("[DEBT_POSITION_NOT_FOUND] DebtPosition related to installmentId " + installmentDTO.getInstallmentId() + " not found");
    }
    if (!debtPosition.getOrganizationId().equals(organization.getOrganizationId())) {
      throw new IllegalArgumentException("[INVALID_INSTALLMENT] The installment provided is not related to the organization requested. requested " + organization.getOrganizationId() + " provided " + debtPosition.getOrganizationId());
    }

    if (BooleanUtils.isTrue(orgSilService.getFlagLegacy())) {
      //legacy payment notification implementation
      legacyPaymentNotification(installmentDTO, organization, orgSilService, loggedUser, accessToken);
    } else {
      //native payment notification implementation
      nativePaymentNotification(installmentDTO, organization, orgSilService, loggedUser, accessToken);
    }

  }

  private void legacyPaymentNotification(InstallmentDTO installmentDTO, Organization organization, OrgSilServiceDTO orgSilService, UserInfo loggedUser, String accessToken) {
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

  private void nativePaymentNotification(InstallmentDTO installmentDTO, Organization organization, OrgSilServiceDTO orgSilService, UserInfo loggedUser, String accessToken) {
    DebtPositionTypeOrg debtPositionTypeOrg = debtPositionTypeService.getDebtPositionTypeOrgByInstallmentId(installmentDTO.getInstallmentId(), accessToken);
    PaymentDataDTO paymentDataDTO = paymentNotificationMapper.mapPaymentData(
      installmentDTO,
      organization.getOrgFiscalCode(),
      debtPositionTypeOrg.getCode(),
      accessToken
    );
    byte[] encodedReceipt = receiptService.getReceiptById(installmentDTO.getReceiptId(), organization.getOrganizationId(), accessToken);
    PaymentNotificationRequest paymentNotificationRequest = PaymentNotificationRequest.builder()
      .paymentData(paymentDataDTO)
      .encodedReceipt(Base64.getEncoder().encodeToString(encodedReceipt))
      .build();

    nativePaymentNotificationService.notifyPayment(orgSilService,
      loggedUser,
      accessToken,
      paymentNotificationRequest
    );
  }

}
