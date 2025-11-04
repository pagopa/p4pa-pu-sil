package it.gov.pagopa.pu.sil.util;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateUtilsTest {
  @Test
  void givenLocalDateWhenToOffsetDateTimeStartOfTheDayThenOk() {
    ZonedDateTime input = ZonedDateTime.of(2025, 10, 15, 14, 30, 0, 0, Constants.ZONEID);

    OffsetDateTime expected = ZonedDateTime.of(2025, 10, 15, 0, 0, 0, 0, Constants.ZONEID).toOffsetDateTime();

    OffsetDateTime result = DateUtils.toOffsetDateTimeStartOfTheDay(input);

    assertTrue(expected.isEqual(result));
  }

  @Test
  void givenNullLocalDateWhenToOffsetDateTimeStartOfTheDayThenReturnNull() {
    assertNull(DateUtils.toOffsetDateTimeStartOfTheDay(null));
  }

  @Test
  void givenLocalDateWhenToOffsetDateTimeEndOfTheDayThenOk() {
    ZonedDateTime input = ZonedDateTime.of(2025, 10, 15, 14, 30, 0, 0, Constants.ZONEID);

    OffsetDateTime expected = ZonedDateTime.of(2025, 10, 16, 0, 0, 0, 0, Constants.ZONEID)
      .minus(1, ChronoUnit.MILLIS)
      .toOffsetDateTime();

    OffsetDateTime result = DateUtils.toOffsetDateTimeEndOfTheDay(input);

    assertTrue(expected.isEqual(result));
  }

  @Test
  void givenNullLocalDateWhenToOffsetDateTimeEndOfTheDayThenReturnNull() {
    assertNull(DateUtils.toOffsetDateTimeEndOfTheDay(null));
  }
}
