package it.gov.pagopa.pu.sil.connector.processexecutions;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.client.IngestionFlowFileClient;
import it.gov.pagopa.pu.sil.connector.processexecutions.client.IngestionFlowFileEntityClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IngestionFlowFileServiceTest {

  private final String accessToken = "ACCESSTOKEN";

  @Mock
  private IngestionFlowFileClient apiClientMock;
  @Mock
  private IngestionFlowFileEntityClient entityClientMock;

  private IngestionFlowFileService service;

  @BeforeEach
  void init(){
    service = new IngestionFlowFileServiceImpl(
      apiClientMock,
      entityClientMock
    );
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      apiClientMock,
      entityClientMock
    );
  }

  @Test
  void whenCreateIngestionFlowFileReservationThenInvokeWithAccessToken() {
    // Given
    Long expectedId = 77L;
    IngestionFlowFileRequestDTO dto = new IngestionFlowFileRequestDTO();

    Mockito.when(apiClientMock.createIngestionFlowFileReservation(Mockito.same(dto), Mockito.same(accessToken)))
      .thenReturn(expectedId);

    // When
    Long result = service.createIngestionFlowFileReservation(dto, accessToken);
    // Then
    Assertions.assertSame(expectedId, result);
  }

  @Test
  void whenGetIngestionFlowFileThenInvokeClient(){
    // Given
    Long organizationId = 1L;
    IngestionFlowFile expectedResult = new IngestionFlowFile();

    Mockito.when(entityClientMock.getIngestionFlowFile(Mockito.same(organizationId), Mockito.same(accessToken)))
      .thenReturn(expectedResult);

    // When
    IngestionFlowFile result = service.getIngestionFlowFile(organizationId, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }
}
