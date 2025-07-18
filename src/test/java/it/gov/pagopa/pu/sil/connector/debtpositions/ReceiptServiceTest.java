package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.ReceiptDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.client.ReceiptClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

  @Mock
  private ReceiptClient clientMock;

  private ReceiptService service;

  @BeforeEach
  void init() {
    service = new ReceiptServiceImpl(clientMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(clientMock);
  }

  @Test
  void whenGetReceiptByIdThenReturnReceiptDTO() {
    // Given
    Long receiptId = 1L;
    String accessToken = "ACCESSTOKEN";
    ReceiptDTO expectedResult = new ReceiptDTO();

    Mockito.when(clientMock.getReceiptById(receiptId, accessToken)).thenReturn(expectedResult);

    // When
    ReceiptDTO result = service.getReceiptById(receiptId, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

}
