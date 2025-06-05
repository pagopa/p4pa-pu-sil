package it.gov.pagopa.pu.sil.connector.processexecutions.client;

import it.gov.pagopa.pu.processexecutions.controller.generated.IngestionFlowFileEntityControllerApi;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
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
class IngestionFlowFileEntityClientTest {
  private final String accessToken = "ACCESSTOKEN";

  @Mock
  private ProcessExecutionsApisHolder processExecutionsApisHolderMock;
  @Mock
  private IngestionFlowFileEntityControllerApi ingestionFlowFileEntityControllerApiMock;

  private IngestionFlowFileEntityClient client;

  @BeforeEach
  void init(){
    client = new IngestionFlowFileEntityClient(processExecutionsApisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      processExecutionsApisHolderMock,
      ingestionFlowFileEntityControllerApiMock
    );
  }

  @Test
  void whenGetIngestionFlowFileThenReturnIngestionFlowFile() {
    Long ingestionFlowFileId = 123L;
    IngestionFlowFile expectedIngestionFlowFile = new IngestionFlowFile();

    Mockito.when(processExecutionsApisHolderMock.getIngestionFlowFileEntityControllerApi(accessToken))
      .thenReturn(ingestionFlowFileEntityControllerApiMock);

    Mockito.when(ingestionFlowFileEntityControllerApiMock.crudGetIngestionflowfile(ingestionFlowFileId+""))
      .thenReturn(expectedIngestionFlowFile);

    IngestionFlowFile result = client.getIngestionFlowFile(ingestionFlowFileId, accessToken);

    Assertions.assertSame(expectedIngestionFlowFile, result);
  }


  @Test
  void givenHttpClientErrorExceptionOtherStatusWhenGetIngestionFlowFileThenThrowIt() {
    Long ingestionFlowFileId = 123L;

    Mockito.when(processExecutionsApisHolderMock.getIngestionFlowFileEntityControllerApi(accessToken))
      .thenReturn(ingestionFlowFileEntityControllerApiMock);

    Mockito.when(ingestionFlowFileEntityControllerApiMock.crudGetIngestionflowfile(ingestionFlowFileId+""))
      .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "NotFound", null, null, null));

    IngestionFlowFile result = client.getIngestionFlowFile(ingestionFlowFileId, accessToken);

    Assertions.assertNull(result);
  }
}
