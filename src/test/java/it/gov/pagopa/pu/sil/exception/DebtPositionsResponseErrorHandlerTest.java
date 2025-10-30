package it.gov.pagopa.pu.sil.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApiClientConfig;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.util.TestUtils;
import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import uk.co.jemos.podam.api.PodamFactory;

@ExtendWith(MockitoExtension.class)
class DebtPositionsResponseErrorHandlerTest {

  private static final String BASE_URL = "http://example.com";
  private static final URI DUMMY_URI = URI.create(BASE_URL);
  public static final HttpStatusCode STATUS_400 = HttpStatusCode.valueOf(400);

  private DebtPositionsResponseErrorHandler errorHandler;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    DebtPositionsApiClientConfig clientConfig = DebtPositionsApiClientConfig.builder()
      .baseUrl(BASE_URL)
      .printBodyWhenError(false)
      .build();
    errorHandler = new DebtPositionsResponseErrorHandler(clientConfig);
  }

  // TODO: fix this
  @Test
  void handleError_shouldTranscodeAndThrowSilFaultExceptionForMappedCode()
    throws IOException {
    ClientHttpResponse response = podamFactory.manufacturePojo(ClientHttpResponse.class);

    Executable exec = () -> errorHandler.handleError(response, STATUS_400, DUMMY_URI, HttpMethod.POST);
    SilFaultException exception = assertThrows(SilFaultException.class, exec);
    assertEquals(SilFaults.PAA_IUV_NON_VALIDO, exception.getFault());
  }

  // TODO: fix this
  @ParameterizedTest
  @ValueSource(strings = {"", "ERROR", "[MALFORMED"})
  void extractErrorCode_shouldThrowPaaSystemExceptionWhenCodeIsNotValid(String debtPositionsErrorMessage)
    throws IOException {
    Executable exec = () -> errorHandler.handleError(null, STATUS_400, DUMMY_URI, HttpMethod.POST);
    SilFaultException exception = assertThrows(SilFaultException.class, exec);

    assertEquals(SilFaults.PAA_SYSTEM_ERROR, exception.getFault());
    assertTrue(exception.getDescription()
      .contains("Errore esterno con codice non trovato"));
  }

}
