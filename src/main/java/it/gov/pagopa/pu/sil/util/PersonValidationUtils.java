package it.gov.pagopa.pu.sil.util;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.sil.exception.common.InvalidValueException;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtIdentificativoUnivocoPersonaFG;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtSoggettoPagatore;
import it.veneto.regione.schemas._2012.pagamenti.ente.StTipoIdentificativoUnivocoPersFG;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Optional;

public class PersonValidationUtils {

  public static void validateFiscalCodeDebtor(CtIdentificativoUnivocoPersonaFG personIdentifier) {
    if (personIdentifier == null) {
      throw new InvalidValueException(ErrorCodeConstants.ERROR_CODE_INVALID_VAT_CODE, "Unique person identifier not present", "Identificativo univoco persona non presente");
    } else if (StringUtils.isBlank(personIdentifier.getCodiceIdentificativoUnivoco()) ||
      personIdentifier.getTipoIdentificativoUnivoco() == null) {
      throw new InvalidValueException(ErrorCodeConstants.ERROR_CODE_INVALID_VAT_CODE, "Unique person identifier not valid", "Identificativo univoco persona non valido");
    } else if (personIdentifier.getTipoIdentificativoUnivoco() == StTipoIdentificativoUnivocoPersFG.F &&
      !ValidationUtils.isValidFiscalCodeNaturalPerson(personIdentifier.getCodiceIdentificativoUnivoco())) {
      throw new InvalidValueException(ErrorCodeConstants.ERROR_CODE_INVALID_VAT_CODE, "Invalid fiscal code: " + personIdentifier.getCodiceIdentificativoUnivoco(), "Codice fiscale persona fisica non valido: " + personIdentifier.getCodiceIdentificativoUnivoco());
    } else if (personIdentifier.getTipoIdentificativoUnivoco() == StTipoIdentificativoUnivocoPersFG.G &&
      !ValidationUtils.isValidFiscalCodeLegalEntity(personIdentifier.getCodiceIdentificativoUnivoco())) {
      throw new InvalidValueException(ErrorCodeConstants.ERROR_CODE_INVALID_VAT_CODE, "Invalid fiscal code: " + personIdentifier.getCodiceIdentificativoUnivoco(), "Codice fiscale persona giuridica non valido: " + personIdentifier.getCodiceIdentificativoUnivoco());
    }
  }

  public static void validateAddress(CtSoggettoPagatore soggettoPagatore) {
    /* only the following options are considered valid:
     *
     * nazione       IT    !IT  -
     * provincia     X      -   -
     * località      X      X   -
     * indirizzo     X      X   -
     * civico        X      X   -
     * cap           X      X   -
     *
     * where 'X' means mandatory , 'o' means optional , '-' means not set
     */
    if (isAddressEmpty(soggettoPagatore)) {
      return; // no address provided, valid case
    }

    if (isNationMissingWithOtherFieldsPresent(soggettoPagatore)) {
      throw new InvalidValueException(ErrorCodeConstants.ERROR_CODE_INVALID_ADDRESS, "Invalid payer address: missing country", "Indirizzo pagatore non valido: nazione mancante");
    } else if (isMandatoryFieldsMissingForItaly(soggettoPagatore)) {
      String silFaultCustomMessage = "Indirizzo pagatore non valido: mancante un campo tra indirizzo, civico, cap, località";
      String message = "Invalid payer address: missing one of the following fields: address, street number, ZIP code, city";

      if (isItalianNation(soggettoPagatore)) {
        silFaultCustomMessage += ", provincia";
        message += ", province";
      }

      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_ADDRESS,
        message,
        silFaultCustomMessage
      );
    } else if (!isValidNation(soggettoPagatore)) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_COUNTRY,
        "Invalid country: " + soggettoPagatore.getNazionePagatore(),
        "Nazione non valida: " + soggettoPagatore.getNazionePagatore()
      );
    } else if (isInvalidProvinceForNonItaly(soggettoPagatore)) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_PROVINCE,
        "Invalid province: " + soggettoPagatore.getProvinciaPagatore() + " (province field is only applicable to Italy.)",
        "Provincia non valida: " + soggettoPagatore.getProvinciaPagatore() + " (la provincia è prevista solo per la nazione IT)"
      );
    } else if (!isValidProvince(soggettoPagatore)) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_PROVINCE,
        "Invalid province: " + soggettoPagatore.getProvinciaPagatore(),
        "Provincia non valida: " + soggettoPagatore.getProvinciaPagatore()
      );
    } else if (!isValidPostalCode(soggettoPagatore)) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_POSTAL_CODE,
        "Invalid postal code: " + soggettoPagatore.getCapPagatore(),
        "CAP non valido: " + soggettoPagatore.getCapPagatore()
      );
    } else if (!isValidCivic(soggettoPagatore)) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_STREET_NUMBER,
        "Invalid street number: " + soggettoPagatore.getCivicoPagatore(),
        "Numero civico non valido: " + soggettoPagatore.getCivicoPagatore()
      );
    }
  }


  // Helper methods
  private static boolean isAddressEmpty(CtSoggettoPagatore soggettoPagatore) {
    return StringUtils.isBlank(soggettoPagatore.getIndirizzoPagatore()) &&
      StringUtils.isBlank(soggettoPagatore.getCivicoPagatore()) &&
      StringUtils.isBlank(soggettoPagatore.getCapPagatore()) &&
      StringUtils.isBlank(soggettoPagatore.getLocalitaPagatore()) &&
      StringUtils.isBlank(soggettoPagatore.getProvinciaPagatore()) &&
      StringUtils.isBlank(soggettoPagatore.getNazionePagatore());
  }

  private static boolean isNationMissingWithOtherFieldsPresent(CtSoggettoPagatore soggettoPagatore) {
    return StringUtils.isBlank(soggettoPagatore.getNazionePagatore()) && (
      StringUtils.isNotBlank(soggettoPagatore.getIndirizzoPagatore()) ||
        StringUtils.isNotBlank(soggettoPagatore.getCivicoPagatore()) ||
        StringUtils.isNotBlank(soggettoPagatore.getCapPagatore()) ||
        StringUtils.isNotBlank(soggettoPagatore.getLocalitaPagatore()) ||
        StringUtils.isNotBlank(soggettoPagatore.getProvinciaPagatore()));
  }

  private static boolean isMandatoryFieldsMissingForItaly(CtSoggettoPagatore soggettoPagatore) {
    return (StringUtils.isBlank(soggettoPagatore.getIndirizzoPagatore()) ||
      StringUtils.isBlank(soggettoPagatore.getCivicoPagatore()) ||
      StringUtils.isBlank(soggettoPagatore.getCapPagatore()) ||
      StringUtils.isBlank(soggettoPagatore.getLocalitaPagatore()) ||
      StringUtils.isBlank(soggettoPagatore.getProvinciaPagatore())) &&
      isItalianNation(soggettoPagatore);
  }

  private static boolean isItalianNation(CtSoggettoPagatore soggettoPagatore) {
    return StringUtils.equals(Locale.ITALY.getCountry(), soggettoPagatore.getNazionePagatore());
  }

  private static boolean isValidNation(CtSoggettoPagatore soggettoPagatore) {
    return Optional.ofNullable(soggettoPagatore.getNazionePagatore())
      .map(ValidationUtils::isValidISOCountry)
      .orElse(true);
  }

  private static boolean isInvalidProvinceForNonItaly(CtSoggettoPagatore soggettoPagatore) {
    return StringUtils.isNotBlank(soggettoPagatore.getProvinciaPagatore()) &&
      !isItalianNation(soggettoPagatore);
  }

  private static boolean isValidProvince(CtSoggettoPagatore soggettoPagatore) {
    return Optional.ofNullable(soggettoPagatore.getProvinciaPagatore())
      .map(ValidationUtils::isValidProvince)
      .orElse(true);
  }

  private static boolean isValidPostalCode(CtSoggettoPagatore soggettoPagatore) {
    return Optional.ofNullable(soggettoPagatore.getCapPagatore())
      .map(c -> ValidationUtils.isValidPostalCode(c, soggettoPagatore.getNazionePagatore()))
      .orElse(true);
  }

  private static boolean isValidCivic(CtSoggettoPagatore soggettoPagatore) {
    return Optional.ofNullable(soggettoPagatore.getCivicoPagatore())
      .map(ValidationUtils::isValidCivic)
      .orElse(true);
  }

  public static void validateAnonymousDebtor(DebtPositionTypeOrg debtPositionTypeOrg, PersonDTO debtor) {
    if (!ValidationUtils.verifyValidAnonymousDebtor(debtPositionTypeOrg, debtor)) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_ANONYMOUS_DEBTOR_NOT_SUPPORTED,
        "Anonymous debtor not supported for debtPositionTypeOrg " + debtPositionTypeOrg.getCode() + " or not configured correctly",
        "Debitore anonimo non supportato per il tipo dovuto: " + debtPositionTypeOrg.getCode() + " oppure non configurato correttamente"
      );
    }
  }

  public static void validateAnonymousDebtor(DebtPositionTypeOrg debtPositionTypeOrg, CtSoggettoPagatore debtor) {
    if (!ValidationUtils.verifyValidAnonymousDebtor(debtPositionTypeOrg, debtor.getIdentificativoUnivocoPagatore())) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_ANONYMOUS_DEBTOR_NOT_SUPPORTED,
        "Anonymous debtor not supported for debtPositionTypeOrg " + debtPositionTypeOrg.getCode() + " or not configured correctly",
        "Debitore anonimo non supportato per il tipo dovuto: " + debtPositionTypeOrg.getCode() + " oppure non configurato correttamente"
      );
    }
  }

}
