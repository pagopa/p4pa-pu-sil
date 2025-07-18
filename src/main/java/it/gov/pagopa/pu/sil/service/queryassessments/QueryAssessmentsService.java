package it.gov.pagopa.pu.sil.service.queryassessments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.classification.dto.generated.Treasury;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentNoPII;
import it.gov.pagopa.pu.sil.connector.classification.ClassificationService;
import it.gov.pagopa.pu.sil.connector.debtpositions.InstallmentService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.AssessmentsBalanceMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.pagamenti.pivot.ente.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class QueryAssessmentsService {
  private static final List<String> PAYMENT_OUTCOME_CODES = List.of("8", "9");

  private final ClassificationService classificationService;
  private final InstallmentService installmentService;
  private final AssessmentsBalanceMapper assessmentsBalanceMapper;

  public QueryAssessmentsService(ClassificationService classificationService,
                                 InstallmentService installmentService,
                                 AssessmentsBalanceMapper assessmentsBalanceMapper) {
    this.classificationService = classificationService;
    this.installmentService = installmentService;
    this.assessmentsBalanceMapper = assessmentsBalanceMapper;
  }

  public PivotSILChiediAccertamentoRisposta handlePivotSILChiediAccertamento(
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode,
    PivotSILChiediAccertamento request
  ) {
    AuthorizationService.validateAdminRole(orgIpaCode, userInfo);

    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);

    RichiestaPerBolletta richiestaPerBolletta = request.getRichiestaPerBolletta();
    RichiestaPerIUF richiestaPerIUF = request.getRichiestaPerIUF();

    if (!ValidationUtils.verifyExclusivePresence(richiestaPerBolletta, richiestaPerIUF)) {
      throw new IllegalArgumentException("Solo uno tra RichiestaPerBolletta o RichiestaPerIUF deve essere presente");
    }

    List<CtBilancio> balances;
    if (richiestaPerBolletta != null) {
      Treasury treasury = classificationService.findTreasuryBySemanticKey(
        organizationId,
        richiestaPerBolletta.getNumeroBolletta(),
        richiestaPerBolletta.getAnnoBolletta(),
        accessToken
      ).orElseThrow(() -> new SilFaultException(SilFaults.PIVOT_BOLLETTA_NON_TROVATA,
        "La bolletta per codIpaEnte [ %s ], annoBolletta [ %s ] e numeroBolletta [ %s ] non è stata trovata"
          .formatted(orgIpaCode, richiestaPerBolletta.getAnnoBolletta(), richiestaPerBolletta.getNumeroBolletta()))
      );
      balances = getAssessmentsBalances(userInfo, accessToken, organizationId, treasury.getIuf(), orgIpaCode);
    } else {
      balances = getAssessmentsBalances(userInfo, accessToken, organizationId, richiestaPerIUF.getIdentificativoUnivocoFlusso(), orgIpaCode);
    }

    PivotSILChiediAccertamentoRisposta response = new PivotSILChiediAccertamentoRisposta();
    response.getBilancios().addAll(balances);
    return response;
  }

  private List<CtBilancio> getAssessmentsBalances(UserInfo userInfo, String accessToken, Long organizationId, String iuf, String organizationIpaCode) {
    List<String> iuds = classificationService.findPaymentsReportingByOrganizationIdAndIuf(organizationId, iuf, accessToken)
      .stream()
      .filter(pr -> !PAYMENT_OUTCOME_CODES.contains(pr.getPaymentOutcomeCode()))
      .map(pr -> installmentService.findAuthorizedByTransferSemanticKey(
        pr.getOrganizationId(),
        pr.getIuv(),
        pr.getIur(),
        pr.getTransferIndex(),
        userInfo.getMappedExternalUserId(),
        accessToken))
      .map(InstallmentNoPII::getIud)
      .toList();
    if (iuds.isEmpty()) {
      throw new SilFaultException(SilFaults.PIVOT_NESSUNA_RENDICONTAZIONE_TROVATA,
        "Nessuna rendicontazione trovata per l'organizzazione [ %s ] e IUF [ %s ]"
          .formatted(organizationIpaCode, iuf));
    }
    return classificationService.findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(
        organizationId, iuds, accessToken).stream()
      .map(assessmentsBalanceMapper::map2CtBilancio)
      .toList();
  }
}
