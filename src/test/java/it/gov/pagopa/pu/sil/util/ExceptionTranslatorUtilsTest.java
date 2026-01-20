package it.gov.pagopa.pu.sil.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExceptionTranslatorUtilsTest {
  @Test
  void givenMessageWithKnownCodeWhenTranslateExceptionMessageThenTranslate() {
    String inputMessage = "[USER_UNAUTHORIZED] Access denied";
    String expectedMessage = "Utente non autorizzato";

    String result = ExceptionTranslatorUtils.translateExceptionMessage(inputMessage);

    assertEquals(expectedMessage, result);
  }

  @Test
  void givenMessageWithUnknownCodeWhenTranslateExceptionMessageThenReturnNotTranslatedMessage() {
    String inputMessage = "[UNKNOWN_CODE] Generic message";

    String result = ExceptionTranslatorUtils.translateExceptionMessage(inputMessage);

    assertEquals(inputMessage, result);
  }

  @Test
  void givenMessageWithoutCodeWhenTranslateExceptionMessageThenReturnNotTranslatedMessage() {
    String inputMessage = "Generic message";

    String result = ExceptionTranslatorUtils.translateExceptionMessage(inputMessage);

    assertEquals(inputMessage, result);
  }
}
