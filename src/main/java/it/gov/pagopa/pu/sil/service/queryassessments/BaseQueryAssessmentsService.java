package it.gov.pagopa.pu.sil.service.queryassessments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.sil.connector.classification.ClassificationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class BaseQueryAssessmentsService<R> {
  private final ClassificationService classificationService;

  protected BaseQueryAssessmentsService(ClassificationService classificationService) {
    this.classificationService = classificationService;
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
    SilFaults fault;
    String errorMessage;
    if (iuf == null) {
      balances = classificationService.findClosedAssessmentsBalanceViewByOrganizationIdAndBill(
        organizationId,
        billNumber,
        billYear,
        accessToken);
      fault = SilFaults.PIVOT_BOLLETTA_NON_TROVATA;
      errorMessage = "La bolletta per codIpaEnte [ %s ], annoBolletta [ %s ] e numeroBolletta [ %s ] non è stata trovata"
        .formatted(orgIpaCode, billYear, billNumber);
    } else {
      balances = classificationService.findClosedAssessmentsBalanceViewByOrganizationIdAndIuf(
        organizationId, iuf, accessToken);
      fault = SilFaults.PIVOT_NESSUNA_RENDICONTAZIONE_TROVATA;
      errorMessage = "Nessuna rendicontazione trovata per l'organizzazione [ %s ] e IUF [ %s ]"
        .formatted(orgIpaCode, iuf);
    }
    if (balances.isEmpty()) {
      throw handleException(fault, errorMessage);
    }
    return mapToResponse(balances);
  }

  protected abstract RuntimeException handleException(SilFaults fault, String message);
  protected abstract R mapToResponse(List<AssessmentsBalanceView> balances);
}
