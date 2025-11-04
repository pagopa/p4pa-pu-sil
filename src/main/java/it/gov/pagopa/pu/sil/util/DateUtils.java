package it.gov.pagopa.pu.sil.util;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class DateUtils {
  private DateUtils() {
  }

  public static OffsetDateTime toOffsetDateTimeStartOfTheDay(OffsetDateTime offsetDateTime) {
    if (offsetDateTime == null) {
      return null;
    }
    return offsetDateTime.truncatedTo(ChronoUnit.DAYS);
  }

  public static OffsetDateTime toOffsetDateTimeEndOfTheDay(OffsetDateTime offsetDateTime) {
    if (offsetDateTime == null) {
      return null;
    }
    OffsetDateTime startOfNextDay = offsetDateTime.truncatedTo(ChronoUnit.DAYS).plusDays(1);
    return startOfNextDay.minus(1, ChronoUnit.MILLIS);
  }
}
