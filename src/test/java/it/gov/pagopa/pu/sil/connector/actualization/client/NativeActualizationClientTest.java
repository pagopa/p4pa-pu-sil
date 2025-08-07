package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.controller.generated.DefaultApi;
import it.gov.pagopa.actualization.dto.generated.Payment;
import it.gov.pagopa.actualization.dto.generated.UpdatedPayment;
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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class NativeActualizationClientTest {
  @Mock
  private ActualizationApisHolder nativeActualizationApisHolderMock;
  @Mock
  private DefaultApi amountUpdatesLegacyApiClientMock;
  @Mock
  private RegistryLogger registryLoggerMock;

  private NativeActualizationClient client;

  @BeforeEach
  void setUp() {
    client = new NativeActualizationClient(nativeActualizationApisHolderMock, registryLoggerMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(nativeActualizationApisHolderMock, amountUpdatesLegacyApiClientMock, registryLoggerMock);
  }

  @Test
  void whenActualizationThenInvokeClient() {
    // Given
    String orgFiscalCode = "orgFiscalCode";
    OrgSilServiceDTO orgSilServiceDTO = mock(OrgSilServiceDTO.class);
    String nav = "31234567890";
    UserInfo loggedUser = mock(UserInfo.class);
    String accessToken = "accessToken";
    Payment payment = new Payment()
      .orgFiscalCode(orgFiscalCode)
      .nav(nav);
    UpdatedPayment updatedPayment = new UpdatedPayment().nav(nav).amountCents(100L);

    Mockito.when(orgSilServiceDTO.getApplicationName()).thenReturn("TestApp");
    Mockito.when(orgSilServiceDTO.getServiceUrl()).thenReturn("http://example.com/amount-update");

    RegistryContextData expectedContextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.SIL_attualizzazioneImporti)
      .orgSilServiceName("TestApp")
      .iuv(Utilities.nav2Iuv(payment.getNav()))
      .loggedUser(loggedUser)
      .build();

    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, expectedContextData, payment, false, false);

    Mockito.when(nativeActualizationApisHolderMock.getActualizationNativeApi(accessToken, "http://example.com"))
      .thenReturn(amountUpdatesLegacyApiClientMock);
    Mockito.when(amountUpdatesLegacyApiClientMock.actualization(payment))
      .thenReturn(updatedPayment);

    // When
    UpdatedPayment result = client.actualization(orgSilServiceDTO, loggedUser, accessToken, payment);

    // Then
    assertSame(updatedPayment, result);
  }
}
