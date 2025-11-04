package it.gov.pagopa.pu.sil.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateUtilsTest {
  @Test
  void givenLocalDateWhenToOffsetDateTimeStartOfTheDayThenOk() {
    OffsetDateTime expected = LocalDate.now().atStartOfDay(Constants.ZONEID).toOffsetDateTime();

    OffsetDateTime result = DateUtils.toOffsetDateTimeStartOfTheDay(
      LocalDate.now());

    assertTrue(expected.isEqual(result));
  }

  @Test
  void givenNullLocalDateWhenToOffsetDateTimeStartOfTheDayThenReturnNull() {
    assertNull(DateUtils.toOffsetDateTimeStartOfTheDay(null));
  }

  @Test
  void givenLocalDateWhenToOffsetDateTimeEndOfTheDayThenOk() {
    OffsetDateTime expected = LocalDateTime.of(LocalDate.now(),
        LocalTime.MAX.truncatedTo(java.time.temporal.ChronoUnit.MILLIS))
      .atZone(Constants.ZONEID).toOffsetDateTime();

    OffsetDateTime result = DateUtils.toOffsetDateTimeEndOfTheDay(
      LocalDate.now());

    assertTrue(expected.isEqual(result));
  }

  @Test
  void givenNullLocalDateWhenToOffsetDateTimeEndOfTheDayThenReturnNull() {
    assertNull(DateUtils.toOffsetDateTimeEndOfTheDay(null));
  }
}
