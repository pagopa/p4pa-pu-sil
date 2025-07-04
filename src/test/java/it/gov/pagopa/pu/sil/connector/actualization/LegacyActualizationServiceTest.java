package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato.CodiceEnum;
import it.gov.pagopa.pu.sil.connector.actualization.client.LegacyActualizationClient;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.exception.PaymentInvalidStatusException;
import it.gov.pagopa.pu.sil.exception.PaymentNotFoundException;
import it.gov.pagopa.pu.sil.exception.PaymentNotNotifiedException;
import it.gov.pagopa.pu.sil.mapper.AmountUpdatesMapper;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

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
    ActualizationResultDTO expectedAmountUpdatesDTO = new ActualizationResultDTO();
    RegistryContextData contextData = mock(RegistryContextData.class);
    Mockito.when(legacyActualizationClientMock.actualization(contextData, accessToken, serviceUrl, pagamento))
           .thenReturn(expectedPagamentoAggiornato);
    Mockito.when(amountUpdatesMapperMock.pagamentoAggiornato2AmountUpdatesDTO(expectedPagamentoAggiornato))
            .thenReturn(expectedAmountUpdatesDTO);

    // When
    ActualizationResultDTO result = legacyActualizationService.actualization(contextData, accessToken, serviceUrl, pagamento);
    // Then
    assertSame(expectedAmountUpdatesDTO, result);
  }

  @Test
  void whenCodice002ThenThrowPaymentNotFoundException() {
    String accessToken = "accessToken";
    String serviceUrl = "http://example.com/service";
    Pagamento pagamento = new Pagamento();
    RegistryContextData contextData = mock(RegistryContextData.class);
    PagamentoAggiornato pagamentoAggiornato = new PagamentoAggiornato();
    pagamentoAggiornato.setCodice(CodiceEnum._002);
    pagamentoAggiornato.setDettaglio("Not found");
    Mockito.when(legacyActualizationClientMock.actualization(contextData, accessToken, serviceUrl, pagamento))
           .thenReturn(pagamentoAggiornato);
    assertThrows(PaymentNotFoundException.class, () -> legacyActualizationService.actualization(contextData, accessToken, serviceUrl, pagamento));
  }

  @Test
  void whenCodice003ThenThrowPaymentNotNotifiedException() {
    String accessToken = "accessToken";
    String serviceUrl = "http://example.com/service";
    Pagamento pagamento = new Pagamento();
    RegistryContextData contextData = mock(RegistryContextData.class);
    PagamentoAggiornato pagamentoAggiornato = new PagamentoAggiornato();
    pagamentoAggiornato.setCodice(CodiceEnum._003);
    pagamentoAggiornato.setDettaglio("Not notified");
    Mockito.when(legacyActualizationClientMock.actualization(contextData, accessToken, serviceUrl, pagamento))
           .thenReturn(pagamentoAggiornato);
    assertThrows(PaymentNotNotifiedException.class, () -> legacyActualizationService.actualization(contextData, accessToken, serviceUrl, pagamento));
  }

  @Test
  void whenCodice004ThenThrowPaymentInvalidStatusException() {
    String accessToken = "accessToken";
    String serviceUrl = "http://example.com/service";
    Pagamento pagamento = new Pagamento();
    PagamentoAggiornato pagamentoAggiornato = new PagamentoAggiornato();
    RegistryContextData contextData = mock(RegistryContextData.class);
    pagamentoAggiornato.setCodice(CodiceEnum._004);
    pagamentoAggiornato.setDettaglio("Invalid status");
    Mockito.when(legacyActualizationClientMock.actualization(contextData, accessToken, serviceUrl, pagamento))
           .thenReturn(pagamentoAggiornato);
    assertThrows(PaymentInvalidStatusException.class, () -> legacyActualizationService.actualization(contextData, accessToken, serviceUrl, pagamento));
  }
}
