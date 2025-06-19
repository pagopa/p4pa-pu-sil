package it.gov.pagopa.pu.sil.connector.amountupdates;

import it.gov.pagopa.amountupdates.legacy.dto.generated.Credentials;
import it.gov.pagopa.amountupdates.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.connector.amountupdates.client.AmountUpdatesLegacyClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
class AmountUpdatesLegacyComponentTest {
  @Mock
  private AmountUpdatesLegacyClient amountUpdatesLegacyClientMock;

  private AmountUpdatesLegacyComponent amountUpdatesLegacyComponent;

  @BeforeEach
  void setUp() {
    amountUpdatesLegacyComponent = new AmountUpdatesLegacyComponentImpl(amountUpdatesLegacyClientMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(amountUpdatesLegacyClientMock);
  }

  @Test
  void whenLoginThenReturnToken() {
    // Given
    String authUrl = "http://example.com";
    Credentials credentials = new Credentials()
      .username("testUser").password("testPassword");
    Token expectedToken = new Token();

    Mockito.when(amountUpdatesLegacyClientMock.login(credentials, authUrl))
           .thenReturn(expectedToken);

    // When
    Token result = amountUpdatesLegacyComponent.login(credentials, authUrl);

    // Then
    assertSame(expectedToken, result);
  }
}
