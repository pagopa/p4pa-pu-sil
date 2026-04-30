package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.util.PersonValidationUtils;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtSoggettoPagatore;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class PersonMapper {

  public PersonDTO getAndValidateDebtor(CtSoggettoPagatore soggettoPagatore) {
    if (soggettoPagatore == null) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Soggetto pagatore non presente");
    }
    if (StringUtils.isNotBlank(soggettoPagatore.getEMailPagatore()) && !ValidationUtils.isValidEmail(soggettoPagatore.getEMailPagatore())) {
      throw new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Email pagatore non valida: " + soggettoPagatore.getEMailPagatore());
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
