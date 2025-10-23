package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.QueryPaymentStatusType;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionInstallmentFacadeService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPagati;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPagatiRisposta;
import jakarta.activation.DataHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PaaSILChiediPagatiService extends AbstractQueryPaymentsService<PaaSILChiediPagati, PaaSILChiediPagatiRisposta> {

  private final PagatiMapper pagatiMapper;

  public PaaSILChiediPagatiService(
    OrganizationService organizationService,
    DebtPositionInstallmentFacadeService debtPositionInstallmentFacadeService,
    PagatiMapper pagatiMapper) {
    super(organizationService, debtPositionInstallmentFacadeService);
    this.pagatiMapper = pagatiMapper;
  }

  @Override
  protected String getOrgIpaCode(PaaSILChiediPagati request) {
    return request.getCodIpaEnte();
  }

  @Override
  protected PaaSILChiediPagatiRisposta mapper(List<Pair<DebtPositionDTO, InstallmentDTO>> debtPositionWithInstallmentList,
                                              Organization organization, String accessToken,
                                              PaaSILChiediPagati request) {

    InstallmentDTO installmentDTO = debtPositionWithInstallmentList.getFirst().getRight();

    //map debt position to Pagati
    byte[] encodedPagati = pagatiMapper.mapDebtPositionsToEncodedPagati(installmentDTO, organization, accessToken);

    // Prepare the response
    PaaSILChiediPagatiRisposta response = new PaaSILChiediPagatiRisposta();
    response.setPagati(new DataHandler(new ByteArrayDataSource("application/octet-stream", encodedPagati)));
    return response;
  }

  @Override
  protected PaymentStatusRequest validateAndTransformRequest(PaaSILChiediPagati request, String orgIpaCode) {
    // no validation needed in this scenario
    return new PaymentStatusRequest(orgIpaCode, QueryPaymentStatusType.INSTALLMENT_ID, request.getIdSession(), false);
  }
}
