package it.gov.pagopa.pu.sil.exception.soap.transcoder;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.exception.soap.SoapFaultTranscoded;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PuForOrganizationPaymentExceptionTranscoderTest extends BaseSoapExceptionTranscoderTest {

  PuForOrganizationPaymentExceptionTranscoderTest() {
    super(SilFaults.PAA_ENTE_NON_VALIDO, SilFaults.PAA_SYSTEM_ERROR, PuForOrganizationPaymentExceptionTranscoder.errorCode2SilFault);
  }

  private final PuForOrganizationPaymentExceptionTranscoder exceptionTranscoder = new PuForOrganizationPaymentExceptionTranscoder();

  @Override
  protected BaseSoapExceptionTranscoder getExceptionTranscoder() {
    return exceptionTranscoder;
  }

  @Test
  void givenNotHandledExportFileServiceExceptionCodeWhenTranscodeExceptionThenHandleIt() {
    // Given
    ExportFileServiceException exception = new ExportFileServiceException("Test message", "TEST_CODE");

    // When
    SoapFaultTranscoded result = getExceptionTranscoder().transcodeException(exception);

    // Then
    Assertions.assertEquals(
      new SoapFaultTranscoded(
        SilFaults.PAA_SYSTEM_ERROR,
        "Errore di sistema"
      ),
      result
    );
  }

  @Test
  void givenExportFileServiceExceptionInvalidTypeCodeWhenTranscodeExceptionThenHandleIt() {
    // Given
    ExportFileServiceException exception = new ExportFileServiceException(
      ErrorCodeConstants.ERROR_CODE_INVALID_DEBT_POSITION_TYPE_ORG_CODE,
      "ERROR MESSAGE");

    // When
    SoapFaultTranscoded result = getExceptionTranscoder().transcodeException(exception);

    // Then
    Assertions.assertEquals(
      new SoapFaultTranscoded(
        SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO,
        SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO.description() + ": ERROR MESSAGE"
      ),
      result
    );
  }

  @Test
  void givenExportFileServiceExceptionInvalidStatusCodeWhenTranscodeExceptionThenHandleIt() {
    // Given
    ExportFileServiceException exception = new ExportFileServiceException(
      ErrorCodeConstants.ERROR_CODE_INVALID_DEBT_POSITION_TYPE_ORG_STATUS,
      "ERROR MESSAGE");

    // When
    SoapFaultTranscoded result = getExceptionTranscoder().transcodeException(exception);

    // Then
    Assertions.assertEquals(
      new SoapFaultTranscoded(
        SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO,
        SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO.description() + ": ERROR MESSAGE"
      ),
      result
    );
  }
}
