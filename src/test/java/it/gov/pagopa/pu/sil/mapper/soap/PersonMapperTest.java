package it.gov.pagopa.pu.sil.mapper.soap;

import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.sil.exception.common.InvalidValueException;
import it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments.soap.ValidationService;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
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
    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> personMapper.getAndValidateDebtor(null));

    assertEquals(ErrorCodeConstants.ERROR_CODE_MISSING_DEBTOR, exception.getCode());
    assertEquals("Missing debtor", exception.getMessage());
    assertEquals("Soggetto pagatore non presente", exception.getSilFaultCustomMessage());
  }

  @Test
  void getAndValidateDebtor_InvalidEmail_ReturnsError() {
    soggettoPagatore.setEMailPagatore("invalid-email");

    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> personMapper.getAndValidateDebtor(soggettoPagatore));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_EMAIL, exception.getCode());
    assertEquals("Email is not valid", exception.getMessage());
    assertEquals("Email pagatore non valida: invalid-email", exception.getSilFaultCustomMessage());
  }

  @Test
  void getAndValidateDebtor_ValidData_ReturnsPersonDTO() {
    PersonDTO result = personMapper.getAndValidateDebtor(soggettoPagatore);

    assertNotNull(result);
    TestUtils.checkNotNullFields(result);
  }

  //endregion
}
