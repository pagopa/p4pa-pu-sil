package it.gov.pagopa.pu.sil.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.time.*;
import java.util.Date;
import java.util.stream.Stream;

class ConversionUtilsTest {
    static Stream<Arguments> stringEuroAmountToCentsAmountProvider() {
        return Stream.of(
            Arguments.of("123.45", 12345L),
            Arguments.of("0.00", 0L),
            Arguments.of(null, null),
            Arguments.of("123,45", 12345L), // Comma as decimal separator
            Arguments.of("123,456", 12346L) // Rounding up
        );
    }

    @ParameterizedTest
    @MethodSource("stringEuroAmountToCentsAmountProvider")
    void testStringEuroAmount2CentsAmount(String euroAmount, Long expected) {
        assertEquals(expected, ConversionUtils.stringEuroAmountToCentsAmount(euroAmount));
    }

    static Stream<Arguments> centsAmountToBigDecimalEuroAmountProvider() {
        return Stream.of(
            Arguments.of(12345L, new BigDecimal("123.45")),
            Arguments.of(0L, new BigDecimal("0.00")),
            Arguments.of(null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("centsAmountToBigDecimalEuroAmountProvider")
    void testCentsAmount2BigDecimalEuroAmount(Long cents, BigDecimal expected) {
        assertEquals(expected, ConversionUtils.centsAmountToBigDecimalEuroAmount(cents));
    }

    static Stream<Arguments> toXMLGregorianCalendarProvider() {
        OffsetDateTime odt = OffsetDateTime.of(2024, 6, 1, 12, 30, 0, 0, ZoneOffset.ofHours(2));
        return Stream.of(
            Arguments.of(odt, true),
            Arguments.of(null, false)
        );
    }

    @ParameterizedTest
    @MethodSource("toXMLGregorianCalendarProvider")
    void testOffsetDateTime2XMLGregorianCalendar(OffsetDateTime input, boolean shouldNotBeNull) {
        XMLGregorianCalendar result = ConversionUtils.toXMLGregorianCalendar(input);
        if (shouldNotBeNull) {
            assertNotNull(result);
            assertEquals(input.toLocalDate().getYear(), result.getYear());
            assertEquals(input.toLocalDate().getMonthValue(), result.getMonth());
            assertEquals(input.toLocalDate().getDayOfMonth(), result.getDay());
        } else {
            assertNull(result);
        }
    }

    static Stream<Arguments> toOffsetDateTimeFromXMLGregorianCalendarProvider() {
        OffsetDateTime odt = OffsetDateTime.of(2024, 6, 1, 12, 30, 0, 0, ZoneOffset.ofHours(2));
        XMLGregorianCalendar xmlCal = ConversionUtils.toXMLGregorianCalendar(odt);
        return Stream.of(
            Arguments.of(xmlCal, odt.withOffsetSameInstant(ZoneId.of("Europe/Rome").getRules().getOffset(odt.toInstant()))),
            Arguments.of(null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("toOffsetDateTimeFromXMLGregorianCalendarProvider")
    void testXMLGregorianCalendar2OffsetDateTime(XMLGregorianCalendar input, OffsetDateTime expected) {
        OffsetDateTime result = ConversionUtils.toOffsetDateTime(input);
        if (expected == null) {
            assertNull(result);
        } else {
            assertNotNull(result);
            assertEquals(expected.toLocalDateTime(), result.toLocalDateTime());
        }
    }

    static Stream<Arguments> toOffsetDateTimeFromDateProvider() {
        Date now = new Date();
        return Stream.of(
            Arguments.of(now, OffsetDateTime.ofInstant(now.toInstant(), ZoneId.systemDefault())),
            Arguments.of(null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("toOffsetDateTimeFromDateProvider")
    void testDate2OffsetDateTime(Date input, OffsetDateTime expected) {
        OffsetDateTime result = ConversionUtils.toOffsetDateTime(input);
        if (expected == null) {
            assertNull(result);
        } else {
            assertNotNull(result);
            assertEquals(expected.toInstant().toEpochMilli(), result.toInstant().toEpochMilli());
        }
    }

    static Stream<Arguments> localDate2RomeMaxTimeProvider() {
        LocalDate date = LocalDate.of(2024, 6, 1);
        OffsetDateTime expected = date.atTime(LocalTime.MAX).atZone(ConversionUtils.ZONE_ID_ROME).toOffsetDateTime();
        OffsetDateTime max = ConversionUtils.MAX_EXPIRATION_DATE.atZone(ConversionUtils.ZONE_ID_ROME).toOffsetDateTime();
        return Stream.of(
            Arguments.of(date, expected),
            Arguments.of(null, max)
        );
    }

    @ParameterizedTest
    @MethodSource("localDate2RomeMaxTimeProvider")
    void testLocalDate2RomeMaxTime(LocalDate input, OffsetDateTime expected) {
        assertEquals(expected, ConversionUtils.localDate2RomeMaxTime(input));
    }

    static Stream<Arguments> atEndOfDayProvider() {
        LocalDate date = LocalDate.of(2024, 6, 1);
        LocalDateTime expected = LocalDateTime.of(date, LocalTime.MAX);
        return Stream.of(
            Arguments.of(date, expected),
            Arguments.of(null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("atEndOfDayProvider")
    void testLocalDate2LocalDateTimeAtEndOfDay(LocalDate input, LocalDateTime expected) {
        assertEquals(expected, ConversionUtils.atEndOfDay(input));
    }
}
