package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.legacy.controller.generated.DefaultApi;
import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.actualization.config.ActualizationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.RegistryLoggerTest;
import it.gov.pagopa.pu.sil.util.Utilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

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
    String orgFiscalCode = "orgFiscalCode";
    OrgSilServiceDTO orgSilServiceDTO = mock(OrgSilServiceDTO.class);
    String nav = "1234567890";
    UserInfo loggedUser = mock(UserInfo.class);
    String accessToken = "accessToken";
    Pagamento pagamento = new Pagamento()
      .cfEnteCreditore(orgFiscalCode)
      .importoPosizione(Pagamento.ImportoPosizioneEnum.S)
      .numeroAvviso(nav);
    PagamentoAggiornato expectedPagamentoAggiornato = new PagamentoAggiornato();

    Mockito.when(orgSilServiceDTO.getApplicationName()).thenReturn("TestApp");
    Mockito.when(orgSilServiceDTO.getServiceUrl()).thenReturn("http://example.com/notification-price");

    RegistryContextData expectedContextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.SIL_attualizzazioneImporti)
      .orgSilServiceName("TestApp")
      .iuv(Utilities.nav2Iuv(nav))
      .loggedUser(loggedUser)
      .build();

    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, expectedContextData, pagamento, false, false);

    Mockito.when(actualizationApisHolderMock.getAmountUpdatesLegacyApi(accessToken, "http://example.com"))
      .thenReturn(amountUpdatesLegacyApiClientMock);
    Mockito.when(amountUpdatesLegacyApiClientMock.attualizzazione(pagamento))
      .thenReturn(expectedPagamentoAggiornato);

    // When
    PagamentoAggiornato result = client.actualization(orgFiscalCode, orgSilServiceDTO, nav, loggedUser, accessToken, pagamento);

    // Then
    assertSame(expectedPagamentoAggiornato, result);
  }
}
