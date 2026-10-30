package it.gov.pagopa.pu.sil.util;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.sil.exception.common.InvalidValueException;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtIdentificativoUnivocoPersonaFG;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtSoggettoPagatore;
import it.veneto.regione.schemas._2012.pagamenti.ente.StTipoIdentificativoUnivocoPersFG;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonValidationUtilsTest {

  private CtSoggettoPagatore soggettoPagatore;

  @BeforeEach
  void setup() {
    soggettoPagatore = new CtSoggettoPagatore();
    soggettoPagatore.setNazionePagatore(Locale.ITALY.getCountry());
    soggettoPagatore.setIndirizzoPagatore("Via Roma");
    soggettoPagatore.setCivicoPagatore("10");
    soggettoPagatore.setCapPagatore("00100");
    soggettoPagatore.setLocalitaPagatore("Roma");
    soggettoPagatore.setProvinciaPagatore("RM");
    soggettoPagatore.setAnagraficaPagatore("Utente Test");
    soggettoPagatore.setIdentificativoUnivocoPagatore(new CtIdentificativoUnivocoPersonaFG());
    soggettoPagatore.getIdentificativoUnivocoPagatore().setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.F);
    soggettoPagatore.getIdentificativoUnivocoPagatore().setCodiceIdentificativoUnivoco("TSTTNT80A01H501O");
    soggettoPagatore.setEMailPagatore("utente@email.it");
  }

  //region: validateFiscalCodeDebtor
  @Test
  void validateFiscalCodeDebtor_NullPersonIdentifier_ReturnsError() {
    InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> PersonValidationUtils.validateFiscalCodeDebtor(null));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_VAT_CODE, result.getCode());
    assertEquals("Unique person identifier not present", result.getMessage());
    assertEquals("Identificativo univoco persona non presente", result.getSilFaultCustomMessage());
  }

  @Test
  void validateFiscalCodeDebtor_BlankFields_ReturnsError() {
    CtIdentificativoUnivocoPersonaFG personIdentifier = new CtIdentificativoUnivocoPersonaFG();
    personIdentifier.setCodiceIdentificativoUnivoco(null);
    personIdentifier.setTipoIdentificativoUnivoco(null);

    InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> PersonValidationUtils.validateFiscalCodeDebtor(personIdentifier));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_VAT_CODE, result.getCode());
    assertEquals("Unique person identifier not valid", result.getMessage());
    assertEquals("Identificativo univoco persona non valido", result.getSilFaultCustomMessage());
  }

  @Test
  void validateFiscalCodeDebtor_InvalidNaturalPersonFiscalCode_ReturnsError() {
    CtIdentificativoUnivocoPersonaFG personIdentifier = new CtIdentificativoUnivocoPersonaFG();
    personIdentifier.setCodiceIdentificativoUnivoco("INVALID_CF");
    personIdentifier.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.F);

    InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> PersonValidationUtils.validateFiscalCodeDebtor(personIdentifier));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_VAT_CODE, result.getCode());
    assertEquals("Invalid fiscal code: INVALID_CF", result.getMessage());
    assertEquals("Codice fiscale persona fisica non valido: INVALID_CF", result.getSilFaultCustomMessage());
  }

  @Test
  void validateFiscalCodeDebtor_InvalidLegalEntityFiscalCode_ReturnsError() {
    CtIdentificativoUnivocoPersonaFG personIdentifier = new CtIdentificativoUnivocoPersonaFG();
    personIdentifier.setCodiceIdentificativoUnivoco("1234567890");
    personIdentifier.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.G);

    InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> PersonValidationUtils.validateFiscalCodeDebtor(personIdentifier));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_VAT_CODE, result.getCode());
    assertEquals("Invalid fiscal code: 1234567890", result.getMessage());
    assertEquals("Codice fiscale persona giuridica non valido: 1234567890", result.getSilFaultCustomMessage());
  }

  @Test
  void validateFiscalCodeDebtor_ValidNaturalPersonFiscalCode_ReturnsNull() {
    CtIdentificativoUnivocoPersonaFG personIdentifier = new CtIdentificativoUnivocoPersonaFG();
    personIdentifier.setCodiceIdentificativoUnivoco("TSTTNT80A01H501O");
    personIdentifier.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.F);

    Assertions.assertDoesNotThrow(() -> PersonValidationUtils.validateFiscalCodeDebtor(personIdentifier));
  }

  @Test
  void validateFiscalCodeDebtor_ValidLegalEntityFiscalCode_ReturnsNull() {
    CtIdentificativoUnivocoPersonaFG personIdentifier = new CtIdentificativoUnivocoPersonaFG();
    personIdentifier.setCodiceIdentificativoUnivoco("12345678901");
    personIdentifier.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.G);

    Assertions.assertDoesNotThrow(() -> PersonValidationUtils.validateFiscalCodeDebtor(personIdentifier));
  }
  //endregion

  //region: validateAddress
  @ParameterizedTest
  @ValueSource(strings = {"indirizzo", "civico", "cap", "localita", "provincia"})
  void validateAddress_MissingNazioneWithAddressFields_ReturnsError(String testCase) {
    soggettoPagatore.setNazionePagatore(null);
    if (!testCase.equals("indirizzo")) {
      soggettoPagatore.setIndirizzoPagatore(null);
    }
    if (!testCase.equals("civico")) {
      soggettoPagatore.setCivicoPagatore(null);
    }
    if (!testCase.equals("cap")) {
      soggettoPagatore.setCapPagatore(null);
    }
    if (!testCase.equals("localita")) {
      soggettoPagatore.setLocalitaPagatore(null);
    }
    if (!testCase.equals("provincia")) {
      soggettoPagatore.setProvinciaPagatore(null);
    }

    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> PersonValidationUtils.validateAddress(soggettoPagatore));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_ADDRESS, exception.getCode());
    assertEquals("Invalid payer address: missing country", exception.getMessage());
    assertEquals("Indirizzo pagatore non valido: nazione mancante", exception.getSilFaultCustomMessage());
  }

  @Test
  void validateAddress_MissingMandatoryFields_ReturnsError() {
    soggettoPagatore.setIndirizzoPagatore(null);

    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> PersonValidationUtils.validateAddress(soggettoPagatore));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_ADDRESS, exception.getCode());
    assertEquals("Invalid payer address: missing one of the following fields: address, street number, ZIP code, city, province", exception.getMessage());
    assertEquals("Indirizzo pagatore non valido: mancante un campo tra indirizzo, civico, cap, località, provincia", exception.getSilFaultCustomMessage());
  }

  @Test
  void validateAddress_InvalidNazione_ReturnsError() {
    soggettoPagatore.setNazionePagatore("INVALID");

    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> PersonValidationUtils.validateAddress(soggettoPagatore));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_COUNTRY, exception.getCode());
    assertEquals("Invalid country: INVALID", exception.getMessage());
    assertEquals("Nazione non valida: INVALID", exception.getSilFaultCustomMessage());
  }

  @Test
  void validateAddress_ValidData_ReturnsPersonDTO() {
    Assertions.assertDoesNotThrow(() -> PersonValidationUtils.validateAddress(soggettoPagatore));
  }

  @Test
  void validateAddress_ValidEmptyData_ReturnsPersonDTO() {
    soggettoPagatore.setCapPagatore(null);
    soggettoPagatore.setCivicoPagatore(null);
    soggettoPagatore.setIndirizzoPagatore(null);
    soggettoPagatore.setNazionePagatore(null);
    soggettoPagatore.setLocalitaPagatore(null);
    soggettoPagatore.setProvinciaPagatore(null);
    Assertions.assertDoesNotThrow(() -> PersonValidationUtils.validateAddress(soggettoPagatore));
  }

  @Test
  void validateAddress_InvalidProvinceForNonItalianNation_ReturnsError() {
    soggettoPagatore.setNazionePagatore("US");
    soggettoPagatore.setProvinciaPagatore("RM");

    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> PersonValidationUtils.validateAddress(soggettoPagatore));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_PROVINCE, exception.getCode());
    assertEquals("Invalid province: RM (province field is only applicable to Italy.)", exception.getMessage());
    assertEquals("Provincia non valida: RM (la provincia è prevista solo per la nazione IT)", exception.getSilFaultCustomMessage());
  }

  @Test
  void validateAddress_InvalidProvinceFormat_ReturnsError() {
    soggettoPagatore.setProvinciaPagatore("INVALID");

    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> PersonValidationUtils.validateAddress(soggettoPagatore));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_PROVINCE, exception.getCode());
    assertEquals("Invalid province: INVALID", exception.getMessage());
    assertEquals("Provincia non valida: INVALID", exception.getSilFaultCustomMessage());
  }

  @Test
  void validateAddress_InvalidPostalCode_ReturnsError() {
    soggettoPagatore.setCapPagatore("INVALID");

    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> PersonValidationUtils.validateAddress(soggettoPagatore));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_POSTAL_CODE, exception.getCode());
    assertEquals("Invalid postal code: INVALID", exception.getMessage());
    assertEquals("CAP non valido: INVALID", exception.getSilFaultCustomMessage());
  }

  @Test
  void validateAddress_InvalidCivicNumber_ReturnsError() {
    soggettoPagatore.setCivicoPagatore("INVALID_TOOOOO_LONG");

    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> PersonValidationUtils.validateAddress(soggettoPagatore));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_STREET_NUMBER, exception.getCode());
    assertEquals("Invalid street number: INVALID_TOOOOO_LONG", exception.getMessage());
    assertEquals("Numero civico non valido: INVALID_TOOOOO_LONG", exception.getSilFaultCustomMessage());
  }
  //endregion

  //region: validateAnonymousDebtor
  @Test
  void validateAnonymousDebtor_InvalidDebtPositionTypeOrg_ReturnsError() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setCode("CODE");
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(false);
    PersonDTO debtor = new PersonDTO();
    debtor.setFiscalCode(Constants.ANONYMOUS_FISCAL_CODE);
    debtor.setEntityType(PersonEntityType.F);

    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> PersonValidationUtils.validateAnonymousDebtor(debtPositionTypeOrg, debtor));

    assertEquals(ErrorCodeConstants.ERROR_CODE_ANONYMOUS_DEBTOR_NOT_SUPPORTED, exception.getCode());
    assertEquals("Anonymous debtor not supported for debtPositionTypeOrg CODE or not configured correctly", exception.getMessage());
    assertEquals("Debitore anonimo non supportato per il tipo dovuto: CODE oppure non configurato correttamente", exception.getSilFaultCustomMessage());
  }

  @Test
  void validateAnonymousDebtor_InvalidAnonymousDebtor_ReturnsError() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setCode("CODE");
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(true);
    PersonDTO debtor = new PersonDTO();
    debtor.setFiscalCode(Constants.ANONYMOUS_FISCAL_CODE);
    debtor.setEntityType(PersonEntityType.G);

    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> PersonValidationUtils.validateAnonymousDebtor(debtPositionTypeOrg, debtor));

    assertEquals(ErrorCodeConstants.ERROR_CODE_ANONYMOUS_DEBTOR_NOT_SUPPORTED, exception.getCode());
    assertEquals("Anonymous debtor not supported for debtPositionTypeOrg CODE or not configured correctly", exception.getMessage());
    assertEquals("Debitore anonimo non supportato per il tipo dovuto: CODE oppure non configurato correttamente", exception.getSilFaultCustomMessage());
  }

  @Test
  void validateAnonymousDebtor_ValidAnonymousDebtor_ReturnsNull() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(true);
    PersonDTO debtor = new PersonDTO();
    debtor.setFiscalCode(Constants.ANONYMOUS_FISCAL_CODE);
    debtor.setEntityType(PersonEntityType.F);

    Assertions.assertDoesNotThrow(() -> PersonValidationUtils.validateAnonymousDebtor(debtPositionTypeOrg, debtor));
  }

  @Test
  void validateAnonymousDebtor_ValidNonAnonymousDebtor_ReturnsNull() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(false);
    PersonDTO debtor = new PersonDTO();
    debtor.setFiscalCode("NON ANONYMOUS");
    debtor.setEntityType(PersonEntityType.F);

    Assertions.assertDoesNotThrow(() -> PersonValidationUtils.validateAnonymousDebtor(debtPositionTypeOrg, debtor));
  }
  //endregion

  //region: validateAnonymousDebtor-CtSoggettoPagatore
  @Test
  void validateAnonymousDebtorCtSoggettoPagatore_InvalidDebtPositionTypeOrg_ReturnsError() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setCode("CODE");
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(false);
    CtSoggettoPagatore debtor = new CtSoggettoPagatore();
    CtIdentificativoUnivocoPersonaFG identificativoUnivocoPersonaFG = new CtIdentificativoUnivocoPersonaFG();
    identificativoUnivocoPersonaFG.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.F);
    identificativoUnivocoPersonaFG.setCodiceIdentificativoUnivoco(Constants.ANONYMOUS_FISCAL_CODE);
    debtor.setIdentificativoUnivocoPagatore(identificativoUnivocoPersonaFG);

    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> PersonValidationUtils.validateAnonymousDebtor(debtPositionTypeOrg, debtor));

    assertEquals(ErrorCodeConstants.ERROR_CODE_ANONYMOUS_DEBTOR_NOT_SUPPORTED, exception.getCode());
    assertEquals("Anonymous debtor not supported for debtPositionTypeOrg CODE or not configured correctly", exception.getMessage());
    assertEquals("Debitore anonimo non supportato per il tipo dovuto: CODE oppure non configurato correttamente", exception.getSilFaultCustomMessage());
  }

  @Test
  void validateAnonymousDebtorCtSoggettoPagatore_ValidAnonymousDebtor_ReturnsNull() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(true);
    CtSoggettoPagatore debtor = new CtSoggettoPagatore();
    CtIdentificativoUnivocoPersonaFG identificativoUnivocoPersonaFG = new CtIdentificativoUnivocoPersonaFG();
    identificativoUnivocoPersonaFG.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.F);
    identificativoUnivocoPersonaFG.setCodiceIdentificativoUnivoco(Constants.ANONYMOUS_FISCAL_CODE);
    debtor.setIdentificativoUnivocoPagatore(identificativoUnivocoPersonaFG);

    Assertions.assertDoesNotThrow(() -> PersonValidationUtils.validateAnonymousDebtor(debtPositionTypeOrg, debtor));
  }
  //endregion
}
