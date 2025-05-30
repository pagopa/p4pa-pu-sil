package it.gov.pagopa.pu.sil;

import io.micrometer.common.util.StringUtils;
import it.gov.pagopa.pu.sil.util.Utilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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

  @ParameterizedTest
  @ValueSource(strings = "IUV")
  @NullAndEmptySource
  void testIuv2Nav(String iuv){
    // Given
    String expectedResult = StringUtils.isBlank(iuv) ? null : "3" + iuv;

    // When
    String result = Utilities.iuv2Nav(iuv);

    // Then
    Assertions.assertEquals(expectedResult, result);
  }

  @ParameterizedTest
  @ValueSource(strings = "3NAV")
  @NullAndEmptySource
  void testNav2Iuv(String nav){
    // Given
    String expectedResult = StringUtils.isBlank(nav) ? null : nav.substring(1);

    // When
    String result = Utilities.nav2Iuv(nav);

    // Then
    Assertions.assertEquals(expectedResult, result);
  }

  @Test
  void testInvalidNav2Iuv(){
    // Given
    String nav = "INVALID_NAV";

    // When
    IllegalArgumentException result = Assertions.assertThrows(IllegalArgumentException.class,()->Utilities.nav2Iuv(nav));

    // Then
    Assertions.assertTrue(result.getMessage().startsWith("Invalid NAV format: "));
  }

  public static void setTraceId(String traceId) {
    MDC.put("traceId", traceId);
  }
  public static void clearTraceIdContext(){
    MDC.clear();
  }
}
