package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.dto.generated.Payment;
import it.gov.pagopa.actualization.dto.generated.UpdatedPayment;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.actualization.client.NativeActualizationClient;
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
class NativeActualizationServiceTest {
  @Mock
  private NativeActualizationClient nativeActualizationClientMock;

  private NativeActualizationService nativeActualizationService;

  @BeforeEach
  void setUp() {
    nativeActualizationService = new NativeActualizationServiceImpl(nativeActualizationClientMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(nativeActualizationClientMock);
  }

  @Test
  void whenActualizationThenReturnUpdatedPayment() {
    // Given
    OrgSilServiceDTO orgSilServiceDTO = mock(OrgSilServiceDTO.class);
    UserInfo loggedUser = mock(UserInfo.class);
    String accessToken = "accessToken";
    Payment payment = new Payment();
    UpdatedPayment updatedPayment = new UpdatedPayment();

    Mockito.when(nativeActualizationClientMock.actualization(orgSilServiceDTO, loggedUser, accessToken, payment))
           .thenReturn(updatedPayment);

    // When
    UpdatedPayment result = nativeActualizationService.actualization(orgSilServiceDTO, loggedUser, accessToken, payment);

    // Then
    assertSame(updatedPayment, result);
  }

}
