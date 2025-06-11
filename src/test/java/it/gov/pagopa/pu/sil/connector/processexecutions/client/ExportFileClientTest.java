package it.gov.pagopa.pu.sil.connector.processexecutions.client;

import it.gov.pagopa.pu.processexecutions.controller.generated.ExportFileControllerApi;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.config.ProcessExecutionsApisHolder;
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
    String expectedId = "https://example.com/exportFileId";
    ClassificationsExportFileRequestDTO dto = new ClassificationsExportFileRequestDTO();

    Mockito.when(processExecutionsApisHolderMock.getExportFileControllerApi(accessToken))
      .thenReturn(exportFileControllerApiMock);
    Mockito.when(exportFileControllerApiMock.createClassificationsExportFileWithHttpInfo(dto))
      .thenReturn(ResponseEntity.created(URI.create(expectedId)).build());

    // When
    String result = client.createClassificationsExportFile(dto, accessToken);
    // Then
    Assertions.assertSame(expectedId, result);
  }

  @Test
  void whenCreatePaidExportFileThenInvokeWithAccessToken() {
    // Given
    String expectedId = "https://example.com/exportFileId";
    PaidExportFileRequestDTO dto = new PaidExportFileRequestDTO();

    Mockito.when(processExecutionsApisHolderMock.getExportFileControllerApi(accessToken))
      .thenReturn(exportFileControllerApiMock);
    Mockito.when(exportFileControllerApiMock.createPaidExportFileWithHttpInfo(dto))
      .thenReturn(ResponseEntity.created(URI.create(expectedId)).build());

    // When
    String result = client.createPaidExportFile(dto, accessToken);
    // Then
    Assertions.assertSame(expectedId, result);
  }
}
