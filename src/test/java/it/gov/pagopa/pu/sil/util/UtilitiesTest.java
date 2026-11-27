package it.gov.pagopa.pu.sil.util;

import io.micrometer.common.util.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.slf4j.MDC;

import java.util.stream.Stream;

import static it.gov.pagopa.pu.sil.util.Constants.INSTALLMENT_REMITTANCE_INFORMATION_PLACEHOLDER;

public class UtilitiesTest {

  private static final String AUX_DIGIT = "3";

  public static void setTraceId(String traceId) {
    setTraceId(traceId, null);
  }
  public static void setTraceId(String traceId, String spanId) {
    MDC.put("traceId", traceId);
    MDC.put("spanId", spanId);
  }
  public static void clearTraceIdContext(){
    MDC.clear();
  }

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

  @Test
  void testGetSpanId(){
    // Given
    String expectedResult = "SPANID";
    setTraceId("TRACEID", expectedResult);

    // When
    String result = Utilities.getSpanId();

    // Then
    Assertions.assertSame(expectedResult, result);
    clearTraceIdContext();
  }

  @ParameterizedTest
  @ValueSource(strings = {"IUV","IUV1,IUV2,IUV3"})
  @NullAndEmptySource
  void testIuv2Nav(String iuv){
    // Given
    String expectedResult = StringUtils.isBlank(iuv) ? null : ("IUV".equals(iuv) ? "3IUV" : "3IUV1,3IUV2,3IUV3");

    // When
    String result = Utilities.iuv2Nav(iuv, AUX_DIGIT);

    // Then
    Assertions.assertEquals(expectedResult, result);
  }

  @ParameterizedTest
  @ValueSource(strings = {"3NAV","3NAV1,3NAV2,3NAV3"})
  @NullAndEmptySource
  void testNav2Iuv(String nav){
    // Given
    String expectedResult = StringUtils.isBlank(nav) ? null : ("3NAV".equals(nav) ? "NAV" : "NAV1,NAV2,NAV3");

    // When
    String result = Utilities.nav2Iuv(nav, AUX_DIGIT);

    // Then
    Assertions.assertEquals(expectedResult, result);
  }

  @Test
  void testInvalidNav2Iuv(){
    // Given
    String nav = "INVALID_NAV";

    // When
    IllegalArgumentException result = Assertions.assertThrows(IllegalArgumentException.class,()->Utilities.nav2Iuv(nav, AUX_DIGIT));

    // Then
    Assertions.assertTrue(result.getMessage().startsWith("Invalid NAV format: "));
  }

  @ParameterizedTest
  @CsvSource(value={
    "12345678901, false",
    "TSTTNT80A01H501O, true",
    "ANONIMO, true",
    "null, false",
    "  , false"
  }, nullValues={"null"})
  void testIsNaturalPerson(String fiscalCode, String expectedResult) {
    boolean result = Utilities.isNaturalPerson(fiscalCode);
    Assertions.assertEquals(Boolean.parseBoolean(expectedResult), result);
  }

  @ParameterizedTest
  @MethodSource("provideRemittanceInformation")
  void testResolveRemittanceInformation(String remittanceInformation, String originalRemittanceInformation, String expectedResult) {
    String result = Utilities.resolveRemittanceInformation(remittanceInformation, originalRemittanceInformation);
    Assertions.assertEquals(expectedResult, result);
  }

  private static Stream<Arguments> provideRemittanceInformation() {
    return Stream.of(
      Arguments.of("remittanceInformation", null, "remittanceInformation"),
      Arguments.of("remittanceInformation", "originalRemittanceInformation", "remittanceInformation"),
      Arguments.of(INSTALLMENT_REMITTANCE_INFORMATION_PLACEHOLDER +" with remittanceInformation", "originalRemittanceInformation", "originalRemittanceInformation")
    );
  }
}
