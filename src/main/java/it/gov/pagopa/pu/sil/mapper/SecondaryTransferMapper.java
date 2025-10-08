package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Taxonomy;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.organization.service.TaxonomyService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.pagamenti.ente.ListaDovutiEntiSecondari;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiVersamentoDovutiEntiSecondari;
import it.veneto.regione.schemas._2012.pagamenti.ente.DovutiEntiSecondari;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class SecondaryTransferMapper {

  private final TaxonomyService taxonomyService;
  private final JAXBTransformService jaxbTransformService;
  private final DebtPositionTypeService debtPositionTypeService;

  public Optional<CtDatiVersamentoDovutiEntiSecondari> mapToCtDatiVersamentoDovutiEntiSecondari(ListaDovutiEntiSecondari dovutiSecondariList) {
    //unmarshall "dovuti secondari" (if present)
    if (dovutiSecondariList != null && !CollectionUtils.isEmpty(dovutiSecondariList.getElementoListaDovutiEntiSecondaris())) {
      if (dovutiSecondariList.getElementoListaDovutiEntiSecondaris().size() > 1) {
        throw new SilFaultException(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_MULTIBENEFICIARI, "Non è possibile inserire un pagamento multibeneficiario se sono presenti più di un dovuto");
      }
      byte[] xmlDovutiSecondari = dovutiSecondariList.getElementoListaDovutiEntiSecondaris().getFirst().getDovutiEntiSecondari();
      try {
        return Optional.of(jaxbTransformService.unmarshalling(xmlDovutiSecondari, DovutiEntiSecondari.class, "/soap/wsdl/payments/PagInf_Dovuti_Pagati_6_2_0.xsd")
          .getDatiVersamentoEntiSecondari());
      } catch (ApplicationException unmarshallingException) {
        String errorMessage = "XML dovuti enti secondari non conforme: \n" +
          jaxbTransformService.getDetailUnmarshalExceptionMessage(unmarshallingException, xmlDovutiSecondari);
        log.error("error unmarshalling PaaSILInviaCarrelloDovuti dovutEntiSecondari: [{}]", errorMessage, unmarshallingException);
        throw new SilFaultException(SilFaults.PAA_XML_NON_VALIDO, errorMessage);
      }
    }
    return Optional.empty();
  }

  public void fillSecondaryTransferData(DebtPositionDTO debtPosition, CtDatiVersamentoDovutiEntiSecondari secondaryTransferData) {

    long secondaryAmount = ConversionUtils.bigDecimalEuroAmountToCentsAmount(secondaryTransferData.getImportoSingoloVersamento());
    TransferDTO secondaryTransfer = TransferDTO.builder()
      .transferIndex(2)
      .orgFiscalCode(secondaryTransferData.getCodiceFiscaleBeneficiario())
      .orgName(secondaryTransferData.getDenominazioneBeneficiario())
      .amountCents(secondaryAmount)
      .remittanceInformation(secondaryTransferData.getCausaleVersamento())
      .iban(secondaryTransferData.getIbanAccreditoBeneficiario())
      .category(ValidationUtils.getCategory(secondaryTransferData.getDatiSpecificiRiscossione()))
      .build();

    PaymentOptionDTO paymentOption = debtPosition.getPaymentOptions().getFirst();
    InstallmentDTO installment = paymentOption.getInstallments().getFirst();
    if (installment.getTransfers() != null) {
      throw new SilFaultException(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_MULTIBENEFICIARI,
        "Non è possibile inserire pagamenti multibeneficiario con più di un trasferimento secondario");
    }
    installment.setTransfers(List.of(secondaryTransfer));
    installment.setAmountCents(installment.getAmountCents() + secondaryAmount);
    paymentOption.setTotalAmountCents(installment.getAmountCents());
  }

  public void checkAndFillSupportedTransfersConfigurationForModify(InstallmentDTO installmentOnDb, InstallmentDTO installmentToSync, boolean legacyMode) {

    final int numTransfersOnDb = installmentOnDb.getTransfers()!=null ? installmentOnDb.getTransfers().size() : 0;
    final int numTransfersToSync;
    final long amountFirstTransfer;
    if(legacyMode){
      /* in legacy mode the first transfer is implicit, so the only supported configurations are:
       * - no transfers (i.e. single transfer)
       * - one transfer (i.e. multi-transfer with only one secondary transfer)
       */
      numTransfersToSync = ObjectUtils.firstNonNull(installmentToSync.getTransfers(), List.of()).size() + 1;
      if(numTransfersToSync>2){
        log.error("more than 2 transfers to sync in legacy mode [{}] for installment: {}", numTransfersToSync, installmentOnDb.getInstallmentId());
        throw new SilFaultException(SilFaults.PAA_ERRORE_RECUPERO_DOVUTI_ENTI_SECONDARI,
          "Configurazione dovuti secondari non supportata per dovuto: " + installmentOnDb.getInstallmentId());
      }

      //populate first transfer data from installmentToSync
      amountFirstTransfer = installmentToSync.getAmountCents() - Optional.ofNullable(installmentToSync.getTransfers()).stream()
        .flatMap(Collection::stream)
        .filter(transfer -> transfer.getTransferIndex() != 1)
        .map(TransferDTO::getAmountCents)
        .reduce(Long::sum)
        .orElse(0L);
    } else {
      // in native mode all transfer are explicitly passed
      numTransfersToSync = ObjectUtils.firstNonNull(installmentToSync.getTransfers(), List.of()).size();
      amountFirstTransfer = -1L; //not used
    }

    if(numTransfersOnDb != numTransfersToSync) {
      log.error("installmentOnDb transfers: {}, installmentToSync transfers: {}", installmentOnDb.getTransfers().size(),
        ObjectUtils.firstNonNull(installmentToSync.getTransfers(), List.of()).size() + 1);
      throw new SilFaultException(SilFaults.PAA_ERRORE_RECUPERO_DOVUTI_ENTI_SECONDARI,
        "Configurazione dovuti secondari non supportata per dovuto: " + installmentOnDb.getInstallmentId());
    }

    //check that only modifiable fields of transfers are different
    installmentOnDb.getTransfers().forEach(transferOnDb -> {
      if (legacyMode && transferOnDb.getTransferIndex() == 1) {
        //special handling for the first transfer in legacy mode
        transferOnDb.setAmountCents(amountFirstTransfer);
        transferOnDb.setRemittanceInformation(installmentToSync.getRemittanceInformation());
      } else {
        TransferDTO transferToSync = Optional.ofNullable(installmentToSync.getTransfers()).stream()
          .flatMap(Collection::stream)
          .filter(t -> t.getTransferIndex().equals(transferOnDb.getTransferIndex()))
          .findFirst()
          .orElseThrow(() -> new SilFaultException(SilFaults.PAA_ERRORE_RECUPERO_DOVUTI_ENTI_SECONDARI,
            "Configurazione dovuti secondari non supportata per dovuto: " + installmentOnDb.getInstallmentId()));
        if (Objects.equals(transferOnDb.getOrgFiscalCode(), transferToSync.getOrgFiscalCode()) &&
          Objects.equals(transferOnDb.getCategory(), transferToSync.getCategory()) &&
          Objects.equals(transferOnDb.getStampHashDocument(), transferToSync.getStampHashDocument()) &&
          Objects.equals(transferOnDb.getStampType(), transferToSync.getStampType()) &&
          Objects.equals(transferOnDb.getStampProvincialResidence(), transferToSync.getStampProvincialResidence()) &&
          Objects.equals(transferOnDb.getIban(), transferToSync.getIban()) &&
          Objects.equals(transferOnDb.getPostalIban(), transferToSync.getPostalIban())
        ) {
          transferOnDb.setAmountCents(transferToSync.getAmountCents());
          transferOnDb.setRemittanceInformation(transferToSync.getRemittanceInformation());
        } else {
          throw new SilFaultException(SilFaults.PAA_CAMPO_NON_MODIFICABILE,
            "Sono stati modificati campi non modificabili per il dovuto: " + installmentOnDb.getInstallmentId());
        }
      }
    });

  }

  private String retrieveAndValidateSecondaryTransferCategory(String legacyPaymentMetadata, Long organizationId, String debtPositionTypeOrgCode, String accessToken) {
    String category = ValidationUtils.getTransferCategoryFromLegacyPaymentMetadataSecondary(legacyPaymentMetadata);
    if (category == null) {
      // Retrieve the category from the DebtPositionTypeOrg using the debtPositionTypeOrgId
      DebtPositionTypeOrg debtPositionTypeOrg = debtPositionTypeService.getDebtPositionTypeOrgByOrgIdAndType(
        organizationId, debtPositionTypeOrgCode, accessToken);
      DebtPositionType debtPositionType = debtPositionTypeService.getDebtPositionTypeById(debtPositionTypeOrg.getDebtPositionTypeId(), accessToken);
      category = debtPositionType.getTaxonomyCode();
    }
    if (StringUtils.isBlank(category)) {
      throw new SilFaultException(SilFaults.PAA_ENTE_SECONDARIO_NON_VALIDO, "Codice tassonomico non presente o non valido per l'ente secondario: " + category);
    }
    Optional<Taxonomy> taxonomy = taxonomyService.getTaxonomyByTaxonomyCode(category, accessToken);
    if (taxonomy.isEmpty()) {
      throw new SilFaultException(SilFaults.PAA_ENTE_SECONDARIO_NON_VALIDO, "Codice tassonomico dei dati specifici riscossione dell'Ente Secondario non presente in archivio [" + category + "]");
    }

    return ValidationUtils.getCategory(legacyPaymentMetadata);
  }
}
