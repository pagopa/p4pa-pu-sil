package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.EntityTypeEnum;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtIdentificativoUnivocoPersonaFG;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtSoggettoPagatore;
import it.veneto.regione.schemas._2012.pagamenti.ente.StTipoIdentificativoUnivocoPersFG;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PersonMapperTest {

  @InjectMocks
  private PersonMapper personMapper;

  @Mock
  private ValidationService validationServiceMock;

  private CtSoggettoPagatore soggettoPagatore;

  @BeforeEach
  public void setup(){
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

    Pair<SilFaults, String> result = personMapper.validateAnonymousDebtor(debtPositionTypeOrg, debtor);

    assertNotNull(result);
    assertEquals(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, result.getLeft());
    assertEquals("Debitore anonimo non supportato per il tipo dovuto: CODE oppure non configurato correttamente", result.getRight());
  }

  @Test
  void validateAnonymousDebtor_InvalidAnonymousDebtor_ReturnsError() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setCode("CODE");
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(true);
    PersonDTO debtor = new PersonDTO();
    debtor.setFiscalCode(Constants.ANONYMOUS_FISCAL_CODE);
    debtor.setEntityType(EntityTypeEnum.G);

    Pair<SilFaults, String> result = personMapper.validateAnonymousDebtor(debtPositionTypeOrg, debtor);

    assertNotNull(result);
    assertEquals(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, result.getLeft());
    assertEquals("Debitore anonimo non supportato per il tipo dovuto: CODE oppure non configurato correttamente", result.getRight());
  }

  @Test
  void validateAnonymousDebtor_ValidAnonymousDebtor_ReturnsNull() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(true);
    PersonDTO debtor = new PersonDTO();
    debtor.setFiscalCode(Constants.ANONYMOUS_FISCAL_CODE);
    debtor.setEntityType(EntityTypeEnum.F);

    Pair<SilFaults, String> result = personMapper.validateAnonymousDebtor(debtPositionTypeOrg, debtor);

    assertNull(result);
  }

  @Test
  void validateAnonymousDebtor_ValidNonAnonymousDebtor_ReturnsNull() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(false);
    PersonDTO debtor = new PersonDTO();
    debtor.setFiscalCode("NON ANONYMOUS");
    debtor.setEntityType(EntityTypeEnum.F);

    Pair<SilFaults, String> result = personMapper.validateAnonymousDebtor(debtPositionTypeOrg, debtor);

    assertNull(result);
  }
  //endregion

  //region: getAndValidateDebtor
  @Test
  void getAndValidateDebtor_NullSoggettoPagatore_ReturnsError() {
    Triple<PersonDTO, SilFaults, String> result = personMapper.getAndValidateDebtor(null);

    assertNotNull(result);
    assertNull(result.getLeft());
    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, result.getMiddle());
    assertEquals("Soggetto pagatore non presente", result.getRight());
  }

  @Test
  void getAndValidateDebtor_InvalidEmail_ReturnsError() {
    soggettoPagatore.setEMailPagatore("invalid-email");

    Triple<PersonDTO, SilFaults, String> result = personMapper.getAndValidateDebtor(soggettoPagatore);

    assertNotNull(result);
    assertNull(result.getLeft());
    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, result.getMiddle());
    assertEquals("Email pagatore non valida: invalid-email", result.getRight());
  }

  @Test
  void getAndValidateDebtor_MissingNazioneWithAddressFields_ReturnsError() {
    soggettoPagatore.setNazionePagatore(null);

    Triple<PersonDTO, SilFaults, String> result = personMapper.getAndValidateDebtor(soggettoPagatore);

    assertNotNull(result);
    assertNull(result.getLeft());
    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, result.getMiddle());
    assertEquals("Indirizzo pagatore non valido: nazione mancante", result.getRight());
  }

  @Test
  void getAndValidateDebtor_MissingMandatoryFields_ReturnsError() {
    soggettoPagatore.setIndirizzoPagatore(null);

    Triple<PersonDTO, SilFaults, String> result = personMapper.getAndValidateDebtor(soggettoPagatore);

    assertNotNull(result);
    assertNull(result.getLeft());
    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, result.getMiddle());
    assertTrue(result.getRight().contains("mancante un campo tra indirizzo, civico, cap, località, provincia"));
  }

  @Test
  void getAndValidateDebtor_InvalidNazione_ReturnsError() {
    soggettoPagatore.setNazionePagatore("INVALID");

    Triple<PersonDTO, SilFaults, String> result = personMapper.getAndValidateDebtor(soggettoPagatore);

    assertNotNull(result);
    assertNull(result.getLeft());
    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, result.getMiddle());
    assertEquals("Nazione non valida: INVALID", result.getRight());
  }

  @Test
  void getAndValidateDebtor_ValidData_ReturnsPersonDTO() {
    Triple<PersonDTO, SilFaults, String> result = personMapper.getAndValidateDebtor(soggettoPagatore);

    assertNotNull(result);
    assertNotNull(result.getLeft());
    assertNull(result.getMiddle());
    assertNull(result.getRight());
  }

  @Test
  void getAndValidateDebtor_InvalidProvinceForNonItalianNation_ReturnsError() {
    soggettoPagatore.setNazionePagatore("US");
    soggettoPagatore.setProvinciaPagatore("RM");

    Triple<PersonDTO, SilFaults, String> result = personMapper.getAndValidateDebtor(soggettoPagatore);

    assertNotNull(result);
    assertNull(result.getLeft());
    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, result.getMiddle());
    assertEquals("Provincia non valida: RM (la provincia è prevista solo per la nazione IT)", result.getRight());
  }

  @Test
  void getAndValidateDebtor_InvalidProvinceFormat_ReturnsError() {
    soggettoPagatore.setProvinciaPagatore("INVALID");

    Triple<PersonDTO, SilFaults, String> result = personMapper.getAndValidateDebtor(soggettoPagatore);

    assertNotNull(result);
    assertNull(result.getLeft());
    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, result.getMiddle());
    assertEquals("Provincia non valida: INVALID", result.getRight());
  }

  @Test
  void getAndValidateDebtor_InvalidPostalCode_ReturnsError() {
    soggettoPagatore.setCapPagatore("INVALID");

    Triple<PersonDTO, SilFaults, String> result = personMapper.getAndValidateDebtor(soggettoPagatore);

    assertNotNull(result);
    assertNull(result.getLeft());
    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, result.getMiddle());
    assertEquals("CAP non valido: INVALID", result.getRight());
  }

  @Test
  void getAndValidateDebtor_InvalidCivicNumber_ReturnsError() {
    soggettoPagatore.setCivicoPagatore("INVALID_TOOOOO_LONG");

    Triple<PersonDTO, SilFaults, String> result = personMapper.getAndValidateDebtor(soggettoPagatore);

    assertNotNull(result);
    assertNull(result.getLeft());
    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, result.getMiddle());
    assertEquals("Numero civico non valido: INVALID_TOOOOO_LONG", result.getRight());
  }
  //endregion
}
