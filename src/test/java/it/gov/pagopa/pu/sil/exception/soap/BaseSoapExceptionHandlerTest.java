package it.gov.pagopa.pu.sil.exception.soap;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.soap.transcoder.BaseSoapExceptionTranscoder;
import it.gov.pagopa.pu.sil.util.Utilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.co.jemos.podam.common.Holder;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.mockito.Mockito.when;

abstract class BaseSoapExceptionHandlerTest<F> {

  protected abstract BaseSoapExceptionHandler<F> getExceptionHandler();
  protected abstract BaseSoapExceptionTranscoder getExceptionTranscoderMock();

  @Test
  void test() {
    // Given
    Object response = new Object();
    String expectedFaultId = "test-trace-id";
    int expectedSerial = 123456789;

    Holder<F> faultHolder = new Holder<>();
    BiConsumer<Object, F> faultSetter = (o, f) -> {
      Assertions.assertSame(response, o);
      faultHolder.setValue(f);
    };

    Exception exception = new Exception("Test Exception");

    SoapFaultTranscoded expectedTranscode = new SoapFaultTranscoded(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Test Fault");
    when(getExceptionTranscoderMock().transcodeException(Mockito.same(exception)))
      .thenReturn(expectedTranscode);

    // When
    Function<Exception, Object> exceptionHandlerFunction = getExceptionHandler()
      .buildExceptionHandlerFunction(response, faultSetter);
    try(MockedStatic<Utilities> utilitiesMockedStatic = Mockito.mockStatic(Utilities.class)) {
      utilitiesMockedStatic.when(Utilities::getTraceId).thenReturn(expectedFaultId);
      utilitiesMockedStatic.when(Utilities::systemTimeSecondsFrom2025).thenReturn(expectedSerial);

      exceptionHandlerFunction.apply(exception);
    }

    // Then
    F result = faultHolder.getValue();
    Assertions.assertNotNull(result);
    assertFaultContent(result, expectedFaultId, expectedSerial, expectedTranscode);
  }

  protected abstract void assertFaultContent(F result, String expectedFaultId, int expectedSerial, SoapFaultTranscoded expectedTranscode);
}
