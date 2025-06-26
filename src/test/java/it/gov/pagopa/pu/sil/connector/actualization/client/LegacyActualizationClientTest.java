package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.legacy.controller.generated.DefaultApi;
import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.sil.connector.actualization.config.ActualizationApisHolder;
import it.gov.pagopa.pu.sil.exception.ActualizationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LegacyActualizationClientTest {
  @Mock
  private ActualizationApisHolder actualizationApisHolderMock;
  @Mock
  private DefaultApi amountUpdatesLegacyApiClientMock;

  private LegacyActualizationClient client;

  @BeforeEach
  void setUp() {
    client = new LegacyActualizationClient(actualizationApisHolderMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(actualizationApisHolderMock);
  }

  @Test
  void whenActualizationThenInvokeClient() {
    // Given
    String serviceUrl = "http://example.com/service";
    String token = "accessToken";
    Pagamento pagamento = new Pagamento();
    PagamentoAggiornato expectedPagamentoAggiornato = new PagamentoAggiornato();

    Mockito.when(actualizationApisHolderMock.getAmountUpdatesLegacyApi(token, serviceUrl))
      .thenReturn(amountUpdatesLegacyApiClientMock);
    Mockito.when(amountUpdatesLegacyApiClientMock.attualizzazione(pagamento))
      .thenReturn(expectedPagamentoAggiornato);
    // When
    PagamentoAggiornato result = client.actualization(token, serviceUrl, pagamento);

    // Then
    assertSame(expectedPagamentoAggiornato, result);
  }

  @Test
  void whenActualizationThenHttpClientErrorExceptionThenNull(){
    //Given
    String serviceUrl = "http://example.com/service";
    String token = "accessToken";
    Pagamento pagamento = new Pagamento();

    Mockito.when(actualizationApisHolderMock.getAmountUpdatesLegacyApi(token, serviceUrl))
      .thenReturn(amountUpdatesLegacyApiClientMock);
    Mockito.when(amountUpdatesLegacyApiClientMock.attualizzazione(pagamento))
      .thenThrow(RestClientException.class);

    // When Then
    assertThrows(ActualizationException.class, () ->
      client.actualization(token, serviceUrl, pagamento)
    );
  }
}
