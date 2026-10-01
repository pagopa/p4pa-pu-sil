package it.gov.pagopa.pu.sil.mapper.soap;

import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.sil.exception.common.InvalidValueException;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import it.gov.pagopa.pu.sil.util.PersonValidationUtils;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtSoggettoPagatore;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class PersonMapper {

  public PersonDTO getAndValidateDebtor(CtSoggettoPagatore soggettoPagatore) {
    if (soggettoPagatore == null) {
      throw new InvalidValueException(ErrorCodeConstants.ERROR_CODE_MISSING_DEBTOR, "Missing debtor", "Soggetto pagatore non presente");
    }
    if (StringUtils.isNotBlank(soggettoPagatore.getEMailPagatore()) && !ValidationUtils.isValidEmail(soggettoPagatore.getEMailPagatore())) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_EMAIL,
        "Email is not valid",
        "Email pagatore non valida: " + soggettoPagatore.getEMailPagatore()
      );
    }
    PersonValidationUtils.validateFiscalCodeDebtor(soggettoPagatore.getIdentificativoUnivocoPagatore());

    PersonValidationUtils.validateAddress(soggettoPagatore);

    return PersonDTO.builder()
      .fiscalCode(soggettoPagatore.getIdentificativoUnivocoPagatore().getCodiceIdentificativoUnivoco())
      .entityType(PersonEntityType.fromValue(soggettoPagatore.getIdentificativoUnivocoPagatore().getTipoIdentificativoUnivoco().value()))
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

}
