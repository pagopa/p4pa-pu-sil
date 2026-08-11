package it.gov.pagopa.pu.sil.connector.sil.actualization.client;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.sil.actualization.config.LegacyActualizationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.RegistryLoggerTest;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.gov.pagopa.sil.actualizationlegacy.client.generated.DefaultApi;
import it.gov.pagopa.sil.actualizationlegacy.dto.generated.Pagamento;
import it.gov.pagopa.sil.actualizationlegacy.dto.generated.PagamentoAggiornato;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyActualizationClientTest {
  @Mock
  private LegacyActualizationApisHolder legacyActualizationApisHolderMock;
  @Mock
  private DefaultApi amountUpdatesLegacyApiClientMock;
  @Mock
  private RegistryLogger registryLoggerMock;

  private LegacyActualizationClient client;

  private static final String AUX_DIGIT = "3";

  @BeforeEach
  void setUp() {
    client = new LegacyActualizationClient(legacyActualizationApisHolderMock, registryLoggerMock, AUX_DIGIT);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(legacyActualizationApisHolderMock, amountUpdatesLegacyApiClientMock, registryLoggerMock);
  }

  @Test
  void whenActualizationThenInvokeClient() {
    // Given
    String orgFiscalCode = "orgFiscalCode";
    OrgSilServiceDTO orgSilServiceDTO = mock(OrgSilServiceDTO.class);
    String nav = "31234567890";
    UserInfo loggedUser = mock(UserInfo.class);
    String accessToken = "accessToken";
    Pagamento pagamento = new Pagamento()
      .cfEnteCreditore(orgFiscalCode)
      .importoPosizione(Pagamento.ImportoPosizioneEnum.S)
      .numeroAvviso(nav);
    PagamentoAggiornato expectedPagamentoAggiornato = new PagamentoAggiornato();

    when(orgSilServiceDTO.getApplicationName()).thenReturn("TestApp");
    when(orgSilServiceDTO.getServiceUrl()).thenReturn("http://example.com/notification-price");

    RegistryContextData expectedContextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.SIL_attualizzazioneImporti)
      .orgSilServiceName("TestApp")
      .iuv(Utilities.nav2Iuv(pagamento.getNumeroAvviso(), AUX_DIGIT))
      .loggedUser(loggedUser)
      .build();

    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, expectedContextData, pagamento, false, false);

    when(legacyActualizationApisHolderMock.getAmountUpdatesLegacyApi(accessToken, "http://example.com"))
      .thenReturn(amountUpdatesLegacyApiClientMock);
    when(amountUpdatesLegacyApiClientMock.attualizzazione(pagamento))
      .thenReturn(expectedPagamentoAggiornato);

    // When
    PagamentoAggiornato result = client.actualization(orgFiscalCode, orgSilServiceDTO, loggedUser, accessToken, pagamento);

    // Then
    assertSame(expectedPagamentoAggiornato, result);
  }
}
