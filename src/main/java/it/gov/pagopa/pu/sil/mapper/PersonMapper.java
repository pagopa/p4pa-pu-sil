package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.EntityTypeEnum;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtSoggettoPagatore;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class PersonMapper {

  private final ValidationService validationService;

  public PersonMapper(ValidationService validationService) {
    this.validationService = validationService;
  }

  public void validateAnonymousDebtor(DebtPositionTypeOrg debtPositionTypeOrg, PersonDTO debtor) {
    if (!ValidationUtils.verifyValidAnonymousDebtor(debtPositionTypeOrg, debtor)) {
      throw new SilFaultException(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, "Debitore anonimo non supportato per il tipo dovuto: " + debtPositionTypeOrg.getCode() + " oppure non configurato correttamente");
    }
  }

  public PersonDTO getAndValidateDebtor(CtSoggettoPagatore soggettoPagatore) {
    if (soggettoPagatore == null) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Soggetto pagatore non presente");
    }
    if (StringUtils.isNotBlank(soggettoPagatore.getEMailPagatore()) && !ValidationUtils.isValidEmail(soggettoPagatore.getEMailPagatore())) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Email pagatore non valida: " + soggettoPagatore.getEMailPagatore());
    }
    validationService.validateFiscalCodeDebtor(soggettoPagatore.getIdentificativoUnivocoPagatore());

    validateAddress(soggettoPagatore);

    return PersonDTO.builder()
      .fiscalCode(soggettoPagatore.getIdentificativoUnivocoPagatore().getCodiceIdentificativoUnivoco())
      .entityType(EntityTypeEnum.fromValue(soggettoPagatore.getIdentificativoUnivocoPagatore().getTipoIdentificativoUnivoco().value()))
      .fullName(soggettoPagatore.getAnagraficaPagatore())
      .email(soggettoPagatore.getEMailPagatore())
      .address(soggettoPagatore.getIndirizzoPagatore())
      .civic(soggettoPagatore.getCivicoPagatore())
      .postalCode(soggettoPagatore.getCapPagatore())
      .location(soggettoPagatore.getLocalitaPagatore())
      .province(soggettoPagatore.getProvinciaPagatore())
      .nation(soggettoPagatore.getNazionePagatore())
      .build();
  }

  private void validateAddress(CtSoggettoPagatore soggettoPagatore) {
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
    if (StringUtils.isBlank(soggettoPagatore.getNazionePagatore()) && (
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
}
