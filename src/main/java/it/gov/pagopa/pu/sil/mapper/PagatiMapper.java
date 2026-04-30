package it.gov.pagopa.pu.sil.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.ReceiptDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.ReceiptService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.veneto.regione.schemas._2012.pagamenti.ente.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PagatiMapper {

  public static final String PAGATI_VERSIONE_OGGETTO = "6.2.0";

  private final JAXBTransformService jaxbTransformService;
  private final ReceiptService receiptService;

  /**
   * Maps a debt position and installment to an encoded PagatiConRicevuta object.
   *
   * @param installment  the installment associated with the debt position
   * @param organization the organization of the payment
   * @param accessToken  the access token for authorization
   * @return the encoded PagatiConRicevuta as byte array
   */
  public byte[] mapDebtPositionsToEncodedPagatiConRicevuta(InstallmentDTO installment, Organization organization, String accessToken) {
    PagatiConRicevuta pagatiConRicevuta = mapToPagatiConRicevuta(installment, organization, true, accessToken);
    return jaxbTransformService.marshallingAsBytes(pagatiConRicevuta, PagatiConRicevuta.class);
  }

  /**
   * Maps a debt position and installment to an encoded Pagati object.
   *
   * @param installment  the installment associated with the debt position
   * @param organization the organization of the payment
   * @param accessToken  the access token for authorization
   * @return the encoded Pagati as byte array
   */
  public byte[] mapDebtPositionsToEncodedPagati(InstallmentDTO installment, Organization organization, String accessToken) {
    // since the Pagati object has the same structure of PagatiConRicevuta, with just some missing fields,
    // we can reuse the same mapping logic and then convert it to Pagati using jackson-databind.
    PagatiConRicevuta pagatiConRicevuta = mapToPagatiConRicevuta(installment, organization, false, accessToken);
    Pagati pagati = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .convertValue(pagatiConRicevuta, Pagati.class);
    return jaxbTransformService.marshallingAsBytes(pagati, Pagati.class);
  }

  private PagatiConRicevuta mapToPagatiConRicevuta(InstallmentDTO installment, Organization organization, boolean withReceiptFields, String accessToken) {
    log.debug("mapping installment[{}] org[{}] withReceipt[{}] with PagatiConRicevuta",
      installment.getInstallmentId(), organization.getOrganizationId(), withReceiptFields);
    ReceiptDTO receipt = receiptService.getReceiptById(installment.getReceiptId(), accessToken);

    PagatiConRicevuta pagatiConRicevuta = new PagatiConRicevuta();

    // Set top-level fields to null
    pagatiConRicevuta.setVersioneOggetto(PAGATI_VERSIONE_OGGETTO);
    pagatiConRicevuta.setDominio(mapToCtDominio(organization));
    pagatiConRicevuta.setIdentificativoMessaggioRicevuta(receipt.getPaymentReceiptId());
    pagatiConRicevuta.setDataOraMessaggioRicevuta(ConversionUtils.toXMLGregorianCalendar(receipt.getPaymentDateTime()));
    pagatiConRicevuta.setRiferimentoMessaggioRichiesta(receipt.getPaymentNote());
    pagatiConRicevuta.setIstitutoAttestante(mapToCtIstitutoAttestante(receipt));
    pagatiConRicevuta.setEnteBeneficiario(mapToCtEnteBeneficiario(organization));
    pagatiConRicevuta.setSoggettoPagatore(mapToCtSoggettoPagatore(receipt));
    pagatiConRicevuta.setDatiPagamento(mapToCtDatiVersamentoPagatiConRicevuta(installment, receipt, withReceiptFields));

    return pagatiConRicevuta;
  }

  private CtDominio mapToCtDominio(Organization organization) {
    CtDominio dominio = new CtDominio();
    dominio.setIdentificativoDominio(organization.getOrgFiscalCode());
    return dominio;
  }

  private CtIstitutoAttestante mapToCtIstitutoAttestante(ReceiptDTO receipt) {
    CtIstitutoAttestante istitutoAttestante = new CtIstitutoAttestante();
    CtIdentificativoUnivoco identificativoUnivoco = new CtIdentificativoUnivoco();
    identificativoUnivoco.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivoco.B);
    identificativoUnivoco.setCodiceIdentificativoUnivoco(receipt.getIdPsp());
    istitutoAttestante.setIdentificativoUnivocoAttestante(identificativoUnivoco);
    istitutoAttestante.setDenominazioneAttestante(receipt.getPspCompanyName());
    return istitutoAttestante;
  }

  private CtEnteBeneficiario mapToCtEnteBeneficiario(Organization organization) {
    CtEnteBeneficiario enteBeneficiario = new CtEnteBeneficiario();
    CtIdentificativoUnivocoPersonaG identificativoUnivocoPersonaG = new CtIdentificativoUnivocoPersonaG();
    identificativoUnivocoPersonaG.setCodiceIdentificativoUnivoco(organization.getOrgFiscalCode());
    identificativoUnivocoPersonaG.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersG.G);
    enteBeneficiario.setIdentificativoUnivocoBeneficiario(identificativoUnivocoPersonaG);
    enteBeneficiario.setDenominazioneBeneficiario(organization.getOrgName());
    return enteBeneficiario;
  }

  private CtSoggettoPagatore mapToCtSoggettoPagatore(ReceiptDTO receipt) {
    PersonDTO debtor = receipt.getDebtor();
    CtSoggettoPagatore soggettoPagatore = new CtSoggettoPagatore();
    CtIdentificativoUnivocoPersonaFG identificativoUnivocoPersonaFG = new CtIdentificativoUnivocoPersonaFG();
    identificativoUnivocoPersonaFG.setCodiceIdentificativoUnivoco(debtor.getFiscalCode());
    identificativoUnivocoPersonaFG.setTipoIdentificativoUnivoco(
      Utilities.isNaturalPerson(debtor.getFiscalCode()) ? StTipoIdentificativoUnivocoPersFG.F : StTipoIdentificativoUnivocoPersFG.G);
    soggettoPagatore.setIdentificativoUnivocoPagatore(identificativoUnivocoPersonaFG);
    soggettoPagatore.setAnagraficaPagatore(debtor.getFullName());
    soggettoPagatore.setNazionePagatore(debtor.getNation());
    soggettoPagatore.setProvinciaPagatore(debtor.getProvince());
    soggettoPagatore.setLocalitaPagatore(debtor.getLocation());
    soggettoPagatore.setIndirizzoPagatore(debtor.getAddress());
    soggettoPagatore.setCivicoPagatore(debtor.getCivic());
    soggettoPagatore.setCapPagatore(debtor.getPostalCode());
    soggettoPagatore.setEMailPagatore(debtor.getEmail());
    return soggettoPagatore;
  }

  private CtDatiVersamentoPagatiConRicevuta mapToCtDatiVersamentoPagatiConRicevuta(InstallmentDTO installment, ReceiptDTO receipt, boolean withReceiptFields) {
    CtDatiVersamentoPagatiConRicevuta datiPagamento = new CtDatiVersamentoPagatiConRicevuta();
    datiPagamento.setIdentificativoUnivocoVersamento(receipt.getCreditorReferenceId());
    datiPagamento.setCodiceContestoPagamento(receipt.getPaymentReceiptId());
    datiPagamento.setImportoTotalePagato(ConversionUtils.centsAmountToBigDecimalEuroAmount(receipt.getPaymentAmountCents()));
    // legacy outcome code is always OK code since no negative outcome receipt are sent by Nodo Pagamenti
    datiPagamento.setCodiceEsitoPagamento(Constants.LEGACY_PAYMENT_OUTCOME_CODE_OK);
    installment.getTransfers().forEach(transfer -> {
      CtDatiSingoloPagamentoPagatiConRicevuta ctDatiSingoloPagamentoPagatiConRicevuta = new CtDatiSingoloPagamentoPagatiConRicevuta();
      ctDatiSingoloPagamentoPagatiConRicevuta.setIndiceDatiSingoloPagamento(transfer.getTransferIndex());
      ctDatiSingoloPagamentoPagatiConRicevuta.setIdentificativoUnivocoDovuto(installment.getIud());
      ctDatiSingoloPagamentoPagatiConRicevuta.setIdentificativoUnivocoRiscossione(receipt.getPaymentReceiptId());
      ctDatiSingoloPagamentoPagatiConRicevuta.setDatiSpecificiRiscossione(installment.getLegacyPaymentMetadata());
      ctDatiSingoloPagamentoPagatiConRicevuta.setCausaleVersamento(Utilities.resolveRemittanceInformation(transfer.getRemittanceInformation(), installment.getOriginalRemittanceInformation()));
      ctDatiSingoloPagamentoPagatiConRicevuta.setEsitoSingoloPagamento(Constants.LEGACY_PAYMENT_OUTCOME_CODE_OK);
      ctDatiSingoloPagamentoPagatiConRicevuta.setDataEsitoSingoloPagamento(ConversionUtils.toXMLGregorianCalendar(receipt.getPaymentDateTime()));
      ctDatiSingoloPagamentoPagatiConRicevuta.setSingoloImportoPagato(ConversionUtils.centsAmountToBigDecimalEuroAmount(transfer.getAmountCents()));
      ctDatiSingoloPagamentoPagatiConRicevuta.setCommissioniApplicatePSP(ConversionUtils.centsAmountToBigDecimalEuroAmount(receipt.getFeeCents()));
      if (withReceiptFields && transfer.getStampHashDocument() != null) {
        CtAllegatoRicevuta ctAllegatoRicevuta = new CtAllegatoRicevuta();
        ctAllegatoRicevuta.setTipoAllegatoRicevuta(StTipoAllegatoRicevuta.BD);
        ctDatiSingoloPagamentoPagatiConRicevuta.setAllegatoRicevuta(ctAllegatoRicevuta);
      }
      datiPagamento.getDatiSingoloPagamentos().add(ctDatiSingoloPagamentoPagatiConRicevuta);
    });

    return datiPagamento;
  }

}
