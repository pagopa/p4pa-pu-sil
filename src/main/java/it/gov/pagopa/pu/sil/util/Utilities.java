package it.gov.pagopa.pu.sil.util;

import org.slf4j.MDC;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Utilities {

  public static final String IUV_SEPARATOR = ",";
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
    } else if (iuv.contains(IUV_SEPARATOR)) {
      return Arrays.stream(iuv.split(IUV_SEPARATOR))
        .map(Utilities::iuv2Nav)
        .collect(Collectors.joining(IUV_SEPARATOR));
    } else {
      return Constants.AUX_DIGIT + iuv;
    }
  }

  public static String nav2Iuv(String nav) {
    if (nav == null || nav.isBlank()) {
      return null;
    } else if (nav.contains(IUV_SEPARATOR)) {
      return Arrays.stream(nav.split(IUV_SEPARATOR))
        .map(Utilities::nav2Iuv)
        .collect(Collectors.joining(IUV_SEPARATOR));
    } else if (nav.length() < 2 || !nav.startsWith(Constants.AUX_DIGIT)) {
      throw new IllegalArgumentException("Invalid NAV format: " + nav);
    } else {
      return nav.substring(1);
    }
  }

  public static LocalDate getSpontaneousSilExpirationDate() {
    LocalDateTime now = LocalDateTime.now(Constants.ZONEID);
    boolean isSameDay = now.plusMinutes(Constants.EXPIRATION_SPONTANEOUS_SIL_DEBT_POSITION_MINUTES)
      .toLocalDate()
      .isEqual(now.toLocalDate());
    return now.toLocalDate().plusDays(isSameDay ? 0 : 1);
  }

  /**
   * Checks if the fiscal code is a valid natural person fiscal code.
   * A valid natural person fiscal code is either 16 characters long or equals the anonymous fiscal code.
   * A natural person must have a {@link it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType#F}.
   *
   * @param fiscalCode the fiscal code to check
   * @return true if the fiscal code is a valid natural person fiscal code, false otherwise
   */
  public static boolean isNaturalPerson(String fiscalCode) {
    return fiscalCode != null && (fiscalCode.length() == 16 || fiscalCode.equals(Constants.ANONYMOUS_FISCAL_CODE));
  }


}
