package it.gov.pagopa.pu.sil.connector.fileshare.client;

import it.gov.pagopa.pu.fileshare.client.generated.ReceiptApi;
import it.gov.pagopa.pu.sil.connector.fileshare.config.FileShareApisHolder;
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

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileShareClientTest {

  @Mock
  private FileShareApisHolder apisHolderMock;
  @Mock
  private ReceiptApi receiptApiMock;

  private FileShareClient client;

  @BeforeEach
  void setUp() {
    client = new FileShareClient(apisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      apisHolderMock,
      receiptApiMock
    );
  }

  @Test
  void whenDownloadReceiptThenInvokeApi() {
    //Given
    String accessToken = "ACCESSTOKEN";
    Resource expectedResult = new ByteArrayResource("result".getBytes(StandardCharsets.UTF_8));

    when(apisHolderMock.getReceiptApi(accessToken))
      .thenReturn(receiptApiMock);
    when(receiptApiMock.downloadRt(1L, 10L))
      .thenReturn(expectedResult);

    // When
    Resource result = client.downloadReceipt(1L, 10L, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }


}
