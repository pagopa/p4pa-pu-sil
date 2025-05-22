package it.gov.pagopa.pu.sil.util;

import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class Utilities {

  private Utilities() {
  }

  public static OffsetDateTime localDatetimeToOffsetDateTime(LocalDateTime localDateTime) {
    return localDateTime != null
      ? localDateTime.atOffset(ZoneId.systemDefault().getRules().getOffset(localDateTime))
      : null;
  }

  public static boolean isValidIntervalBetweenOffsetDateTime(OffsetDateTime dateFrom, OffsetDateTime dateTo, ChronoUnit chronoUnit, long maxInterval){
    return chronoUnit.between(dateFrom, dateTo) <= maxInterval;
  }

  public static String getTraceId(){
    return MDC.get("traceId");
  }
}
