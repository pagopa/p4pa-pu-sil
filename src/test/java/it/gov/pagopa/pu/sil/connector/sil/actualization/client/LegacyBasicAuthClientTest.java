package it.gov.pagopa.pu.sil.connector.sil.actualization.client;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.connector.sil.actualization.config.LegacyActualizationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.RegistryLoggerTest;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.gov.pagopa.sil.actualizationlegacy.client.generated.DefaultApi;
import it.gov.pagopa.sil.actualizationlegacy.dto.generated.Credentials;
import it.gov.pagopa.sil.actualizationlegacy.dto.generated.Token;
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
class LegacyBasicAuthClientTest {
  @Mock
  private LegacyActualizationApisHolder legacyActualizationApisHolderMock;
  @Mock
  private DefaultApi amountUpdatesLegacyApiClientMock;
  @Mock
  private RegistryLogger registryLoggerMock;

  private LegacyBasicAuthClient client;

  private static final String AUX_DIGIT = "3";

  @BeforeEach
  void setUp() {
    client = new LegacyBasicAuthClient(legacyActualizationApisHolderMock, registryLoggerMock, AUX_DIGIT);
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
      .iuv(Utilities.nav2Iuv(nav, AUX_DIGIT))
      .loggedUser(loggedUser)
      .build();

    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, expectedContextData, credential, false, false);

    when(legacyActualizationApisHolderMock.getAmountUpdatesLegacyApi(null, "http://example.com"))
           .thenReturn(amountUpdatesLegacyApiClientMock);
    when(amountUpdatesLegacyApiClientMock.login(credential))
           .thenReturn(expectedToken);

    // When
    Token result = client.login(orgFiscalCode, orgSilServiceName, nav, loggedUser, credential, authUrl);

    // Then
    assertSame(expectedToken, result);
  }
}
