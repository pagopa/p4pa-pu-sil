package it.gov.pagopa.pu.sil.connector.processexecutions.client;

import it.gov.pagopa.pu.processexecutions.client.generated.IngestionFlowFileControllerApi;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.config.ProcessExecutionsApisHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionFlowFileClientTest {

  @Mock
  private ProcessExecutionsApisHolder processExecutionsApisHolderMock;
  @Mock
  private IngestionFlowFileControllerApi ingestionFlowFileControllerApiMock;

  private IngestionFlowFileClient client;

  @BeforeEach
  void init(){
    client = new IngestionFlowFileClient(processExecutionsApisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      processExecutionsApisHolderMock,
      ingestionFlowFileControllerApiMock
    );
  }
  @Test
  void whenCreateIngestionFlowFileReservationThenInvokeWithAccessToken() {
    // Given
    Long expectedId = 77L;
    IngestionFlowFileRequestDTO dto = new IngestionFlowFileRequestDTO();

    String accessToken = "ACCESSTOKEN";
    when(processExecutionsApisHolderMock.getIngestionFlowFileControllerApi(accessToken))
      .thenReturn(ingestionFlowFileControllerApiMock);
    when(ingestionFlowFileControllerApiMock.createIngestionFlowFileReservation(dto))
      .thenReturn(expectedId);

    // When
    Long result = client.createIngestionFlowFileReservation(dto, accessToken);
    // Then
    Assertions.assertSame(expectedId, result);
  }
}
