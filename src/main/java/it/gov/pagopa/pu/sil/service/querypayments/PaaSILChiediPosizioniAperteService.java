package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.QueryPaymentStatusType;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionInstallmentFacadeService;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPosizioniAperte;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPosizioniAperteRisposta;
import it.veneto.regione.pagamenti.ente.PaaSILPosizioniAperte;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PaaSILChiediPosizioniAperteService extends AbstractQueryPaymentsService<PaaSILChiediPosizioniAperte, PaaSILChiediPosizioniAperteRisposta> {

  public PaaSILChiediPosizioniAperteService(OrganizationService organizationService, DebtPositionInstallmentFacadeService debtPositionInstallmentFacadeService) {
    super(organizationService, debtPositionInstallmentFacadeService);
  }

  @Override
  protected String getOrgIpaCode(PaaSILChiediPosizioniAperte request) {
    return request.getCodIpaEnte();
  }

  @Override
  protected PaaSILChiediPosizioniAperteRisposta mapper(List<Pair<DebtPositionDTO, InstallmentDTO>> debtPositionWithInstallmentList, Organization organization, String accessToken, PaaSILChiediPosizioniAperte request) {
    PaaSILChiediPosizioniAperteRisposta response = new PaaSILChiediPosizioniAperteRisposta();

    debtPositionWithInstallmentList.stream()
      .filter(pair -> DebtPositionStatus.PAID.equals(pair.getLeft().getStatus())
        && InstallmentStatus.PAID.equals(pair.getRight().getStatus()))
      .forEach(pair -> {
        response.getPaaSILPosizioniApertes().add(new PaaSILPosizioniAperte());
      });

    return response;
  }

  @Override
  protected PaymentStatusRequest validateAndTransformRequest(PaaSILChiediPosizioniAperte request, String orgIpaCode) {
    return new PaymentStatusRequest(
      orgIpaCode,
      QueryPaymentStatusType.INSTALLMENT_ID,
      request.getIdentificativoUnivocoPersonaFG().getCodiceIdentificativoUnivoco(),
      false
    );
  }
}
