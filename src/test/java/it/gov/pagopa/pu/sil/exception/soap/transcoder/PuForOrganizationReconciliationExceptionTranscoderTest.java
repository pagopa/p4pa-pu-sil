package it.gov.pagopa.pu.sil.exception.soap.transcoder;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.exception.IngestionFlowFileTypeValidationException;
import it.gov.pagopa.pu.sil.exception.soap.SoapFaultTranscoded;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PuForOrganizationReconciliationExceptionTranscoderTest extends BaseSoapExceptionTranscoderTest {

  PuForOrganizationReconciliationExceptionTranscoderTest() {
    super(SilFaults.PIVOT_ENTE_NON_VALIDO, SilFaults.PIVOT_SYSTEM_ERROR, PuForOrganizationReconciliationExceptionTranscoder.errorCode2SilFault);
  }

  @Override
  protected BaseSoapExceptionTranscoder getExceptionTranscoder() {
    return new PuForOrganizationReconciliationExceptionTranscoder();
  }

  @Test
  void givenIngestionFlowFileTypeValidationExceptionWhenTranscodeExceptionThenHandleIt() {
    // Given
    String rejectedValue = "TEST_REJECTED_VALUE";
    IngestionFlowFileTypeValidationException exception = new IngestionFlowFileTypeValidationException("ERROR MESSAGE", rejectedValue);

    // When
    SoapFaultTranscoded result = getExceptionTranscoder().transcodeException(exception);

    // Then
    Assertions.assertEquals(
      new SoapFaultTranscoded(
        SilFaults.PIVOT_TIPO_FLUSSO_NON_VALIDO,
        "Tipo di flusso non valido: %s".formatted(rejectedValue)
      ),
      result
    );
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
        SilFaults.PIVOT_SYSTEM_ERROR,
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
        SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO,
        SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO.description() + ": ERROR MESSAGE"
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
        SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO,
        SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO.description() + ": ERROR MESSAGE"
      ),
      result
    );
  }
}
