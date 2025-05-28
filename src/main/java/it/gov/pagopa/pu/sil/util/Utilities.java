package it.gov.pagopa.pu.sil.util;

import org.slf4j.MDC;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class Utilities {

  private static final long OFFSET_2025_01_01_MILLIS = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    .toInstant().toEpochMilli();

  private Utilities() {
  }

  public static String getTraceId(){
    return MDC.get("traceId");
  }

  public static int systemTimeSecondsFrom2025() {
    return (int) ((System.currentTimeMillis() - OFFSET_2025_01_01_MILLIS) / 1000L);
  }

  public static String iuv2Nav(String iuv) {
    if (iuv == null || iuv.isBlank()) {
      return null;
    }
    return "3"+iuv;
  }

  public static String nav2Iuv(String nav) {
    if (nav == null || nav.isBlank()) {
      return null;
    } else if (nav.length() < 2 || !nav.startsWith("3")) {
      throw new IllegalArgumentException("Invalid NAV format: " + nav);
    }

    return nav.substring(1);
  }
}
