package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.actualization.client.LegacyActualizationClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

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
  void whenActualizationThenReturnPagamentoAggiornato() {
    // Given
    String orgFiscalCode = "orgFiscalCode";
    OrgSilServiceDTO orgSilServiceDTO = mock(OrgSilServiceDTO.class);
    UserInfo loggedUser = mock(UserInfo.class);
    String accessToken = "accessToken";
    Pagamento pagamento = new Pagamento();
    PagamentoAggiornato expectedPagamentoAggiornato = new PagamentoAggiornato();

    Mockito.when(legacyActualizationClientMock.actualization(orgFiscalCode, orgSilServiceDTO, loggedUser, accessToken, pagamento))
           .thenReturn(expectedPagamentoAggiornato);

    // When
    PagamentoAggiornato result = legacyActualizationService.actualization(orgFiscalCode, orgSilServiceDTO, loggedUser, accessToken, pagamento);
    // Then
    assertSame(expectedPagamentoAggiornato, result);
  }

}
