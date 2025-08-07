package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.legacy.controller.generated.DefaultApi;
import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.connector.actualization.config.LegacyActualizationApisHolder;
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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LegacyBasicAuthClientTest {
  @Mock
  private LegacyActualizationApisHolder legacyActualizationApisHolderMock;
  @Mock
  private DefaultApi amountUpdatesLegacyApiClientMock;
  @Mock
  private RegistryLogger registryLoggerMock;

  private LegacyBasicAuthClient client;

  @BeforeEach
  void setUp() {
    client = new LegacyBasicAuthClient(legacyActualizationApisHolderMock, registryLoggerMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(legacyActualizationApisHolderMock);
  }

  @Test
  void whenLoginThenInvokeClient() {
    // Given
    String orgFiscalCode = "orgFiscalCode";
    String orgSilServiceName = "TestService";
    String nav = "31234567890";
    UserInfo loggedUser = mock(UserInfo.class);
    String authUrl = "http://example.com/login";
    Credentials credential = new Credentials("username", "password");
    Token expectedToken = new Token();

    RegistryContextData expectedContextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.SIL_attualizzazioneImporti)
      .orgSilServiceName(orgSilServiceName)
      .iuv(Utilities.nav2Iuv(nav))
      .loggedUser(loggedUser)
      .build();

    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, expectedContextData, credential, false, false);

    Mockito.when(legacyActualizationApisHolderMock.getAmountUpdatesLegacyApi(null, "http://example.com"))
           .thenReturn(amountUpdatesLegacyApiClientMock);
    Mockito.when(amountUpdatesLegacyApiClientMock.login(credential))
           .thenReturn(expectedToken);

    // When
    Token result = client.login(orgFiscalCode, orgSilServiceName, nav, loggedUser, credential, authUrl);

    // Then
    assertSame(expectedToken, result);
  }
}
