package it.gov.pagopa.pu.sil.service.queryassessments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentNoPII;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.mapper.AssessmentsBalanceMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.connector.classification.ClassificationService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.classification.dto.generated.Treasury;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.pagamenti.pivot.ente.CtBilancio;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILChiediAccertamento;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILChiediAccertamentoRisposta;
import it.veneto.regione.pagamenti.pivot.ente.RichiestaPerBolletta;
import it.veneto.regione.pagamenti.pivot.ente.RichiestaPerIUF;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class QueryAssessmentsService {
  private static final List<String> PAYMENT_OUTCOME_CODES = List.of("8", "9");

  private final ClassificationService classificationService;
  private final DebtPositionService debtPositionService;
  private final AssessmentsBalanceMapper assessmentsBalanceMapper;

  public QueryAssessmentsService(ClassificationService classificationService,
                                 DebtPositionService debtPositionService,
                                 AssessmentsBalanceMapper assessmentsBalanceMapper) {
    this.classificationService = classificationService;
    this.debtPositionService = debtPositionService;
    this.assessmentsBalanceMapper = assessmentsBalanceMapper;
  }

  public PivotSILChiediAccertamentoRisposta handlePivotSILChiediAccertamento(
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode,
    PivotSILChiediAccertamento request) {
    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);

    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call ingestion flow file for organization {}", clientId, orgIpaCode);
      throw new UnauthorizedException("Utente non autorizzato");
    }

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
      balances = getAssessmentsBalances(userInfo, accessToken, organizationId, treasury.getIuf());
    } else {
      balances = getAssessmentsBalances(userInfo, accessToken, organizationId, richiestaPerIUF.getIdentificativoUnivocoFlusso());
    }

    PivotSILChiediAccertamentoRisposta response = new PivotSILChiediAccertamentoRisposta();
    response.getBilancios().addAll(balances);
    return response;
  }

  private List<CtBilancio> getAssessmentsBalances(UserInfo userInfo, String accessToken, Long organizationId, String iuf) {
    List<String> iuds = classificationService.findPaymentsReportingByOrganizationIdAndIuf(organizationId, iuf, accessToken)
      .stream()
      .filter(pr -> !PAYMENT_OUTCOME_CODES.contains(pr.getPaymentOutcomeCode()))
      .map(pr -> debtPositionService.findAuthorizedByTransferSemanticKey(
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
          .formatted(organizationId, iuf));
    }
    return classificationService.findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(
        organizationId, iuds, accessToken).stream()
      .map(assessmentsBalanceMapper::map2CtBilancio)
      .toList();
  }
}
