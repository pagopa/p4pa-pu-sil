package it.gov.pagopa.pu.sil.service.inbound.payments.queryassessments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.sil.connector.classification.AssessmentService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class BaseQueryAssessmentsService<R> {
  private final AssessmentService assessmentService;

  protected BaseQueryAssessmentsService(AssessmentService assessmentService) {
    this.assessmentService = assessmentService;
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

    if (iuf == null) {
      List<AssessmentsBalanceView> balances = assessmentService
        .findClosedAssessmentsBalanceViewByOrganizationIdAndBill(
          organizationId, billNumber, billYear, accessToken);
      if (balances.isEmpty()) {
        throw handleException(
          SilFaults.PIVOT_BOLLETTA_NON_TROVATA,
          "La bolletta per codIpaEnte [ %s ], annoBolletta [ %s ] e numeroBolletta [ %s ] non è stata trovata"
            .formatted(orgIpaCode, billYear, billNumber));
      }
      return mapToResponse(balances);
    }

    List<AssessmentsBalanceView> balances = assessmentService
      .findClosedAssessmentsBalanceViewByOrganizationIdAndIuf(
        organizationId, iuf, accessToken);
    if (balances.isEmpty()) {
      throw handleException(
        SilFaults.PIVOT_NESSUNA_RENDICONTAZIONE_TROVATA,
        "Nessuna rendicontazione trovata per l'organizzazione [ %s ] e IUF [ %s ]"
          .formatted(orgIpaCode, iuf));
    }
    return mapToResponse(balances);
  }

  protected abstract RuntimeException handleException(SilFaults fault, String message);
  protected abstract R mapToResponse(List<AssessmentsBalanceView> balances);
}
