package it.gov.pagopa.pu.sil.util;

import it.gov.pagopa.pu.sil.enums.ErrorCode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExceptionTranslatorUtils {
  private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\\[([A-Z_]+)\\]");

  public static String translateExceptionMessage(String exceptionMessage) {
    Matcher matcher = ERROR_CODE_PATTERN.matcher(exceptionMessage);

    if (matcher.find()) {
      String code = matcher.group(1);
      return ErrorCode.fromCode(code).map(ErrorCode::getMessage).orElse(exceptionMessage);
    } else {
      return exceptionMessage;
    }
  }
}
