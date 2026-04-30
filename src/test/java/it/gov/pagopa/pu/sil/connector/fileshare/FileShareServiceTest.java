package it.gov.pagopa.pu.sil.connector.fileshare;

import it.gov.pagopa.pu.sil.connector.fileshare.client.FileShareClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

@ExtendWith(MockitoExtension.class)
class FileShareServiceTest {

  @Mock
  private FileShareClient clientMock;

  private FileShareService service;

  @BeforeEach
  void init(){
    service = new FileShareServiceImpl(clientMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(clientMock);
  }

  @Test
  void whenDownloadReceiptThenInvokeClient(){
    // Given
    String accessToken = "ACCESSTOKEN";
    Resource expectedResult = new ByteArrayResource("result".getBytes(StandardCharsets.UTF_8));

    Mockito.when(clientMock.downloadReceipt(1L, 10L, accessToken))
      .thenReturn(expectedResult);

    // When
    Resource result = service.downloadReceipt(1L, 10L, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

}
