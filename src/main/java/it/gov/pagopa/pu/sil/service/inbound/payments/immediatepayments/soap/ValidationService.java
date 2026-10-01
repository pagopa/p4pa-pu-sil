package it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments.soap;

import static it.gov.pagopa.pu.sil.util.Constants.ORDINARY_DEBT_POSITION_ORIGINS;

import it.gov.pagopa.pu.sil.connector.debtpositions.InstallmentService;
import it.gov.pagopa.pu.sil.exception.common.InvalidValueException;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiMarcaBolloDigitale;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiVersamentoDovutiEntiSecondari;
import java.math.BigDecimal;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class ValidationService {

  private final InstallmentService installmentService;

  public ValidationService(InstallmentService installmentService) {
    this.installmentService = installmentService;
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
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_STAMP,
        "Invalid length for stampDocumentHash field [4-72]",
        "Lunghezza errata campo hash documento marca da bollo digitale [4-72]"
      );
    } else if (StringUtils.length(stamp.getProvinciaResidenza()) != 2) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_STAMP,
        "Invalid length for stampProvincialResidence field [2]",
        "Lunghezza errata campo provincia residenza marca da bollo digitale [2]"
      );
    } else if (StringUtils.length(stamp.getTipoBollo()) != 2) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_STAMP,
        "Invalid length for stamptype field [2]",
        "Lunghezza errata campo tipo bollo marca da bollo digitale [2]"
        );
    }
  }

  public void validateIud(Long orgId, String iud, String accessToken) {
    Boolean isInstallmentExistsByIudIuvNav = installmentService.isInstallmentExistsByIudIuvNav(
      orgId, iud, null, null, ORDINARY_DEBT_POSITION_ORIGINS, accessToken);
    if (Boolean.TRUE.equals(isInstallmentExistsByIudIuvNav)) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_DUPLICATED_IUD,
        "Installment with iud " + iud + " already exists",
        "IUD duplicato: " + iud
      );
    }
  }

  public void validatePrimaryDebtPositionOrganization(PaaSILInviaCarrelloDovuti request, String orgIpaCode) {
    if (request.getListaDovuti() == null || CollectionUtils.isEmpty(request.getListaDovuti().getElementoListaDovutis())) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_SYSTEM_ERROR,
        "ListaDovuti is null or elementoListaDovutis is empty",
        "Dovuti non presenti"
      );
    } else if (request.getListaDovuti().getElementoListaDovutis().stream()
      .anyMatch(x -> !StringUtils.equals(x.getCodIpaEnte(), orgIpaCode))) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_ORGANIZATION,
        "codIpaEnte doesn't match with orgIpaCode",
        "L'inserimento di dovuti per enti diversi dal chiamante è deprecato"
      );
    }
  }

  public void validateSecondaryDebtPositionCount(PaaSILInviaCarrelloDovuti request, int numDebtPositions) {
    if (request.getListaDovutiEntiSecondari() != null && !CollectionUtils.isEmpty(request.getListaDovutiEntiSecondari().getElementoListaDovutiEntiSecondaris())) {
      if (numDebtPositions > 1) {
        throw new InvalidValueException(
          ErrorCodeConstants.ERROR_CODE_MULTIBENEFICIARY_THRESHOLD,
          "It is not possible to insert a multi-beneficiary payment if there is more than one debt position",
          "Non è possibile inserire un pagamento multibeneficiario se sono presenti più di un dovuto"
        );
      } else if (request.getListaDovutiEntiSecondari().getElementoListaDovutiEntiSecondaris().size() > 1) {
        throw new InvalidValueException(
          ErrorCodeConstants.ERROR_CODE_MULTIBENEFICIARY_THRESHOLD,
          "It is not possible to insert a multi-beneficiary payment with more than one element in elementoListaDovutiEntiSecondaris",
          "Non è possibile inserire pagamenti multibeneficiario con più di un dovuto secondario"
        );
      }
    }
  }

  public void validateSecondaryDebtPositionData(CtDatiVersamentoDovutiEntiSecondari secondaryTransferData, int primaryDebtPositionCount) {
    if (primaryDebtPositionCount != 1) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_MULTIBENEFICIARY_THRESHOLD,
        "It is not possible to insert a multi-beneficiary payment with more than one debt position",
        "Non è possibile inserire pagamenti multibeneficiario con più di un dovuto"
      );
    }
    if (!ValidationUtils.isValidFiscalCodeLegalEntity(secondaryTransferData.getCodiceFiscaleBeneficiario())) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_VAT_CODE,
        "Invalid secondary org vat cod: " + secondaryTransferData.getCodiceFiscaleBeneficiario(),
        "Codice fiscale ente secondario non valido: " + secondaryTransferData.getCodiceFiscaleBeneficiario()
      );
    } else if (StringUtils.isBlank(secondaryTransferData.getIbanAccreditoBeneficiario()) ||
      !ValidationUtils.isValidIban(secondaryTransferData.getIbanAccreditoBeneficiario())) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_IBAN,
        "Invalid secondary org iban: " + secondaryTransferData.getIbanAccreditoBeneficiario(),
        "IBAN accredito Ente secondario non valido [" + secondaryTransferData.getIbanAccreditoBeneficiario() + "]"
      );
    }
    if (secondaryTransferData.getImportoSingoloVersamento() == null || BigDecimal.ZERO.compareTo(secondaryTransferData.getImportoSingoloVersamento()) >= 0) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_AMOUNT,
        "Invalid importoSingoloVersamento: " + secondaryTransferData.getImportoSingoloVersamento(),
        "Importo singolo versamento non valido: " + secondaryTransferData.getImportoSingoloVersamento()
      );
    }
  }

  public void validateCartSize(int size) {
    if (size > Constants.MAX_CART_SIZE) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_CART_SIZE,
        "Invalid cart size: " + size + "/" + Constants.MAX_CART_SIZE,
        "Numero massimo dovuti nel carrello superato: " + size + "/" + Constants.MAX_CART_SIZE
      );
    } else if (size == 0) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_XML,
        "Invalid cart size",
        "Nessun dovuto presente"
      );
    }
  }
}
