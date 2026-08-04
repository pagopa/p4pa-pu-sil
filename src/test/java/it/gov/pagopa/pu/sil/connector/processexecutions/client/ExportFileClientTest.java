package it.gov.pagopa.pu.sil.connector.processexecutions.client;

import it.gov.pagopa.pu.processexecutions.controller.generated.ExportFileControllerApi;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.config.ProcessExecutionsApisHolder;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import it.gov.pagopa.pu.sil.exception.common.InvalidValueException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportFileClientTest {

  private final String accessToken = "ACCESSTOKEN";

  @Mock
  private ProcessExecutionsApisHolder processExecutionsApisHolderMock;
  @Mock
  private ExportFileControllerApi exportFileControllerApiMock;

  private ExportFileClient client;

  @BeforeEach
  void init() {
    client = new ExportFileClient(processExecutionsApisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      processExecutionsApisHolderMock,
      exportFileControllerApiMock
    );
  }

  @Test
  void whenCreateClassificationsExportFileThenInvokeWithAccessToken() {
    // Given
    Long expectedId = 123L;
    ClassificationsExportFileRequestDTO dto = new ClassificationsExportFileRequestDTO();

    when(processExecutionsApisHolderMock.getExportFileControllerApi(accessToken))
      .thenReturn(exportFileControllerApiMock);
    when(exportFileControllerApiMock.createClassificationsExportFileWithHttpInfo(dto))
      .thenReturn(ResponseEntity.created(URI.create(expectedId.toString())).build());

    // When
    Long result = client.createClassificationsExportFile(dto, accessToken);
    // Then
    Assertions.assertSame(expectedId, result);
  }

  @Test
  void whenCreatePaidExportFileThenInvokeWithAccessToken() {
    // Given
    Long expectedId = 123L;
    PaidExportFileRequestDTO dto = new PaidExportFileRequestDTO();

    when(processExecutionsApisHolderMock.getExportFileControllerApi(accessToken))
      .thenReturn(exportFileControllerApiMock);
    when(exportFileControllerApiMock.createPaidExportFileWithHttpInfo(dto))
      .thenReturn(ResponseEntity.created(URI.create(expectedId.toString())).build());

    // When
    Long result = client.createPaidExportFile(dto, accessToken);
    // Then
    Assertions.assertSame(expectedId, result);
  }

  @Test
  void whenCreatePaidExportFileWithHttpInfoExceptionThenThrowClientException() {
    // Given
    PaidExportFileRequestDTO dto = new PaidExportFileRequestDTO();

    when(processExecutionsApisHolderMock.getExportFileControllerApi(accessToken))
      .thenReturn(exportFileControllerApiMock);

    InvalidValueException badRequest = new InvalidValueException(ProcessExecutionsErrorDTO.CategoryEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE.getValue(), "Invalid time range");

    when(exportFileControllerApiMock.createPaidExportFileWithHttpInfo(dto))
      .thenThrow(badRequest);

    // When
    ExportFileClientException exception = assertThrows(ExportFileClientException.class, () ->
      client.createPaidExportFile(dto, accessToken)
    );

    // Then
    assertEquals(ProcessExecutionsErrorDTO.CategoryEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE.getValue(), exception.getCode());
  }


}
