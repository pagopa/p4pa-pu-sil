package it.gov.pagopa.pu.sil.exception.soap.transcoder;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.BaseBusinessException;
import it.gov.pagopa.pu.sil.exception.InvalidValueException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.exception.soap.SoapFaultTranscoded;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.Map;

abstract class BaseSoapExceptionTranscoderTest {

  private final SilFaults unauthorizedFault;
  private final SilFaults systemErrorFault;
  private final Map<String, SilFaults> errorCode2SilFault;

  BaseSoapExceptionTranscoderTest(SilFaults unauthorizedFault, SilFaults systemErrorFault, Map<String, SilFaults> errorCode2SilFault) {
    this.unauthorizedFault = unauthorizedFault;
    this.systemErrorFault = systemErrorFault;
    this.errorCode2SilFault = errorCode2SilFault;
  }

  protected abstract BaseSoapExceptionTranscoder getExceptionTranscoder();

  @Test
  void givenAuthorizationDeniedExceptionWhenTranscodeExceptionThenHandleIt(){
    // Given
    AuthorizationDeniedException exception = new AuthorizationDeniedException("Test Exception");

    // When
    SoapFaultTranscoded result = getExceptionTranscoder().transcodeException(exception);

    // Then
    Assertions.assertEquals(
      new SoapFaultTranscoded(
        unauthorizedFault,
        "Utente non autorizzato"
      ),
      result
    );
  }

  @Test
  void givenSilFaultExceptionWhenTranscodeExceptionThenHandleIt(){
    // Given
    SilFaults silFault = SilFaults.PAA_ENTE_NON_VALIDO;
    SilFaultException exception = new SilFaultException(silFault, "Test Exception");

    // When
    SoapFaultTranscoded result = getExceptionTranscoder().transcodeException(exception);

    // Then
    Assertions.assertEquals(
      new SoapFaultTranscoded(
        silFault,
        "Test Exception"
      ),
      result
    );
  }

  @Test
  void givenUnhandledBaseBusinessExceptionCodeWhenTranscodeExceptionThenHandleIt(){
    // Given
    BaseBusinessException exception = new InvalidValueException("UNHANDLED_CODE", "Test Exception");

    // When
    SoapFaultTranscoded result = getExceptionTranscoder().transcodeException(exception);

    // Then
    Assertions.assertEquals(
      new SoapFaultTranscoded(
        systemErrorFault,
        "Errore di sistema"
      ),
      result
    );
  }

  @Test
  void givenHandledBaseBusinessExceptionCodeWhenTranscodeExceptionThenHandleIt(){
    errorCode2SilFault.forEach((code, silFault) -> {
      // Given
      BaseBusinessException exception = new InvalidValueException(code, "Test Exception");

      // When
      SoapFaultTranscoded result = getExceptionTranscoder().transcodeException(exception);

      // Then
      Assertions.assertEquals(
        new SoapFaultTranscoded(
          silFault,
          silFault.description()
        ),
        result
      );
    });
  }
}
