package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.veneto.regione.pagamenti.ente.PaaSILChiediStoricoPagamenti;
import it.veneto.regione.pagamenti.ente.PaaSILChiediStoricoPagamentiRisposta;
import it.veneto.regione.pagamenti.ente.PaaSILStoricoPagamenti;
import jakarta.activation.DataHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static it.gov.pagopa.pu.sil.util.Constants.MIXED_DP_TYPE_ORG_CODE;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaaSILChiediStoricoPagamentiService {

  private final DebtPositionService debtPositionService;
  private final OrganizationService organizationService;
  private final ReceiptService receiptService;
  private final PagatiMapper pagatiMapper;

  public PaaSILChiediStoricoPagamentiRisposta processRequest(
    PaaSILChiediStoricoPagamenti request,
    UserInfo userInfo,
    String accessToken
  ) {
    PaaSILChiediStoricoPagamentiRisposta response = new PaaSILChiediStoricoPagamentiRisposta();
    AuthorizationService.validateAdminRole(request.getCodIpaEnte(), userInfo);

    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, request.getCodIpaEnte());
    Organization organization = organizationService.getOrganizationById(organizationId, accessToken)
      .orElse(null);
    if (organization == null || !OrganizationStatus.ACTIVE.equals(organization.getStatus())) {
      throw new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "L'ente non è valido o non è abilitato");
    }

    OffsetDateTime dateFrom = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS);
    if (request.getDataFrom() != null) {
      dateFrom = ConversionUtils.toOffsetDateTime(request.getDataFrom());
    }

    OffsetDateTime dateTo = dateFrom.plusDays(1);
    if (request.getDataTo() != null) {
      dateTo = ConversionUtils.toOffsetDateTime(request.getDataTo());
    }

    OffsetDateTimeIntervalFilter dateFilter = new OffsetDateTimeIntervalFilter(dateFrom, dateTo);

    List<DebtPositionDTO> debtPositions = debtPositionService.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
      request.getIdentificativoUnivocoPersonaFG().getCodiceIdentificativoUnivoco(),
      PersonEntityType.fromValue(request.getIdentificativoUnivocoPersonaFG().getTipoIdentificativoUnivoco().value()),
      organizationId,
      List.of(MIXED_DP_TYPE_ORG_CODE),
      InstallmentStatus.PAID,
      dateFilter,
      accessToken
    );

    response.setDateTo(ConversionUtils.toXMLGregorianCalendar(dateTo));
    response.getPaaSILStoricoPagamentis().addAll(processDebtPositions(request, organization, debtPositions, accessToken));
    return response;
  }

  private List<PaaSILStoricoPagamenti> processDebtPositions(PaaSILChiediStoricoPagamenti request, Organization organization, List<DebtPositionDTO> debtPositions, String accessToken) {
    return debtPositions.stream()
      .flatMap(debtPosition -> debtPosition.getPaymentOptions().stream()
        .flatMap(paymentOption -> paymentOption.getInstallments().stream()
          .map(installment -> {
            PaaSILStoricoPagamenti payment = new PaaSILStoricoPagamenti();
            byte[] receiptData = receiptService.getReceiptById(installment.getReceiptId(), debtPosition.getOrganizationId(), accessToken);
            payment.setRt(new DataHandler(new ByteArrayDataSource(
              "application/octet-stream", receiptData
            )));
            payment.setCodIpaEnte(request.getCodIpaEnte());
            payment.setDeNomeEnte(organization.getOrgName());
            payment.setUrlDownloadRT(null); // @TODO: da implementare con la P4PU-1026
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
