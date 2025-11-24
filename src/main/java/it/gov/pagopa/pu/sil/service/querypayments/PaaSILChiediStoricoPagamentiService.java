package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
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
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PaaSILChiediStoricoPagamentiService extends AbstractDebtorQueryPaymentService<PaaSILChiediStoricoPagamenti, PaaSILChiediStoricoPagamentiRisposta> {

  private final ReceiptService receiptService;
  private final PagatiMapper pagatiMapper;

  public PaaSILChiediStoricoPagamentiService(@Value("${public-base-url.bff}") String bffBaseUrl,
                                             DebtPositionService debtPositionService,
                                             OrganizationService organizationService,
                                             AuthorizationService authorizationService,
                                             ReceiptService receiptService,
                                             PagatiMapper pagatiMapper) {
    super(bffBaseUrl, debtPositionService, organizationService, authorizationService);
    this.receiptService = receiptService;
    this.pagatiMapper = pagatiMapper;
  }

  @Override
  protected AbstractDebtorQueryPaymentService.DebtorQueryPaymentRequest transformRequest(PaaSILChiediStoricoPagamenti request) {
    OffsetDateTime dateFrom = ConversionUtils.toOffsetDateTime(request.getDataFrom());
    OffsetDateTime dateTo = Optional.ofNullable(request.getDataTo())
        .map(ConversionUtils::toOffsetDateTime)
        .orElse(dateFrom.plusDays(1));

    return new AbstractDebtorQueryPaymentService.DebtorQueryPaymentRequest(request.getCodIpaEnte(),
      PersonEntityType.fromValue(request.getIdentificativoUnivocoPersonaFG().getTipoIdentificativoUnivoco().value()),
      request.getIdentificativoUnivocoPersonaFG().getCodiceIdentificativoUnivoco(),
      InstallmentStatus.PAID,
      dateFrom,
      dateTo);
  }

  @Override
  protected PaaSILChiediStoricoPagamentiRisposta gatherToResponse(AbstractDebtorQueryPaymentService.DebtorQueryPaymentRequest request,
                                                                   List<Organization> organizations,
                                                                   List<DebtPositionDTO> debtPositions,
                                                                   String accessToken) {
    PaaSILChiediStoricoPagamentiRisposta response = new PaaSILChiediStoricoPagamentiRisposta();
    response.setDateTo(ConversionUtils.toXMLGregorianCalendar(request.getDateTo()));

    debtPositions.stream()
      .flatMap(debtPosition -> {
        Organization organization = organizations.stream()
          .filter(org -> org.getOrganizationId().equals(debtPosition.getOrganizationId()))
          .findFirst()
          .orElseThrow();

        return debtPosition.getPaymentOptions().stream()
          .flatMap(paymentOption -> paymentOption.getInstallments().stream()
            .map(installment -> {
              PaaSILStoricoPagamenti payment = new PaaSILStoricoPagamenti();
              byte[] receiptData = receiptService.getReceiptById(installment.getReceiptId(), debtPosition.getOrganizationId(), accessToken);
              payment.setRt(new DataHandler(new ByteArrayDataSource("application/octet-stream", receiptData)));
              payment.setCodIpaEnte(organization.getIpaCode());
              payment.setDeNomeEnte(organization.getOrgName());
              payment.setUrlDownloadRT(composeReceiptDownloadUrl(installment.getReceiptId(), organization.getOrganizationId(), accessToken));
              byte[] pagatiConRicevuta = pagatiMapper.mapDebtPositionsToEncodedPagatiConRicevuta(installment, organization, accessToken);
              payment.setCtPagatiConRicevuta(new DataHandler(new ByteArrayDataSource("application/octet-stream", pagatiConRicevuta)));
              return payment;
            }));
      })
      .forEach(response.getPaaSILStoricoPagamentis()::add);

    return response;
  }
}
