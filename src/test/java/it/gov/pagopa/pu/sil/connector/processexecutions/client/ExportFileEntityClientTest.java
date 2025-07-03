package it.gov.pagopa.pu.sil.connector.processexecutions.client;

import it.gov.pagopa.pu.processexecutions.controller.generated.ExportFileEntityControllerApi;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile;
import it.gov.pagopa.pu.sil.connector.processexecutions.config.ProcessExecutionsApisHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class ExportFileEntityClientTest {
  private final String accessToken = "ACCESSTOKEN";

  @Mock
  private ProcessExecutionsApisHolder processExecutionsApisHolderMock;
  @Mock
  private ExportFileEntityControllerApi exportFileEntityControllerApiMock;

  private ExportFileEntityClient client;

  @BeforeEach
  void setUp() {
    client = new ExportFileEntityClient(processExecutionsApisHolderMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(
      processExecutionsApisHolderMock,
      exportFileEntityControllerApiMock
    );
  }


  @Test
  void whenGetExportFileThenReturnExportFile() {
    // Given
    Long exportFileId = 123L;
    ExportFile expectedExportFile = new ExportFile();

    Mockito.when(processExecutionsApisHolderMock.getExportFileEntityControllerApi(accessToken))
      .thenReturn(exportFileEntityControllerApiMock);

    Mockito.when(exportFileEntityControllerApiMock.crudGetExportfile(exportFileId.toString()))
      .thenReturn(expectedExportFile);

    // When
    ExportFile result = client.getExportFile(exportFileId, accessToken);

    // Then
    Assertions.assertSame(expectedExportFile, result);
  }

  @Test
  void givenHttpClientErrorExceptionOtherStatusWhenGetExportFileThenThrowIt() {
    // Given
    Long exportFileId = 123L;

    Mockito.when(processExecutionsApisHolderMock.getExportFileEntityControllerApi(accessToken))
      .thenReturn(exportFileEntityControllerApiMock);

    Mockito.when(exportFileEntityControllerApiMock.crudGetExportfile(exportFileId.toString()))
      .thenThrow(HttpClientErrorException
        .create(HttpStatus.NOT_FOUND, "NotFound", null, null, null));

    // When
    ExportFile result = client.getExportFile(exportFileId, accessToken);

    // Then
    Assertions.assertNull(result);
  }
}
