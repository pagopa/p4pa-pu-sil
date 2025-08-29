package it.gov.pagopa.pu.sil.connector.pagopapayments;


import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.sil.connector.pagopapayments.client.PagopaPaymentsClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagopaPaymentsServiceTest {
  @Mock
  private PagopaPaymentsClient pagopaPaymentsClientMock;

  private PagopaPaymentsService service;

  @BeforeEach
  void init() {
    service = new PagopaPaymentsServiceImpl(pagopaPaymentsClientMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(pagopaPaymentsClientMock);
  }

  @Test
  void whenGenerateNoticeThenInvokeClient() {
    // Given
    String accessToken = "accessToken";
    String iuv = "IUV";
    DebtPositionDTO debtPositionDTO = new DebtPositionDTO();
    Resource resource = new ByteArrayResource("notice content".getBytes());

    when(pagopaPaymentsClientMock.generateNotice(iuv, debtPositionDTO, accessToken))
      .thenReturn(resource);

    // When
    Resource result = service.generateNotice(iuv, debtPositionDTO, accessToken);

    // Then
    assertEquals(resource, result);
  }
}
