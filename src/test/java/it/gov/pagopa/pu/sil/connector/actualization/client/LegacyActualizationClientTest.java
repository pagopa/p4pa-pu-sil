package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.legacy.controller.generated.DefaultApi;
import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.connector.actualization.config.ActualizationApisHolder;
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
  void whenLoginThenInvokeClient() {
    // Given
    String authUrl = "http://example.com/auth";
    Credentials credential = new Credentials("username", "password");
    Token expectedToken = new Token();

    Mockito.when(actualizationApisHolderMock.getAmountUpdatesLegacyApi(null, authUrl))
           .thenReturn(amountUpdatesLegacyApiClientMock);
    Mockito.when(amountUpdatesLegacyApiClientMock.login(credential))
           .thenReturn(expectedToken);

    // When
    Token result = client.login(credential, authUrl);

    // Then
    assertSame(expectedToken, result);
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
}
