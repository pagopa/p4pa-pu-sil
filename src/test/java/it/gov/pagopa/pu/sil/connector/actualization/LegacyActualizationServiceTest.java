package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato.CodiceEnum;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.actualization.client.LegacyActualizationClient;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.exception.PaymentInvalidStatusException;
import it.gov.pagopa.pu.sil.exception.PaymentNotFoundException;
import it.gov.pagopa.pu.sil.exception.PaymentNotNotifiedException;
import it.gov.pagopa.pu.sil.mapper.AmountUpdatesMapper;
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
    String orgFiscalCode = "orgFiscalCode";
    OrgSilServiceDTO orgSilServiceDTO = mock(OrgSilServiceDTO.class);
    String nav = "nav123";
    UserInfo loggedUser = mock(UserInfo.class);
    String accessToken = "accessToken";
    Pagamento pagamento = new Pagamento();
    PagamentoAggiornato expectedPagamentoAggiornato = new PagamentoAggiornato();
    ActualizationResultDTO expectedAmountUpdatesDTO = new ActualizationResultDTO();

    Mockito.when(legacyActualizationClientMock.actualization(orgFiscalCode, orgSilServiceDTO, nav, loggedUser, accessToken, pagamento))
           .thenReturn(expectedPagamentoAggiornato);
    Mockito.when(amountUpdatesMapperMock.pagamentoAggiornato2AmountUpdatesDTO(expectedPagamentoAggiornato))
            .thenReturn(expectedAmountUpdatesDTO);

    // When
    ActualizationResultDTO result = legacyActualizationService.actualization(orgFiscalCode, orgSilServiceDTO, nav, loggedUser, accessToken, pagamento);
    // Then
    assertSame(expectedAmountUpdatesDTO, result);
  }

  @Test
  void whenCodice002ThenThrowPaymentNotFoundException() {
    String orgFiscalCode = "orgFiscalCode";
    OrgSilServiceDTO orgSilServiceDTO = mock(OrgSilServiceDTO.class);
    String nav = "nav123";
    UserInfo loggedUser = mock(UserInfo.class);
    String accessToken = "accessToken";
    Pagamento pagamento = new Pagamento();
    PagamentoAggiornato pagamentoAggiornato = new PagamentoAggiornato();
    pagamentoAggiornato.setCodice(CodiceEnum._002);
    pagamentoAggiornato.setDettaglio("Not found");
    Mockito.when(legacyActualizationClientMock.actualization(orgFiscalCode, orgSilServiceDTO, nav, loggedUser, accessToken, pagamento))
           .thenReturn(pagamentoAggiornato);
    assertThrows(PaymentNotFoundException.class, () -> legacyActualizationService.actualization(orgFiscalCode, orgSilServiceDTO, nav, loggedUser, accessToken, pagamento));
  }

  @Test
  void whenCodice003ThenThrowPaymentNotNotifiedException() {
    String orgFiscalCode = "orgFiscalCode";
    OrgSilServiceDTO orgSilServiceDTO = mock(OrgSilServiceDTO.class);
    String nav = "nav123";
    UserInfo loggedUser = mock(UserInfo.class);
    String accessToken = "accessToken";
    Pagamento pagamento = new Pagamento();
    PagamentoAggiornato pagamentoAggiornato = new PagamentoAggiornato();
    pagamentoAggiornato.setCodice(CodiceEnum._003);
    pagamentoAggiornato.setDettaglio("Not notified");
    Mockito.when(legacyActualizationClientMock.actualization(orgFiscalCode, orgSilServiceDTO, nav, loggedUser, accessToken, pagamento))
           .thenReturn(pagamentoAggiornato);
    assertThrows(PaymentNotNotifiedException.class, () -> legacyActualizationService.actualization(orgFiscalCode, orgSilServiceDTO, nav, loggedUser, accessToken, pagamento));
  }

  @Test
  void whenCodice004ThenThrowPaymentInvalidStatusException() {
    String orgFiscalCode = "orgFiscalCode";
    OrgSilServiceDTO orgSilServiceDTO = mock(OrgSilServiceDTO.class);
    String nav = "nav123";
    UserInfo loggedUser = mock(UserInfo.class);
    String accessToken = "accessToken";
    Pagamento pagamento = new Pagamento();
    PagamentoAggiornato pagamentoAggiornato = new PagamentoAggiornato();
    pagamentoAggiornato.setCodice(CodiceEnum._004);
    pagamentoAggiornato.setDettaglio("Invalid status");
    Mockito.when(legacyActualizationClientMock.actualization(orgFiscalCode, orgSilServiceDTO, nav, loggedUser, accessToken, pagamento))
           .thenReturn(pagamentoAggiornato);
    assertThrows(PaymentInvalidStatusException.class, () -> legacyActualizationService.actualization(orgFiscalCode, orgSilServiceDTO, nav, loggedUser, accessToken, pagamento));
  }
}
