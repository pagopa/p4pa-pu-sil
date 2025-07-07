package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.legacy.controller.generated.DefaultApi;
import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.sil.connector.actualization.config.ActualizationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.RegistryLoggerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LegacyActualizationClientTest {
  @Mock
  private ActualizationApisHolder actualizationApisHolderMock;
  @Mock
  private DefaultApi amountUpdatesLegacyApiClientMock;
  @Mock
  private RegistryLogger registryLoggerMock;

  private LegacyActualizationClient client;

  @BeforeEach
  void setUp() {
    client = new LegacyActualizationClient(actualizationApisHolderMock, registryLoggerMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(actualizationApisHolderMock, amountUpdatesLegacyApiClientMock, registryLoggerMock);
  }

  @Test
  void whenActualizationThenInvokeClient() {
    // Given
    String serviceUrl = "http://example.com/service";
    String token = "accessToken";
    Pagamento pagamento = new Pagamento()
      .cfEnteCreditore("orgFiscalCode")
      .importoPosizione(Pagamento.ImportoPosizioneEnum.S)
      .numeroAvviso("1234567890");
    PagamentoAggiornato expectedPagamentoAggiornato = new PagamentoAggiornato();
    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode("orgFiscalCode")
      .eventType(RegistryEventType.SIL_notificaPagamento)
      .build();
    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, contextData, pagamento, false, false);

    Mockito.when(actualizationApisHolderMock.getAmountUpdatesLegacyApi(token, serviceUrl))
      .thenReturn(amountUpdatesLegacyApiClientMock);
    Mockito.when(amountUpdatesLegacyApiClientMock.attualizzazione(pagamento))
      .thenReturn(expectedPagamentoAggiornato);
    // When
    PagamentoAggiornato result = client.actualization(contextData, token, serviceUrl, pagamento);

    // Then
    assertSame(expectedPagamentoAggiornato, result);
  }
}
