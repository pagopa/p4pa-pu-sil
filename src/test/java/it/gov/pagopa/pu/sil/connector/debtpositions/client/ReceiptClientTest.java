package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.controller.generated.ReceiptApi;
import it.gov.pagopa.pu.debtpositions.dto.generated.ReceiptDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApisHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class ReceiptClientTest {

  @Mock
  private DebtPositionsApisHolder apisHolderMock;
  @Mock
  private ReceiptApi receiptApiMock;


  private ReceiptClient client;

  @BeforeEach
  void setUp() {
    client = new ReceiptClient(apisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      apisHolderMock,
      receiptApiMock
      );
  }

  @ParameterizedTest
  @ValueSource(longs = {1L})
  void whenGetReceiptByIdThenInvokeApi(Long receiptId) {
    // Given
    String accessToken = "ACCESSTOKEN";
    ReceiptDTO expectedResult;

    Mockito.when(apisHolderMock.getReceiptApi(accessToken))
      .thenReturn(receiptApiMock);

    expectedResult = new ReceiptDTO();
      Mockito.when(receiptApiMock.getReceipt(receiptId)).thenReturn(expectedResult);


    // When
    ReceiptDTO result = client.getReceiptById(receiptId, accessToken);

    // Then
    Assertions.assertEquals(expectedResult, result);
  }

}
