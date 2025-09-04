package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.connector.debtpositions.InstallmentService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiMarcaBolloDigitale;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiVersamentoDovutiEntiSecondari;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;

import static it.gov.pagopa.pu.sil.util.Constants.ORDINARY_DEBT_POSITION_ORIGINS;

@Service
public class ValidationService {

  private final InstallmentService installmentService;

  public ValidationService(InstallmentService installmentService) {
    this.installmentService = installmentService;
  }

  public void validateDebtPositionTypeOrg(DebtPositionTypeOrg debtPositionTypeOrg, String debtPositionTypeOrgCode) {
    if (debtPositionTypeOrg == null) {
      throw new SilFaultException(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, "Tipo dovuto non valido: " + debtPositionTypeOrgCode);
    } else if (Boolean.FALSE.equals(debtPositionTypeOrg.getFlagActive())) {
      throw new SilFaultException(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO, "Tipo dovuto non abilitato: " + debtPositionTypeOrgCode);
    }
  }

  public void validateStamp(CtDatiSingoloVersamentoDovuti versamento) {
    if (versamento.getDatiMarcaBolloDigitale() == null) {
      return; //valid no-stamp data
    }
    CtDatiMarcaBolloDigitale stamp = versamento.getDatiMarcaBolloDigitale();
    if (StringUtils.isBlank(stamp.getTipoBollo()) &&
      StringUtils.isBlank(stamp.getHashDocumento()) &&
      StringUtils.isBlank(stamp.getProvinciaResidenza())) {
      versamento.setDatiMarcaBolloDigitale(null);
      return; //valid no-stamp data
    }

    //invalid stamp data
    if (StringUtils.length(stamp.getHashDocumento()) < 4 || StringUtils.length(stamp.getHashDocumento()) > 72) {
      throw new SilFaultException(SilFaults.PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA, "Lunghezza errata campo hash documento marca da bollo digitale [4-72]");
    } else if (StringUtils.length(stamp.getProvinciaResidenza()) != 2) {
      throw new SilFaultException(SilFaults.PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA, "Lunghezza errata campo provincia residenza marca da bollo digitale [2]");
    } else if (StringUtils.length(stamp.getTipoBollo()) != 2) {
      throw new SilFaultException(SilFaults.PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA, "Lunghezza errata campo tipo bollo marca da bollo digitale [2]");
    }
  }

  public void validateIud(Long orgId, String iud, String accessToken) {
    Boolean isInstallmentExistsByIudIuvNav = installmentService.isInstallmentExistsByIudIuvNav(
      orgId, iud, null, null, ORDINARY_DEBT_POSITION_ORIGINS, accessToken);
    if (Boolean.TRUE.equals(isInstallmentExistsByIudIuvNav)) {
      throw new SilFaultException(SilFaults.PAA_IUD_DUPLICATO, "IUD duplicato: " + iud);
    }
  }

  public void validatePaymentData(CtDatiSingoloVersamentoDovuti versamento) {
    if (versamento.getImportoSingoloVersamento() == null || BigDecimal.ZERO.compareTo(versamento.getImportoSingoloVersamento()) >= 0) {
      throw new SilFaultException(SilFaults.PAA_IMPORTO_SINGOLO_VERSAMENTO_NON_VALIDO, "Importo singolo versamento non valido: " + versamento.getImportoSingoloVersamento());
    }
    if (StringUtils.isBlank(versamento.getDatiSpecificiRiscossione()) || !ValidationUtils.isValidLegacyPaymentMetadata(versamento.getDatiSpecificiRiscossione())) {
      throw new SilFaultException(SilFaults.PAA_DATI_SPECIFICI_RISCOSSIONE_NON_VALIDO, "Dati specifici riscossione non validi: " + versamento.getDatiSpecificiRiscossione());
    }
    if (!ValidationUtils.verifyBalanceAmount(versamento.getBilancio(), versamento.getImportoSingoloVersamento())) {
      throw new SilFaultException(SilFaults.PAA_IMPORTO_BILANCIO_NON_VALIDO, "Importo bilancio non valido");
    }
    if (StringUtils.isBlank(versamento.getCausaleVersamento())) {
      throw new SilFaultException(SilFaults.PAA_CAUSALE_NON_PRESENTE, "Causale versamento non presente o non valida");
    }
  }

  public void validatePrimaryDebtPositionOrganization(PaaSILInviaCarrelloDovuti request, String orgIpaCode) {
    if (request.getListaDovuti() == null || CollectionUtils.isEmpty(request.getListaDovuti().getElementoListaDovutis())) {
      throw new SilFaultException(SilFaults.PAA_SYSTEM_ERROR, "Dovuti non presenti");
    } else if (request.getListaDovuti().getElementoListaDovutis().stream()
      .anyMatch(x -> !StringUtils.equals(x.getCodIpaEnte(), orgIpaCode))) {
      throw new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "L'inserimento di dovuti per enti diversi dal chiamante è deprecato");
    }
  }

  public void validateSecondaryDebtPositionCount(PaaSILInviaCarrelloDovuti request, int numDebtPositions) {
    if (request.getListaDovutiEntiSecondari() != null && !CollectionUtils.isEmpty(request.getListaDovutiEntiSecondari().getElementoListaDovutiEntiSecondaris())) {
      if (numDebtPositions > 1) {
        throw new SilFaultException(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_MULTIBENEFICIARI, "Non è possibile inserire un pagamento multibeneficiario se sono presenti più di un dovuto");
      } else if (request.getListaDovutiEntiSecondari().getElementoListaDovutiEntiSecondaris().size() > 1) {
        throw new SilFaultException(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_MULTIBENEFICIARI, "Non è possibile inserire pagamenti multibeneficiario con più di un dovuto secondario");
      }
    }
  }

  public void validateSecondaryDebtPositionData(CtDatiVersamentoDovutiEntiSecondari secondaryTransferData, int primaryDebtPositionCount) {
    if (primaryDebtPositionCount != 1) {
      throw new SilFaultException(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_MULTIBENEFICIARI, "Non è possibile inserire pagamenti multibeneficiario con più di un dovuto");
    }
    if (!ValidationUtils.isValidFiscalCodeLegalEntity(secondaryTransferData.getCodiceFiscaleBeneficiario())) {
      throw new SilFaultException(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, "Codice fiscale ente secondario non valido: " + secondaryTransferData.getCodiceFiscaleBeneficiario());
    } else if (StringUtils.isBlank(secondaryTransferData.getIbanAccreditoBeneficiario()) ||
      !ValidationUtils.isValidIban(secondaryTransferData.getIbanAccreditoBeneficiario())) {
      throw new SilFaultException(SilFaults.PAA_ENTE_SECONDARIO_NON_VALIDO, "IBAN accredito Ente secondario non valido [" + secondaryTransferData.getIbanAccreditoBeneficiario() + "]");
    }
    if (secondaryTransferData.getImportoSingoloVersamento() == null || BigDecimal.ZERO.compareTo(secondaryTransferData.getImportoSingoloVersamento()) >= 0) {
      throw new SilFaultException(SilFaults.PAA_IMPORTO_SINGOLO_VERSAMENTO_NON_VALIDO, "Importo singolo versamento non valido: " + secondaryTransferData.getImportoSingoloVersamento());
    }
    if (!ValidationUtils.isValidLegacyPaymentMetadataSecondary(secondaryTransferData.getDatiSpecificiRiscossione())) {
      throw new SilFaultException(SilFaults.PAA_DATI_SPECIFICI_RISCOSSIONE_NON_VALIDO, "Dati specifici riscossione non validi: " + secondaryTransferData.getDatiSpecificiRiscossione());
    }
  }

}
