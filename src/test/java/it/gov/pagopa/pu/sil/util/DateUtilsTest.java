package it.gov.pagopa.pu.sil.util;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateUtilsTest {
  @Test
  void givenOffsetDateTimeWhenToOffsetDateTimeStartOfTheDayThenOk() {
    OffsetDateTime input = OffsetDateTime.of(2025, 10, 15, 14, 30, 0, 0, ZoneOffset.ofHours(2));

    OffsetDateTime expected = OffsetDateTime.of(2025, 10, 15, 0, 0, 0, 0, ZoneOffset.ofHours(2));

    OffsetDateTime result = DateUtils.toOffsetDateTimeStartOfTheDay(input);

    assertTrue(expected.isEqual(result));
  }

  @Test
  void givenNullWhenToOffsetDateTimeStartOfTheDayThenReturnNull() {
    assertNull(DateUtils.toOffsetDateTimeStartOfTheDay(null));
  }

  @Test
  void givenOffsetDateTimeWhenToOffsetDateTimeEndOfTheDayThenOk() {
    OffsetDateTime input = OffsetDateTime.of(2025, 10, 15, 14, 30, 0, 0, ZoneOffset.ofHours(2));

    OffsetDateTime startOfNextDay = OffsetDateTime.of(2025, 10, 16, 0, 0, 0, 0, ZoneOffset.ofHours(2));
    OffsetDateTime expected = startOfNextDay.minus(1, ChronoUnit.MILLIS);

    OffsetDateTime result = DateUtils.toOffsetDateTimeEndOfTheDay(input);

    assertTrue(expected.isEqual(result));
  }

  @Test
  void givenNullWhenToOffsetDateTimeEndOfTheDayThenReturnNull() {
    assertNull(DateUtils.toOffsetDateTimeEndOfTheDay(null));
  }
}
