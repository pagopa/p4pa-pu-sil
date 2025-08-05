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
    if (StringUtils.isBlank(soggettoPagatore.getIndirizzoPagatore()) &&
      StringUtils.isBlank(soggettoPagatore.getCivicoPagatore()) &&
      StringUtils.isBlank(soggettoPagatore.getCapPagatore()) &&
      StringUtils.isBlank(soggettoPagatore.getLocalitaPagatore()) &&
      StringUtils.isBlank(soggettoPagatore.getProvinciaPagatore()) &&
      StringUtils.isBlank(soggettoPagatore.getNazionePagatore())
    ) {
      // no address provided, valid case
    } else if (StringUtils.isBlank(soggettoPagatore.getNazionePagatore()) && (
      StringUtils.isNotBlank(soggettoPagatore.getIndirizzoPagatore()) ||
        StringUtils.isNotBlank(soggettoPagatore.getCivicoPagatore()) ||
        StringUtils.isNotBlank(soggettoPagatore.getCapPagatore()) ||
        StringUtils.isNotBlank(soggettoPagatore.getLocalitaPagatore()) ||
        StringUtils.isNotBlank(soggettoPagatore.getProvinciaPagatore())
    )) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Indirizzo pagatore non valido: nazione mancante");
    } else if (StringUtils.isBlank(soggettoPagatore.getIndirizzoPagatore()) ||
      StringUtils.isBlank(soggettoPagatore.getCivicoPagatore()) ||
      StringUtils.isBlank(soggettoPagatore.getCapPagatore()) ||
      StringUtils.isBlank(soggettoPagatore.getLocalitaPagatore()) ||
      StringUtils.isBlank(soggettoPagatore.getProvinciaPagatore()) &&
        StringUtils.equals(Locale.ITALY.getCountry(), soggettoPagatore.getNazionePagatore())
    ) {
      String message = "Indirizzo pagatore non valido: mancante un campo tra indirizzo, civico, cap, località";
      if (StringUtils.equals(Locale.ITALY.getCountry(), soggettoPagatore.getNazionePagatore())) {
        message += ", provincia";
      }
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, message);
    } else if (!Optional.ofNullable(soggettoPagatore.getNazionePagatore()).map(ValidationUtils::isValidISOCountry).orElse(true)) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Nazione non valida: " + soggettoPagatore.getNazionePagatore());
    } else if (StringUtils.isNotBlank(soggettoPagatore.getProvinciaPagatore()) &&
      !StringUtils.equals(Locale.ITALY.getCountry(), soggettoPagatore.getNazionePagatore())) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Provincia non valida: " + soggettoPagatore.getProvinciaPagatore() +
        " (la provincia è prevista solo per la nazione IT)");
    } else if (!Optional.ofNullable(soggettoPagatore.getProvinciaPagatore()).map(ValidationUtils::isValidProvince).orElse(true)) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Provincia non valida: " + soggettoPagatore.getProvinciaPagatore());
    } else if (!Optional.ofNullable(soggettoPagatore.getCapPagatore())
      .map(c -> ValidationUtils.isValidPostalCode(c, soggettoPagatore.getNazionePagatore())).orElse(true)) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "CAP non valido: " + soggettoPagatore.getCapPagatore());
    } else if (!Optional.ofNullable(soggettoPagatore.getCivicoPagatore()).map(ValidationUtils::isValidCivic).orElse(true)) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Numero civico non valido: " + soggettoPagatore.getCivicoPagatore());
    }
  }

  public static void validateAnonymousDebtor(DebtPositionTypeOrg debtPositionTypeOrg, PersonDTO debtor) {
    if (!ValidationUtils.verifyValidAnonymousDebtor(debtPositionTypeOrg, debtor)) {
      throw new SilFaultException(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, "Debitore anonimo non supportato per il tipo dovuto: " + debtPositionTypeOrg.getCode() + " oppure non configurato correttamente");
    }
  }

  public static void validateAnonymousDebtor(DebtPositionTypeOrg debtPositionTypeOrg, CtSoggettoPagatore debtor) {
    if (!ValidationUtils.verifyValidAnonymousDebtor(debtPositionTypeOrg, debtor.getIdentificativoUnivocoPagatore() )) {
      throw new SilFaultException(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, "Debitore anonimo non supportato per il tipo dovuto: " + debtPositionTypeOrg.getCode() + " oppure non configurato correttamente");
    }
  }

}
