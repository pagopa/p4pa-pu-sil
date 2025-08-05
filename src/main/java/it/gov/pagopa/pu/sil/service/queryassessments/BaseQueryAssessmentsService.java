package it.gov.pagopa.pu.sil.service.queryassessments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.classification.dto.generated.Treasury;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentNoPII;
import it.gov.pagopa.pu.sil.connector.classification.ClassificationService;
import it.gov.pagopa.pu.sil.connector.debtpositions.InstallmentService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class BaseQueryAssessmentsService<R> {
  private static final List<String> PAYMENT_OUTCOME_CODES = List.of("8", "9");

  private final ClassificationService classificationService;
  private final InstallmentService installmentService;

  protected BaseQueryAssessmentsService(ClassificationService classificationService,
                                        InstallmentService installmentService) {
    this.classificationService = classificationService;
    this.installmentService = installmentService;
  }

  public R getAssessment(
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode,
    String iuf,
    String billYear,
    String billNumber) {
    AuthorizationService.validateAdminRole(orgIpaCode, userInfo);

    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);

    List<AssessmentsBalanceView> balances;
    if (iuf == null) {
      Treasury treasury = classificationService.findTreasuryBySemanticKey(
        organizationId,
        billNumber,
        billYear,
        accessToken
      ).orElseThrow(() -> handleException(SilFaults.PIVOT_BOLLETTA_NON_TROVATA,
        "La bolletta per codIpaEnte [ %s ], annoBolletta [ %s ] e numeroBolletta [ %s ] non è stata trovata"
          .formatted(orgIpaCode, billYear, billNumber))
      );
      balances = getAssessmentsBalances(userInfo, accessToken, organizationId, treasury.getIuf(), orgIpaCode);
    } else {
      balances = getAssessmentsBalances(userInfo, accessToken, organizationId, iuf, orgIpaCode);
    }
    return mapToResponse(balances);
  }

  private List<AssessmentsBalanceView> getAssessmentsBalances(UserInfo userInfo, String accessToken, Long organizationId, String iuf, String organizationIpaCode) {
    List<String> iuds = classificationService.findPaymentsReportingByOrganizationIdAndIuf(organizationId, iuf, accessToken)
      .stream()
      .filter(pr -> pr != null && !PAYMENT_OUTCOME_CODES.contains(pr.getPaymentOutcomeCode()))
      .flatMap(pr -> Optional.ofNullable(installmentService.findAuthorizedByTransferSemanticKey(
        pr.getOrganizationId(),
        pr.getIuv(),
        pr.getIur(),
        pr.getTransferIndex(),
        userInfo.getMappedExternalUserId(),
        accessToken)).stream())
      .map(InstallmentNoPII::getIud)
      .toList();
    if (iuds.isEmpty()) {
      throw handleException(SilFaults.PIVOT_NESSUNA_RENDICONTAZIONE_TROVATA,
        "Nessuna rendicontazione trovata per l'organizzazione [ %s ] e IUF [ %s ]"
          .formatted(organizationIpaCode, iuf));
    }
    return classificationService.findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(
        organizationId, iuds, accessToken);
  }

  protected abstract RuntimeException handleException(SilFaults fault, String message);
  protected abstract R mapToResponse(List<AssessmentsBalanceView> balances);
}
