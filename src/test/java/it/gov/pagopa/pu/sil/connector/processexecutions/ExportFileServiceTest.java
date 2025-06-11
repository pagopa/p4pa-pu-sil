package it.gov.pagopa.pu.sil.connector.processexecutions;

import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.client.ExportFileClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportFileServiceTest {

  private final String accessToken = "ACCESSTOKEN";

  @Mock
  private ExportFileClient apiClientMock;

  private ExportFileService service;

  @BeforeEach
  void init() {
    service = new ExportFileServiceImpl(
      apiClientMock
    );
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      apiClientMock
    );
  }

  @Test
  void whenCreateClassificationsExportFileThenInvokeWithAccessToken() {
    // Given
    Long expectedId = 123L;
    ClassificationsExportFileRequestDTO dto = new ClassificationsExportFileRequestDTO();

    Mockito.when(apiClientMock.createClassificationsExportFile(Mockito.same(dto), Mockito.same(accessToken)))
      .thenReturn(expectedId);

    // When
    Long result = service.createClassificationsExportFile(dto, accessToken);
    // Then
    Assertions.assertSame(expectedId, result);
  }

  @Test
  void whenCreatePaidExportFileThenInvokeWithAccessToken() {
    // Given
    Long expectedId = 123L;
    PaidExportFileRequestDTO dto = new PaidExportFileRequestDTO();

    Mockito.when(apiClientMock.createPaidExportFile(Mockito.same(dto), Mockito.same(accessToken)))
      .thenReturn(expectedId);

    // When
    Long result = service.createPaidExportFile(dto, accessToken);
    // Then
    Assertions.assertSame(expectedId, result);
  }
}
