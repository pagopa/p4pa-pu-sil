package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.sil.connector.actualization.client.LegacyActualizationClient;
import it.gov.pagopa.pu.sil.dto.generated.AmountUpdatesDTO;
import it.gov.pagopa.pu.sil.mapper.AmountUpdatesMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
class LegacyActualizationServiceTest {
  @Mock
  private LegacyActualizationClient legacyActualizationClientMock;
  @Mock
  private AmountUpdatesMapper amountUpdatesMapperMock;

  private LegacyActualizationService legacyActualizationService;

  @BeforeEach
  void setUp() {
    legacyActualizationService = new LegacyActualizationServiceImpl(legacyActualizationClientMock, amountUpdatesMapperMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(legacyActualizationClientMock, amountUpdatesMapperMock);
  }

  @Test
  void whenActualizationThenReturnPagamentoAggiornato() {
    // Given
    String accessToken = "accessToken";
    String serviceUrl = "http://example.com/service";
    Pagamento pagamento = new Pagamento();
    PagamentoAggiornato expectedPagamentoAggiornato = new PagamentoAggiornato();
    AmountUpdatesDTO expectedAmountUpdatesDTO = new AmountUpdatesDTO();

    Mockito.when(legacyActualizationClientMock.actualization(accessToken, serviceUrl, pagamento))
           .thenReturn(expectedPagamentoAggiornato);
    Mockito.when(amountUpdatesMapperMock.pagamentoAggiornato2AmountUpdatesDTO(expectedPagamentoAggiornato))
            .thenReturn(expectedAmountUpdatesDTO);

    // When
    AmountUpdatesDTO result = legacyActualizationService.actualization(accessToken, serviceUrl, pagamento);
    // Then
    assertSame(expectedAmountUpdatesDTO, result);
  }

  @Test
  void whenActualizationThrowsExceptionThenReturnKoAmountUpdatesDTO() {
    // Given
    String accessToken = "accessToken";
    String serviceUrl = "http://example.com/service";
    Pagamento pagamento = new Pagamento();
    AmountUpdatesDTO koAmountUpdatesDTO = new AmountUpdatesDTO();

    Mockito.when(legacyActualizationClientMock.actualization(accessToken, serviceUrl, pagamento))
           .thenThrow(Exception.class);
    Mockito.when(amountUpdatesMapperMock.mapToKoAmountUpdatesDTO())
            .thenReturn(koAmountUpdatesDTO);

    // When
    AmountUpdatesDTO result = legacyActualizationService.actualization(accessToken, serviceUrl, pagamento);
    // Then
    assertSame(koAmountUpdatesDTO, result);
  }
}
