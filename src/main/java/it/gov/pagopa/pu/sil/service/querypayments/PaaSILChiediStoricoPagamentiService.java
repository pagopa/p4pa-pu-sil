package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.veneto.regione.pagamenti.ente.PaaSILChiediStoricoPagamenti;
import it.veneto.regione.pagamenti.ente.PaaSILChiediStoricoPagamentiRisposta;
import it.veneto.regione.pagamenti.ente.PaaSILStoricoPagamenti;
import jakarta.activation.DataHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
public class PaaSILChiediStoricoPagamentiService extends AbstractDebtorQueryPaymentService<PaaSILChiediStoricoPagamenti, PaaSILChiediStoricoPagamentiRisposta> {

  private final ReceiptService receiptService;
  private final PagatiMapper pagatiMapper;

  public PaaSILChiediStoricoPagamentiService(@Value("${public-base-url.fileshare}") String fileShareBaseUrl,
                                             DebtPositionService debtPositionService,
                                             OrganizationService organizationService,
                                             ReceiptService receiptService,
                                             PagatiMapper pagatiMapper) {
    super(fileShareBaseUrl, debtPositionService, organizationService);
    this.receiptService = receiptService;
    this.pagatiMapper = pagatiMapper;
  }

  @Override
  protected String getCodIpaEnte(PaaSILChiediStoricoPagamenti request) {
    return request.getCodIpaEnte();
  }

  @Override
  protected String getDebtorFiscalCode(PaaSILChiediStoricoPagamenti request) {
    return request.getIdentificativoUnivocoPersonaFG().getCodiceIdentificativoUnivoco();
  }

  @Override
  protected PersonEntityType getDebtorEntityType(PaaSILChiediStoricoPagamenti request) {
    return PersonEntityType.fromValue(request.getIdentificativoUnivocoPersonaFG().getTipoIdentificativoUnivoco().value());
  }

  @Override
  protected InstallmentStatus getInstallmentStatus() { return InstallmentStatus.PAID; }

  @Override
  protected OffsetDateTimeIntervalFilter getDateFilter(PaaSILChiediStoricoPagamenti request) {
    OffsetDateTime dateFrom = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS);
    if (request.getDataFrom() != null) {
      dateFrom = ConversionUtils.toOffsetDateTime(request.getDataFrom());
    }

    OffsetDateTime dateTo = dateFrom.plusDays(1);
    if (request.getDataTo() != null) {
      dateTo = ConversionUtils.toOffsetDateTime(request.getDataTo());
    }

    return new OffsetDateTimeIntervalFilter(dateFrom, dateTo);
  }

  @Override
  protected PaaSILChiediStoricoPagamentiRisposta createResponse() {
    return new PaaSILChiediStoricoPagamentiRisposta();
  }

  @Override
  protected void gatherToResponse(PaaSILChiediStoricoPagamenti request,
                                  Organization organization,
                                  List<DebtPositionDTO> debtPositions,
                                  String accessToken,
                                  PaaSILChiediStoricoPagamentiRisposta response) {
    OffsetDateTimeIntervalFilter dateFilter = getDateFilter(request);
    response.setDateTo(ConversionUtils.toXMLGregorianCalendar(dateFilter.getTo()));
    response.getPaaSILStoricoPagamentis().addAll(processDebtPositions(organization, debtPositions, accessToken));
  }

  private List<PaaSILStoricoPagamenti> processDebtPositions(Organization organization, List<DebtPositionDTO> debtPositions, String accessToken) {
    return debtPositions.stream()
      .flatMap(debtPosition -> debtPosition.getPaymentOptions().stream()
        .flatMap(paymentOption -> paymentOption.getInstallments().stream()
          .map(installment -> {
            PaaSILStoricoPagamenti payment = new PaaSILStoricoPagamenti();
            byte[] receiptData = receiptService.getReceiptById(installment.getReceiptId(), debtPosition.getOrganizationId(), accessToken);
            payment.setRt(new DataHandler(new ByteArrayDataSource(
              "application/octet-stream", receiptData
            )));
            payment.setCodIpaEnte(organization.getIpaCode());
            payment.setDeNomeEnte(organization.getOrgName());
            payment.setUrlDownloadRT(composeReceiptDownloadUrl(installment.getReceiptId(), organization.getOrganizationId()));
            byte[] pagatiConRicevuta = pagatiMapper.mapDebtPositionsToEncodedPagatiConRicevuta(installment, organization, accessToken);

            payment.setCtPagatiConRicevuta(
              new DataHandler(
                new ByteArrayDataSource("application/octet-stream", pagatiConRicevuta)
              )
            );

            return payment;
          })
        )
      )
      .toList();
  }
}
