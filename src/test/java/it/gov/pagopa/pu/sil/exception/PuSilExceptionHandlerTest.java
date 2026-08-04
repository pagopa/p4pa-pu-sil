package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.sil.exception.common.CommonExceptionHandlerTest;
import it.gov.pagopa.pu.sil.exception.common.InvalidValueException;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.HttpClientErrorException;

import static org.mockito.Mockito.doThrow;

class PuSilExceptionHandlerTest extends CommonExceptionHandlerTest {

  @Test
  void handleHttpClientErrorException() throws Exception {
    doThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN, "Error")).when(testControllerSpy).testEndpoint(DATA, BODY);

    performRequest(DATA, MediaType.APPLICATION_JSON)
      .andExpect(MockMvcResultMatchers.status().isForbidden())
      .andExpect(MockMvcResultMatchers.jsonPath("$.category").value("GENERIC_ERROR"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("GENERIC_ERROR"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(HttpStatus.FORBIDDEN.value()+" Error"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.fields").doesNotExist())
      .andExpect(MockMvcResultMatchers.jsonPath("$.traceId").value(traceId));
  }

  @Test
  void handleIllegalArgumentException() throws Exception {
    doThrow(new IllegalArgumentException("Error")).when(testControllerSpy).testEndpoint(DATA, BODY);

    performRequest(DATA, MediaType.APPLICATION_JSON)
      .andExpect(MockMvcResultMatchers.status().isNotFound())
      .andExpect(MockMvcResultMatchers.jsonPath("$.category").value("BAD_REQUEST"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("BAD_REQUEST"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Error"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.traceId").value(traceId));
  }

  @Test
  void handleHttpHostConnectionExceptionException() throws Exception {
    doThrow(new RuntimeException("connection refused", new HttpHostConnectException("error"))).when(testControllerSpy).testEndpoint(DATA, BODY);

    performRequest(DATA, MediaType.APPLICATION_JSON)
      .andExpect(MockMvcResultMatchers.status().isInternalServerError())
      .andExpect(MockMvcResultMatchers.jsonPath("$.category").value("GENERIC_ERROR"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("PU_SIL_CONNECTION_ERROR"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("connection refused"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.fields").doesNotExist())
      .andExpect(MockMvcResultMatchers.jsonPath("$.traceId").value(traceId));
  }

  @Test
  void handlePaymentNotFoundException() throws Exception {
    doThrow(new PaymentNotFoundException("Error")).when(testControllerSpy).testEndpoint(DATA, BODY);

    performRequest(DATA, MediaType.APPLICATION_JSON)
      .andExpect(MockMvcResultMatchers.status().isNotFound())
      .andExpect(MockMvcResultMatchers.jsonPath("$.category").value("NOT_FOUND"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("PAYMENT_NOT_FOUND"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Error"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.fields").doesNotExist())
      .andExpect(MockMvcResultMatchers.jsonPath("$.traceId").value(traceId));
  }

  @Test
  void handlePaymentNotNotifiedException() throws Exception {
    doThrow(new PaymentNotNotifiedException("Error")).when(testControllerSpy).testEndpoint(DATA, BODY);

    performRequest(DATA, MediaType.APPLICATION_JSON)
      .andExpect(MockMvcResultMatchers.status().isPreconditionFailed())
      .andExpect(MockMvcResultMatchers.jsonPath("$.category").value("BAD_REQUEST"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("PAYMENT_NOT_NOTIFIED"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Error"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.fields").doesNotExist())
      .andExpect(MockMvcResultMatchers.jsonPath("$.traceId").value(traceId));
  }

  @Test
  void handlePaymentInvalidStatusException() throws Exception {
    doThrow(new PaymentInvalidStatusException("Error")).when(testControllerSpy).testEndpoint(DATA, BODY);

    performRequest(DATA, MediaType.APPLICATION_JSON)
      .andExpect(MockMvcResultMatchers.status().isConflict())
      .andExpect(MockMvcResultMatchers.jsonPath("$.category").value("BAD_REQUEST"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_PAYMENT_STATUS"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Error"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.fields").doesNotExist())
      .andExpect(MockMvcResultMatchers.jsonPath("$.traceId").value(traceId));
  }

  @Test
  void handleAuthorizationDeniedException() throws Exception {
    doThrow(new AuthorizationDeniedException("Error")).when(testControllerSpy).testEndpoint(DATA, BODY);

    performRequest(DATA, MediaType.APPLICATION_JSON)
      .andExpect(MockMvcResultMatchers.status().isForbidden())
      .andExpect(MockMvcResultMatchers.jsonPath("$.category").value("UNAUTHORIZED"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("UNAUTHORIZED"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Error"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.fields").doesNotExist())
      .andExpect(MockMvcResultMatchers.jsonPath("$.traceId").value(traceId));
  }

  @Test
  void handleAssessmentNotFoundException() throws Exception {
    doThrow(new AssessmentNotFoundException("Error")).when(testControllerSpy).testEndpoint(DATA, BODY);

    performRequest(DATA, MediaType.APPLICATION_JSON)
      .andExpect(MockMvcResultMatchers.status().isNotFound())
      .andExpect(MockMvcResultMatchers.jsonPath("$.category").value("NOT_FOUND"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("ASSESSMENT_NOT_FOUND"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Error"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.fields").doesNotExist())
      .andExpect(MockMvcResultMatchers.jsonPath("$.traceId").value(traceId));
  }

  @Test
  void handleExportFileClientException() throws Exception {
    doThrow(new ExportFileClientException(new InvalidValueException(ProcessExecutionsErrorDTO.CategoryEnum.PROCESS_EXECUTIONS_BAD_REQUEST.getValue(), "Error"))).when(testControllerSpy).testEndpoint(DATA, BODY);

    performRequest(DATA, MediaType.APPLICATION_JSON)
      .andExpect(MockMvcResultMatchers.status().isBadRequest())
      .andExpect(MockMvcResultMatchers.jsonPath("$.category").value("BAD_REQUEST"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("PROCESS_EXECUTIONS_BAD_REQUEST"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Error"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.fields").doesNotExist())
      .andExpect(MockMvcResultMatchers.jsonPath("$.traceId").value(traceId));
  }

  @Test
  void handleExportFileServiceException() throws Exception {
    doThrow(new ExportFileServiceException(ErrorCodeConstants.ERROR_CODE_INVALID_DEBT_POSITION_TYPE_ORG_CODE, "Error")).when(testControllerSpy).testEndpoint(DATA, BODY);

    performRequest(DATA, MediaType.APPLICATION_JSON)
      .andExpect(MockMvcResultMatchers.status().isBadRequest())
      .andExpect(MockMvcResultMatchers.jsonPath("$.category").value("BAD_REQUEST"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_DEBT_POSITION_TYPE_ORG_CODE"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Error"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.fields").doesNotExist())
      .andExpect(MockMvcResultMatchers.jsonPath("$.traceId").value(traceId));
  }

  @Test
  void handleBalanceParseException() throws Exception {
    doThrow(new BalanceParseException("Error", null)).when(testControllerSpy).testEndpoint(DATA, BODY);

    performRequest(DATA, MediaType.APPLICATION_JSON)
      .andExpect(MockMvcResultMatchers.status().isInternalServerError())
      .andExpect(MockMvcResultMatchers.jsonPath("$.category").value("GENERIC_ERROR"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_BALANCE"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Error"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.fields").doesNotExist())
      .andExpect(MockMvcResultMatchers.jsonPath("$.traceId").value(traceId));
  }
}
