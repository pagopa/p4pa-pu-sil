package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.legacy.controller.generated.DefaultApi;
import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.connector.actualization.config.ActualizationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
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
class LegacyBasicAuthClientTest {
  @Mock
  private ActualizationApisHolder actualizationApisHolderMock;
  @Mock
  private DefaultApi amountUpdatesLegacyApiClientMock;
  @Mock
  private RegistryLogger registryLoggerMock;

  private LegacyBasicAuthClient client;

  @BeforeEach
  void setUp() {
    client = new LegacyBasicAuthClient(actualizationApisHolderMock, registryLoggerMock);
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
    RegistryContextData contextData = mock(RegistryContextData.class);
    Mockito.when(actualizationApisHolderMock.getAmountUpdatesLegacyApi(null, authUrl))
           .thenReturn(amountUpdatesLegacyApiClientMock);
    Mockito.when(amountUpdatesLegacyApiClientMock.login(credential))
           .thenReturn(expectedToken);

    // When
    Token result = client.login(contextData, credential, authUrl);

    // Then
    assertSame(expectedToken, result);
  }
}
