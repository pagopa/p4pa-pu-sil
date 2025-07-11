package it.gov.pagopa.pu.sil.connector.pagopapayments.client;


import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.pagopapayments.client.generated.PrintPaymentNoticeApi;
import it.gov.pagopa.pu.sil.connector.pagopapayments.config.PagoPaPaymentsApisHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

@ExtendWith(MockitoExtension.class)
class PrintPaymentsNoticeClientTest {

  @Mock
  private PagoPaPaymentsApisHolder pagoPaPaymentsApisHolderMock;
  @Mock
  private PrintPaymentNoticeApi printPaymentNoticeApiMock;

  @InjectMocks
  private PagopaPaymentsClient pagopaPaymentsClient;


  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      pagoPaPaymentsApisHolderMock
    );
  }

  @Test
  void whenGenerateNoticeThenInvokeWithAccessToken() {
    // Given
    String accessToken = "ACCESSTOKEN";
    String iuv = "IUV";
    DebtPositionDTO debtPositionDTO = new DebtPositionDTO();
    Resource resource = new ByteArrayResource("notice content".getBytes());

    Mockito.when(pagoPaPaymentsApisHolderMock.getPrintPaymentNoticeApi(accessToken))
      .thenReturn(printPaymentNoticeApiMock);
    Mockito.when(printPaymentNoticeApiMock.generateNotice(iuv, debtPositionDTO)).thenReturn(resource);

    // When
    Resource returnedResource = pagopaPaymentsClient.generateNotice(iuv, debtPositionDTO, accessToken);

    // Then
    Assertions.assertEquals(resource, returnedResource);
  }
}
