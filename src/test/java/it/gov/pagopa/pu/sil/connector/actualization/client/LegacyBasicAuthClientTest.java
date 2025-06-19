package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.amountupdates.legacy.controller.generated.DefaultApi;
import it.gov.pagopa.amountupdates.legacy.dto.generated.Credentials;
import it.gov.pagopa.amountupdates.legacy.dto.generated.Token;
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
class LegacyBasicAuthClientTest {
  @Mock
  private ActualizationApisHolder actualizationApisHolderMock;
  @Mock
  private DefaultApi amountUpdatesLegacyApiClientMock;

  private LegacyBasicAuthClient client;

  @BeforeEach
  void setUp() {
    client = new LegacyBasicAuthClient(actualizationApisHolderMock);
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
}
