package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.debtpositions.ReceiptService;
import it.gov.pagopa.pu.sil.connector.fileshare.FileShareService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.veneto.regione.pagamenti.ente.PaaSILChiediStoricoPagamenti;
import it.veneto.regione.pagamenti.ente.PaaSILChiediStoricoPagamentiRisposta;
import it.veneto.regione.pagamenti.ente.PaaSILStoricoPagamenti;
import it.veneto.regione.schemas._2012.pagamenti.ente.*;
import jakarta.activation.DataHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaaSILChiediStoricoPagamentiService {

  private static final String OBJECT_VERSION = "6.2.0";

  private final DebtPositionService debtPositionService;
  private final OrganizationService organizationService;
  private final JAXBTransformService jaxbTransformService;
  private final ReceiptService receiptService;
  private final FileShareService fileShareService;

  public PaaSILChiediStoricoPagamentiRisposta processRequest(
    PaaSILChiediStoricoPagamenti request,
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode
  ) {
    PaaSILChiediStoricoPagamentiRisposta response = new PaaSILChiediStoricoPagamentiRisposta();
    AuthorizationService.validateAdminRole(request.getCodIpaEnte(), userInfo);

    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, request.getCodIpaEnte());
    Organization organization = organizationService.getOrganizationById(organizationId, accessToken)
      .orElse(null);
    if (organization == null || !OrganizationStatus.ACTIVE.equals(organization.getStatus())) {
      throw new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "L'ente non è valido o non è abilitato");
    }

    OffsetDateTime dateFrom = LocalDate.now().atStartOfDay().atOffset(ZoneId.systemDefault().getRules().getOffset(Instant.now()));
    if (request.getDataFrom() != null) {
      dateFrom = request.getDataFrom().toGregorianCalendar().toZonedDateTime().toOffsetDateTime();
    }

    OffsetDateTime dateTo = LocalDate.now().atTime(LocalTime.MAX).atOffset(ZoneId.systemDefault().getRules().getOffset(Instant.now()));
    if (request.getDataTo() != null) {
      dateTo = request.getDataTo().toGregorianCalendar().toZonedDateTime().toOffsetDateTime();
    }

    List<DebtPositionDTO> debtPositions = debtPositionService.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
      request.getIdentificativoUnivocoPersonaFG().getCodiceIdentificativoUnivoco(),
      PersonEntityType.fromValue(request.getIdentificativoUnivocoPersonaFG().getTipoIdentificativoUnivoco().value()),
      organizationId,
      InstallmentStatus.PAID,
      dateFrom,
      dateTo,
      accessToken
    );

    response.setDateTo(toXMLGregorianCalendar(dateTo));
    response.getPaaSILStoricoPagamentis().addAll(processDebtPositions(request, organization, debtPositions, accessToken));
    return response;
  }

  private List<PaaSILStoricoPagamenti> processDebtPositions(PaaSILChiediStoricoPagamenti request, Organization organization, List<DebtPositionDTO> debtPositions, String accessToken) {
    return debtPositions.stream()
      .flatMap(debtPosition -> debtPosition.getPaymentOptions().stream()
        .flatMap(paymentOption -> paymentOption.getInstallments().stream()
          .map(installment -> {
            PaaSILStoricoPagamenti payment = new PaaSILStoricoPagamenti();
            ReceiptDTO receipt = receiptService.getReceiptById(installment.getReceiptId(), accessToken);
            byte[] receiptData = getReceiptDataById(installment.getReceiptId(), debtPosition.getOrganizationId(), accessToken);
            payment.setRt(new DataHandler(new ByteArrayDataSource(
              "application/octet-stream", receiptData
            )));
            payment.setCodIpaEnte(request.getCodIpaEnte());
            payment.setDeNomeEnte(organization.getOrgName());
            payment.setUrlDownloadRT(null); // @TODO: da implementare con la P4PU-1026
            PagatiConRicevuta pagatiConRicevuta = buildPagatiConRicevuta(installment, receipt, organization);
            byte[] pagatiConRicevutaBytes = jaxbTransformService.marshallingAsBytes(pagatiConRicevuta, PagatiConRicevuta.class);

            payment.setCtPagatiConRicevuta(
              new DataHandler(
                new ByteArrayDataSource("application/octet-stream", pagatiConRicevutaBytes)
              )
            );

            return payment;
          })
        )
      )
      .toList();
  }

  private PagatiConRicevuta buildPagatiConRicevuta(InstallmentDTO installment, ReceiptDTO receipt, Organization organization) {
    ObjectFactory of = new ObjectFactory();

    PagatiConRicevuta pagatiConRicevuta = of.createPagatiConRicevuta();
    pagatiConRicevuta.setVersioneOggetto(OBJECT_VERSION);
    CtDominio domain = new CtDominio();
    domain.setIdentificativoDominio(organization.getOrgFiscalCode());
    pagatiConRicevuta.setDominio(domain);

    CtIstitutoAttestante istitutoAttestante = new CtIstitutoAttestante();
    CtIdentificativoUnivoco identificativoUnivoco = new CtIdentificativoUnivoco();
    identificativoUnivoco.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivoco.B);
    identificativoUnivoco.setCodiceIdentificativoUnivoco(receipt.getIdPsp());
    istitutoAttestante.setIdentificativoUnivocoAttestante(identificativoUnivoco);
    istitutoAttestante.setDenominazioneAttestante(receipt.getPspCompanyName());
    pagatiConRicevuta.setIstitutoAttestante(istitutoAttestante);

    CtEnteBeneficiario enteBeneficiario = new CtEnteBeneficiario();
    CtIdentificativoUnivocoPersonaG iupEnteBeneficiario = new CtIdentificativoUnivocoPersonaG();
    iupEnteBeneficiario.setCodiceIdentificativoUnivoco(organization.getOrgFiscalCode());
    iupEnteBeneficiario.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersG.G);
    enteBeneficiario.setIdentificativoUnivocoBeneficiario(iupEnteBeneficiario);
    enteBeneficiario.setDenominazioneBeneficiario(organization.getOrgName());
    pagatiConRicevuta.setEnteBeneficiario(enteBeneficiario);

    CtSoggettoPagatore soggettoPagatore = of.createCtSoggettoPagatore();
    CtIdentificativoUnivocoPersonaFG identificativoUnivocoPagatore = new CtIdentificativoUnivocoPersonaFG();
    identificativoUnivocoPagatore.setCodiceIdentificativoUnivoco(installment.getDebtor().getFiscalCode());
    identificativoUnivocoPagatore.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.fromValue(installment.getDebtor().getEntityType().getValue()));
    soggettoPagatore.setIdentificativoUnivocoPagatore(identificativoUnivocoPagatore);
    soggettoPagatore.setAnagraficaPagatore(installment.getDebtor().getFullName());
    soggettoPagatore.setIndirizzoPagatore(installment.getDebtor().getAddress());
    soggettoPagatore.setCivicoPagatore(installment.getDebtor().getCivic());
    soggettoPagatore.setCapPagatore(installment.getDebtor().getPostalCode());
    soggettoPagatore.setLocalitaPagatore(installment.getDebtor().getLocation());
    soggettoPagatore.setProvinciaPagatore(installment.getDebtor().getProvince());
    soggettoPagatore.setNazionePagatore(installment.getDebtor().getNation());
    soggettoPagatore.setEMailPagatore(installment.getDebtor().getEmail());
    pagatiConRicevuta.setSoggettoPagatore(soggettoPagatore);

    pagatiConRicevuta.setIdentificativoMessaggioRicevuta(receipt.getPaymentReceiptId());
    pagatiConRicevuta.setDataOraMessaggioRicevuta(ConversionUtils.toXMLGregorianCalendar(receipt.getPaymentDateTime()));
    pagatiConRicevuta.setRiferimentoMessaggioRichiesta(receipt.getPaymentNote());
    pagatiConRicevuta.setRiferimentoDataRichiesta(null); // @TODO: non disponibile al momento?

    CtDatiVersamentoPagatiConRicevuta datiVersamento = of.createCtDatiVersamentoPagatiConRicevuta();
    datiVersamento.setCodiceEsitoPagamento(Constants.LEGACY_PAYMENT_OUTCOME_CODE_OK);
    datiVersamento.setIdentificativoUnivocoVersamento(receipt.getCreditorReferenceId());
    datiVersamento.setCodiceContestoPagamento(receipt.getPaymentReceiptId());
    datiVersamento.setImportoTotalePagato(ConversionUtils.centsAmountToBigDecimalEuroAmount(receipt.getPaymentAmountCents()));

    installment.getTransfers().forEach(transfer -> {
      CtDatiSingoloPagamentoPagatiConRicevuta ctDatiSingoloPagamentoPagatiConRicevuta = new CtDatiSingoloPagamentoPagatiConRicevuta();
      ctDatiSingoloPagamentoPagatiConRicevuta.setIndiceDatiSingoloPagamento(transfer.getTransferIndex());
      ctDatiSingoloPagamentoPagatiConRicevuta.setIdentificativoUnivocoDovuto(installment.getIud());
      ctDatiSingoloPagamentoPagatiConRicevuta.setIdentificativoUnivocoRiscossione(receipt.getPaymentReceiptId());
      ctDatiSingoloPagamentoPagatiConRicevuta.setDatiSpecificiRiscossione(installment.getLegacyPaymentMetadata());
      ctDatiSingoloPagamentoPagatiConRicevuta.setCausaleVersamento(transfer.getRemittanceInformation());
      ctDatiSingoloPagamentoPagatiConRicevuta.setEsitoSingoloPagamento(Constants.LEGACY_PAYMENT_OUTCOME_CODE_OK);
      ctDatiSingoloPagamentoPagatiConRicevuta.setDataEsitoSingoloPagamento(ConversionUtils.toXMLGregorianCalendar(receipt.getPaymentDateTime()));
      ctDatiSingoloPagamentoPagatiConRicevuta.setSingoloImportoPagato(ConversionUtils.centsAmountToBigDecimalEuroAmount(transfer.getAmountCents()));
      ctDatiSingoloPagamentoPagatiConRicevuta.setCommissioniApplicatePSP(ConversionUtils.centsAmountToBigDecimalEuroAmount(receipt.getFeeCents()));
      if (transfer.getMbdAttachment() != null) {
        CtAllegatoRicevuta ctAllegatoRicevuta = new CtAllegatoRicevuta();
        ctAllegatoRicevuta.setTipoAllegatoRicevuta(StTipoAllegatoRicevuta.BD);
        ctAllegatoRicevuta.setTestoAllegato(transfer.getMbdAttachment().getBytes(StandardCharsets.UTF_8));
        ctDatiSingoloPagamentoPagatiConRicevuta.setAllegatoRicevuta(ctAllegatoRicevuta);
      }
      datiVersamento.getDatiSingoloPagamentos().add(ctDatiSingoloPagamentoPagatiConRicevuta);
    });

    pagatiConRicevuta.setDatiPagamento(datiVersamento);

    return pagatiConRicevuta;
  }

  private static XMLGregorianCalendar toXMLGregorianCalendar(OffsetDateTime dateTime) throws RuntimeException {
    if (dateTime == null) {
      return null;
    }

    ZonedDateTime zonedDateTime = dateTime.toLocalDateTime().atZone(ZoneId.systemDefault());
    GregorianCalendar gregorianCalendar = GregorianCalendar.from(zonedDateTime);
    try {
      return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
    } catch (Exception e) {
      throw new RuntimeException("Error converting LocalDateTime to XMLGregorianCalendar", e);
    }
  }

  private byte[] getReceiptDataById(Long receiptId, Long organizationId, String accessToken) {
    return Optional.ofNullable(fileShareService.downloadReceipt(organizationId, receiptId, accessToken))
      .map(resource -> {
        try {
          return resource.getContentAsByteArray();
        } catch (IOException ioe) {
          throw new ApplicationException(ioe);
        }
      })
      .orElseThrow(() -> new ApplicationException("receipt not found for id: " + receiptId));
  }

}
