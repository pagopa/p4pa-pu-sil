package it.gov.pagopa.pu.sil;

import it.gov.pagopa.pu.sil.util.Utilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

public class UtilitiesTest {

  @Test
  void testGetTraceId(){
    // Given
    String expectedResult = "TRACEID";
    setTraceId(expectedResult);

    // When
    String result = Utilities.getTraceId();

    // Then
    Assertions.assertSame(expectedResult, result);
    clearTraceIdContext();
  }

  public static void setTraceId(String traceId) {
    MDC.put("traceId", traceId);
  }
  public static void clearTraceIdContext(){
    MDC.clear();
  }
}
