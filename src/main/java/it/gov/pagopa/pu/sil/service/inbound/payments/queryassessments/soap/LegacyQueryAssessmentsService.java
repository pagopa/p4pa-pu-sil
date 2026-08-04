package it.gov.pagopa.pu.sil.service.inbound.payments.queryassessments.soap;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.sil.connector.classification.AssessmentService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.soap.LegacyAssessmentsBalanceMapper;
import it.gov.pagopa.pu.sil.service.inbound.payments.queryassessments.BaseQueryAssessmentsService;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.pagamenti.pivot.ente.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class LegacyQueryAssessmentsService extends BaseQueryAssessmentsService<PivotSILChiediAccertamentoRisposta> {

  private final LegacyAssessmentsBalanceMapper legacyAssessmentsBalanceMapper;

  public LegacyQueryAssessmentsService(AssessmentService assessmentService,
                                       LegacyAssessmentsBalanceMapper legacyAssessmentsBalanceMapper) {
    super(assessmentService);
    this.legacyAssessmentsBalanceMapper = legacyAssessmentsBalanceMapper;
  }

  public PivotSILChiediAccertamentoRisposta handlePivotSILChiediAccertamento(
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode,
    PivotSILChiediAccertamento request) {

    RichiestaPerBolletta billRequest = request.getRichiestaPerBolletta();
    RichiestaPerIUF iufRequest = request.getRichiestaPerIUF();

    if (!ValidationUtils.verifyExclusivePresence(billRequest, iufRequest)) {
      throw new SilFaultException(SilFaults.PIVOT_RICHIESTA_CON_PARAMETRI_MULTIPLI, "Solo uno tra RichiestaPerBolletta o RichiestaPerIUF deve essere presente");
    }

    String iuf = Optional.ofNullable(iufRequest)
      .map(RichiestaPerIUF::getIdentificativoUnivocoFlusso)
      .orElse(null);

    Optional<RichiestaPerBolletta> optionalBillRequest = Optional.ofNullable(billRequest);
    String billYear = optionalBillRequest
      .map(RichiestaPerBolletta::getAnnoBolletta)
      .orElse(null);

    String billNumber = optionalBillRequest
      .map(RichiestaPerBolletta::getNumeroBolletta)
      .orElse(null);

    return getAssessment(userInfo, accessToken, orgIpaCode, iuf, billYear, billNumber);
  }

  @Override
  protected RuntimeException handleException(SilFaults fault, String message) {
    return new SilFaultException(fault, message);
  }

  @Override
  protected PivotSILChiediAccertamentoRisposta mapToResponse(List<AssessmentsBalanceView> balances) {
    List<CtBilancio> ctBilancios = balances.stream()
      .map(legacyAssessmentsBalanceMapper::map2CtBilancio)
      .toList();
    PivotSILChiediAccertamentoRisposta response = new PivotSILChiediAccertamentoRisposta();
    response.getBilancios().addAll(ctBilancios);
    return response;
  }


}
