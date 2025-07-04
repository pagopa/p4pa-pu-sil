package it.gov.pagopa.pu.sil.service.paymentnotification;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.paymentnotification.LegacyPaymentNotificationService;
import it.gov.pagopa.pu.sil.exception.PaymentNotFoundException;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.service.SilAccessTokenService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import it.gov.pagopa.pu.sil.service.debtpositions.DebtPositionFacadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationService {

  private final OrgSilServiceComponent orgSilServiceComponent;
  private final LegacyPaymentNotificationService legacyPaymentNotificationService;
  private final SilAccessTokenService silAccessTokenService;
  private final OrganizationService organizationService;
  private final DebtPositionService debtPositionService;
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

    List<Pair<DebtPositionDTO, InstallmentDTO>> debtPositionWithInstallmentList = getDebtPositionsAndInstallments(accessToken, installments);

    PaymentNotification paymentNotification = buildPaymentNotification(debtPositionWithInstallmentList, organization, accessToken);

    String silAccessToken = silAccessTokenService.getSilAccessToken(orgSilService, accessToken);

    legacyPaymentNotificationService.notifyPayment(
      silAccessToken,
      orgSilService.getServiceUrl(),
      paymentNotification
    );
  }

  private List<Pair<DebtPositionDTO, InstallmentDTO>> getDebtPositionsAndInstallments(String accessToken, List<InstallmentDTO> installmentDTOs) {
    return installmentDTOs.stream()
      .map(InstallmentDTO::getInstallmentId)
      //search for the debt position by installmentId
      .map(installmentId -> Pair.of(installmentId, debtPositionService.getDebtPositionByInstallmentId(installmentId, accessToken)))
      //find the installment in the debt position
      .map(debtPositionPair -> Pair.of(debtPositionPair.getRight(), findInstallmentOfDebtPosition(debtPositionPair.getRight(),
        installment -> Objects.equals(installment.getInstallmentId(), debtPositionPair.getLeft()))))
      //return the pair of debt position and matching installment
      .toList();
  }

  private InstallmentDTO findInstallmentOfDebtPosition(DebtPositionDTO debtPosition, Predicate<InstallmentDTO> installmentFinderPredicate) {
    return debtPosition.getPaymentOptions().stream()
      .flatMap(po -> po.getInstallments().stream())
      .filter(installmentFinderPredicate)
      .findFirst()
      .orElseThrow(() -> new PaymentNotFoundException("Installment not found"));
  }

  private PaymentNotification buildPaymentNotification(List<Pair<DebtPositionDTO, InstallmentDTO>> debtPositionWithInstallmentList,
                                                       Organization organization, String accessToken) {
    DebtPositionDTO debtPositionDTO = debtPositionWithInstallmentList.getFirst().getLeft();
    InstallmentDTO installmentDTO = debtPositionWithInstallmentList.getFirst().getRight();
    byte[] encodedPagati = pagatiMapper.mapDebtPositionsToEncodedPagati(debtPositionDTO, installmentDTO, organization, accessToken);
    byte[] encodedReceipt = receiptService.getReceiptById(installmentDTO.getReceiptId(), organization.getOrganizationId(), accessToken);
    return PaymentNotification.builder()
      .rt(Base64.getEncoder().encodeToString(encodedReceipt))
      .esito(Base64.getEncoder().encodeToString(encodedPagati))
      .build();
  }
}
