package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
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
  void getAndValidateDebtor_ValidData_ReturnsPersonDTO() {
    PersonDTO result = personMapper.getAndValidateDebtor(soggettoPagatore);

    assertNotNull(result);
    TestUtils.checkNotNullFields(result);
  }

  //endregion
}
