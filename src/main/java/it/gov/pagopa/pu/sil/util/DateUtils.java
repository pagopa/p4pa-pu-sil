package it.gov.pagopa.pu.sil.util;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class DateUtils {
  private DateUtils() {
  }

  public static OffsetDateTime toOffsetDateTimeStartOfTheDay(ZonedDateTime zonedDateTime) {
    if (zonedDateTime == null) {
      return null;
    }
    return zonedDateTime.truncatedTo(java.time.temporal.ChronoUnit.DAYS).toOffsetDateTime();
  }

  public static OffsetDateTime toOffsetDateTimeEndOfTheDay(ZonedDateTime zonedDateTime) {
    if (zonedDateTime == null) {
      return null;
    }
    ZonedDateTime startOfNextDay = zonedDateTime.truncatedTo(ChronoUnit.DAYS).plusDays(1);
    return startOfNextDay.minus(1, java.time.temporal.ChronoUnit.MILLIS)
      .toOffsetDateTime();
  }
}
