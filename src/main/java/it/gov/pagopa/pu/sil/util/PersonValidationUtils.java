package it.gov.pagopa.pu.sil.util;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtIdentificativoUnivocoPersonaFG;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtSoggettoPagatore;
import it.veneto.regione.schemas._2012.pagamenti.ente.StTipoIdentificativoUnivocoPersFG;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Optional;

public class PersonValidationUtils {

  public static void validateFiscalCodeDebtor(CtIdentificativoUnivocoPersonaFG personIdentifier) {
    if (personIdentifier == null) {
      throw new SilFaultException(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, "Identificativo univoco persona non presente");
    } else if (StringUtils.isBlank(personIdentifier.getCodiceIdentificativoUnivoco()) ||
      personIdentifier.getTipoIdentificativoUnivoco() == null) {
      throw new SilFaultException(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, "Identificativo univoco persona non valido");
    } else if (personIdentifier.getTipoIdentificativoUnivoco() == StTipoIdentificativoUnivocoPersFG.F &&
      !ValidationUtils.isValidFiscalCodeNaturalPerson(personIdentifier.getCodiceIdentificativoUnivoco())) {
      throw new SilFaultException(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, "Codice fiscale persona fisica non valido: " + personIdentifier.getCodiceIdentificativoUnivoco());
    } else if (personIdentifier.getTipoIdentificativoUnivoco() == StTipoIdentificativoUnivocoPersFG.G &&
      !ValidationUtils.isValidFiscalCodeLegalEntity(personIdentifier.getCodiceIdentificativoUnivoco())) {
      throw new SilFaultException(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, "Codice fiscale persona giuridica non valido: " + personIdentifier.getCodiceIdentificativoUnivoco());
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
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Indirizzo pagatore non valido: nazione mancante");
    } else if (isMandatoryFieldsMissingForItaly(soggettoPagatore)) {
      String message = "Indirizzo pagatore non valido: mancante un campo tra indirizzo, civico, cap, località";
      if (isItalianNation(soggettoPagatore)) {
        message += ", provincia";
      }
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, message);
    } else if (!isValidNation(soggettoPagatore)) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Nazione non valida: " + soggettoPagatore.getNazionePagatore());
    } else if (isInvalidProvinceForNonItaly(soggettoPagatore)) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Provincia non valida: " + soggettoPagatore.getProvinciaPagatore() +
        " (la provincia è prevista solo per la nazione IT)");
    } else if (!isValidProvince(soggettoPagatore)) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Provincia non valida: " + soggettoPagatore.getProvinciaPagatore());
    } else if (!isValidPostalCode(soggettoPagatore)) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "CAP non valido: " + soggettoPagatore.getCapPagatore());
    } else if (!isValidCivic(soggettoPagatore)) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Numero civico non valido: " + soggettoPagatore.getCivicoPagatore());
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
      throw new SilFaultException(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, "Debitore anonimo non supportato per il tipo dovuto: " + debtPositionTypeOrg.getCode() + " oppure non configurato correttamente");
    }
  }

  public static void validateAnonymousDebtor(DebtPositionTypeOrg debtPositionTypeOrg, CtSoggettoPagatore debtor) {
    if (!ValidationUtils.verifyValidAnonymousDebtor(debtPositionTypeOrg, debtor.getIdentificativoUnivocoPagatore())) {
      throw new SilFaultException(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, "Debitore anonimo non supportato per il tipo dovuto: " + debtPositionTypeOrg.getCode() + " oppure non configurato correttamente");
    }
  }

}
