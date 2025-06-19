package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.connector.actualization.client.LegacyActualizationClient;
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

  private LegacyActualizationService legacyActualizationService;

  @BeforeEach
  void setUp() {
    legacyActualizationService = new LegacyActualizationServiceImpl(legacyActualizationClientMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(legacyActualizationClientMock);
  }

  @Test
  void whenLoginThenReturnToken() {
    // Given
    String authUrl = "http://example.com";
    Credentials credentials = new Credentials()
      .username("testUser").password("testPassword");
    Token expectedToken = new Token();

    Mockito.when(legacyActualizationClientMock.login(credentials, authUrl))
           .thenReturn(expectedToken);

    // When
    Token result = legacyActualizationService.login(credentials, authUrl);

    // Then
    assertSame(expectedToken, result);
  }

  @Test
  void whenActualizationThenReturnPagamentoAggiornato() {
    // Given
    String accessToken = "accessToken";
    String serviceUrl = "http://example.com/service";
    Pagamento pagamento = new Pagamento();
    PagamentoAggiornato expectedPagamentoAggiornato = new PagamentoAggiornato();

    Mockito.when(legacyActualizationClientMock.actualization(accessToken, serviceUrl, pagamento))
           .thenReturn(expectedPagamentoAggiornato);

    // When
    PagamentoAggiornato result = legacyActualizationService.actualization(accessToken, serviceUrl, pagamento);
    // Then
    assertSame(expectedPagamentoAggiornato, result);
  }
}
