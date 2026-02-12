package it.gov.pagopa.pu.sil.connector.processexecutions.client;

import it.gov.pagopa.pu.processexecutions.controller.generated.ExportFileControllerApi;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.config.ProcessExecutionsApisHolder;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    Mockito.when(processExecutionsApisHolderMock.getExportFileControllerApi(accessToken))
      .thenReturn(exportFileControllerApiMock);
    Mockito.when(exportFileControllerApiMock.createClassificationsExportFileWithHttpInfo(dto))
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

    Mockito.when(processExecutionsApisHolderMock.getExportFileControllerApi(accessToken))
      .thenReturn(exportFileControllerApiMock);
    Mockito.when(exportFileControllerApiMock.createPaidExportFileWithHttpInfo(dto))
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

    Mockito.when(processExecutionsApisHolderMock.getExportFileControllerApi(accessToken))
      .thenReturn(exportFileControllerApiMock);

    ProcessExecutionsErrorDTO processExecutionsErrorDTO = new ProcessExecutionsErrorDTO(
      ProcessExecutionsErrorDTO.CategoryEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE, "Invalid time range", "traceId");
    HttpClientErrorException.BadRequest badRequest = (HttpClientErrorException.BadRequest) HttpClientErrorException.create(
      HttpStatus.BAD_REQUEST, "Bad request", null, null, null);
    badRequest.setBodyConvertFunction(t -> processExecutionsErrorDTO);

    Mockito.when(exportFileControllerApiMock.createPaidExportFileWithHttpInfo(dto))
      .thenThrow(badRequest);

    // When
    ExportFileClientException exception = assertThrows(ExportFileClientException.class, () ->
      client.createPaidExportFile(dto, accessToken)
    );

    // Then
    assertEquals(ProcessExecutionsErrorDTO.CategoryEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE, exception.getCode());
  }


}
