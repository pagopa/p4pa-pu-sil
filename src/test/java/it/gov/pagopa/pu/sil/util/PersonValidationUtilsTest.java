package it.gov.pagopa.pu.sil.util;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
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
    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> PersonValidationUtils.validateFiscalCodeDebtor(null));

    assertEquals(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, result.getFault());
    assertEquals("Identificativo univoco persona non presente", result.getDescription());
  }

  @Test
  void validateFiscalCodeDebtor_BlankFields_ReturnsError() {
    CtIdentificativoUnivocoPersonaFG personIdentifier = new CtIdentificativoUnivocoPersonaFG();
    personIdentifier.setCodiceIdentificativoUnivoco(null);
    personIdentifier.setTipoIdentificativoUnivoco(null);

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> PersonValidationUtils.validateFiscalCodeDebtor(personIdentifier));

    assertEquals(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, result.getFault());
    assertEquals("Identificativo univoco persona non valido", result.getDescription());
  }

  @Test
  void validateFiscalCodeDebtor_InvalidNaturalPersonFiscalCode_ReturnsError() {
    CtIdentificativoUnivocoPersonaFG personIdentifier = new CtIdentificativoUnivocoPersonaFG();
    personIdentifier.setCodiceIdentificativoUnivoco("INVALID_CF");
    personIdentifier.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.F);

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> PersonValidationUtils.validateFiscalCodeDebtor(personIdentifier));

    assertEquals(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, result.getFault());
    assertEquals("Codice fiscale persona fisica non valido: INVALID_CF", result.getDescription());
  }

  @Test
  void validateFiscalCodeDebtor_InvalidLegalEntityFiscalCode_ReturnsError() {
    CtIdentificativoUnivocoPersonaFG personIdentifier = new CtIdentificativoUnivocoPersonaFG();
    personIdentifier.setCodiceIdentificativoUnivoco("1234567890");
    personIdentifier.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.G);

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> PersonValidationUtils.validateFiscalCodeDebtor(personIdentifier));

    assertEquals(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, result.getFault());
    assertEquals("Codice fiscale persona giuridica non valido: 1234567890", result.getDescription());
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

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> PersonValidationUtils.validateAddress(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("Indirizzo pagatore non valido: nazione mancante", exception.getDescription());
  }

  @Test
  void validateAddress_MissingMandatoryFields_ReturnsError() {
    soggettoPagatore.setIndirizzoPagatore(null);

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> PersonValidationUtils.validateAddress(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("Indirizzo pagatore non valido: mancante un campo tra indirizzo, civico, cap, località, provincia", exception.getDescription());
  }

  @Test
  void validateAddress_InvalidNazione_ReturnsError() {
    soggettoPagatore.setNazionePagatore("INVALID");

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> PersonValidationUtils.validateAddress(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("Nazione non valida: INVALID", exception.getDescription());
  }

  @Test
  void validateAddress_ValidData_ReturnsPersonDTO() {
    Assertions.assertDoesNotThrow(() -> PersonValidationUtils.validateAddress(soggettoPagatore));
  }

  @Test
  void validateAddress_InvalidProvinceForNonItalianNation_ReturnsError() {
    soggettoPagatore.setNazionePagatore("US");
    soggettoPagatore.setProvinciaPagatore("RM");

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> PersonValidationUtils.validateAddress(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("Provincia non valida: RM (la provincia è prevista solo per la nazione IT)", exception.getDescription());
  }

  @Test
  void validateAddress_InvalidProvinceFormat_ReturnsError() {
    soggettoPagatore.setProvinciaPagatore("INVALID");

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> PersonValidationUtils.validateAddress(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("Provincia non valida: INVALID", exception.getDescription());
  }

  @Test
  void validateAddress_InvalidPostalCode_ReturnsError() {
    soggettoPagatore.setCapPagatore("INVALID");

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> PersonValidationUtils.validateAddress(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("CAP non valido: INVALID", exception.getDescription());
  }

  @Test
  void validateAddress_InvalidCivicNumber_ReturnsError() {
    soggettoPagatore.setCivicoPagatore("INVALID_TOOOOO_LONG");

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> PersonValidationUtils.validateAddress(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("Numero civico non valido: INVALID_TOOOOO_LONG", exception.getDescription());
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

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> PersonValidationUtils.validateAnonymousDebtor(debtPositionTypeOrg, debtor));

    assertEquals(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, exception.getFault());
    assertEquals("Debitore anonimo non supportato per il tipo dovuto: CODE oppure non configurato correttamente", exception.getDescription());
  }

  @Test
  void validateAnonymousDebtor_InvalidAnonymousDebtor_ReturnsError() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setCode("CODE");
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(true);
    PersonDTO debtor = new PersonDTO();
    debtor.setFiscalCode(Constants.ANONYMOUS_FISCAL_CODE);
    debtor.setEntityType(PersonEntityType.G);

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> PersonValidationUtils.validateAnonymousDebtor(debtPositionTypeOrg, debtor));

    assertEquals(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, exception.getFault());
    assertEquals("Debitore anonimo non supportato per il tipo dovuto: CODE oppure non configurato correttamente", exception.getDescription());
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

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> PersonValidationUtils.validateAnonymousDebtor(debtPositionTypeOrg, debtor));

    assertEquals(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, exception.getFault());
    assertEquals("Debitore anonimo non supportato per il tipo dovuto: CODE oppure non configurato correttamente", exception.getDescription());
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
