package it.gov.pagopa.pu.sil.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static it.gov.pagopa.pu.sil.util.Constants.ZONEID;

public class DateUtils {
  public static OffsetDateTime toOffsetDateTimeStartOfTheDay(LocalDate localDate) {
    if (localDate == null) {
      return null;
    }
    return localDate.atStartOfDay(ZONEID).toOffsetDateTime();
  }

  public static OffsetDateTime toOffsetDateTimeEndOfTheDay(LocalDate localDate) {
    if (localDate == null) {
      return null;
    }
    LocalDateTime endOfDay = LocalDateTime.of(localDate, LocalTime.MAX.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    return endOfDay.atZone(ZONEID).toOffsetDateTime();
  }
}
