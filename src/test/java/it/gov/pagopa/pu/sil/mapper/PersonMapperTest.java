package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.EntityTypeEnum;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtIdentificativoUnivocoPersonaFG;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtSoggettoPagatore;
import it.veneto.regione.schemas._2012.pagamenti.ente.StTipoIdentificativoUnivocoPersFG;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class PersonMapperTest {

  @InjectMocks
  private PersonMapper personMapper;

  @Mock
  private ValidationService validationServiceMock;

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


  //region: validateAnonymousDebtor
  @Test
  void validateAnonymousDebtor_InvalidDebtPositionTypeOrg_ReturnsError() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setCode("CODE");
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(false);
    PersonDTO debtor = new PersonDTO();
    debtor.setFiscalCode(Constants.ANONYMOUS_FISCAL_CODE);
    debtor.setEntityType(EntityTypeEnum.F);

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> personMapper.validateAnonymousDebtor(debtPositionTypeOrg, debtor));

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
    debtor.setEntityType(EntityTypeEnum.G);

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> personMapper.validateAnonymousDebtor(debtPositionTypeOrg, debtor));

    assertEquals(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, exception.getFault());
    assertEquals("Debitore anonimo non supportato per il tipo dovuto: CODE oppure non configurato correttamente", exception.getDescription());
  }

  @Test
  void validateAnonymousDebtor_ValidAnonymousDebtor_ReturnsNull() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(true);
    PersonDTO debtor = new PersonDTO();
    debtor.setFiscalCode(Constants.ANONYMOUS_FISCAL_CODE);
    debtor.setEntityType(EntityTypeEnum.F);

    Assertions.assertDoesNotThrow(() -> personMapper.validateAnonymousDebtor(debtPositionTypeOrg, debtor));
  }

  @Test
  void validateAnonymousDebtor_ValidNonAnonymousDebtor_ReturnsNull() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(false);
    PersonDTO debtor = new PersonDTO();
    debtor.setFiscalCode("NON ANONYMOUS");
    debtor.setEntityType(EntityTypeEnum.F);

    Assertions.assertDoesNotThrow(() -> personMapper.validateAnonymousDebtor(debtPositionTypeOrg, debtor));
  }
  //endregion

  //region: getAndValidateDebtor
  @Test
  void getAndValidateDebtor_NullSoggettoPagatore_ReturnsError() {
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> personMapper.getAndValidateDebtor(null));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("Soggetto pagatore non presente", exception.getDescription());
  }

  @Test
  void getAndValidateDebtor_InvalidEmail_ReturnsError() {
    soggettoPagatore.setEMailPagatore("invalid-email");

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> personMapper.getAndValidateDebtor(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("Email pagatore non valida: invalid-email", exception.getDescription());
  }

  @Test
  void getAndValidateDebtor_MissingNazioneWithAddressFields_ReturnsError() {
    soggettoPagatore.setNazionePagatore(null);

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> personMapper.getAndValidateDebtor(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("Indirizzo pagatore non valido: nazione mancante", exception.getDescription());
  }

  @Test
  void getAndValidateDebtor_MissingMandatoryFields_ReturnsError() {
    soggettoPagatore.setIndirizzoPagatore(null);

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> personMapper.getAndValidateDebtor(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("Indirizzo pagatore non valido: mancante un campo tra indirizzo, civico, cap, località, provincia", exception.getDescription());
  }

  @Test
  void getAndValidateDebtor_InvalidNazione_ReturnsError() {
    soggettoPagatore.setNazionePagatore("INVALID");

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> personMapper.getAndValidateDebtor(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("Nazione non valida: INVALID", exception.getDescription());
  }

  @Test
  void getAndValidateDebtor_ValidData_ReturnsPersonDTO() {
    PersonDTO result = personMapper.getAndValidateDebtor(soggettoPagatore);

    assertNotNull(result);
    TestUtils.checkNotNullFields(result);
  }

  @Test
  void getAndValidateDebtor_InvalidProvinceForNonItalianNation_ReturnsError() {
    soggettoPagatore.setNazionePagatore("US");
    soggettoPagatore.setProvinciaPagatore("RM");

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> personMapper.getAndValidateDebtor(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("Provincia non valida: RM (la provincia è prevista solo per la nazione IT)", exception.getDescription());
  }

  @Test
  void getAndValidateDebtor_InvalidProvinceFormat_ReturnsError() {
    soggettoPagatore.setProvinciaPagatore("INVALID");

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> personMapper.getAndValidateDebtor(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("Provincia non valida: INVALID", exception.getDescription());
  }

  @Test
  void getAndValidateDebtor_InvalidPostalCode_ReturnsError() {
    soggettoPagatore.setCapPagatore("INVALID");

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> personMapper.getAndValidateDebtor(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("CAP non valido: INVALID", exception.getDescription());
  }

  @Test
  void getAndValidateDebtor_InvalidCivicNumber_ReturnsError() {
    soggettoPagatore.setCivicoPagatore("INVALID_TOOOOO_LONG");

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> personMapper.getAndValidateDebtor(soggettoPagatore));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("Numero civico non valido: INVALID_TOOOOO_LONG", exception.getDescription());
  }
  //endregion
}
