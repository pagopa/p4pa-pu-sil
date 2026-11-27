package it.gov.pagopa.pu.sil.connector.sil.actualization;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.sil.actualization.client.ActualizationClient;
import it.gov.pagopa.sil.actualization.dto.generated.Payment;
import it.gov.pagopa.sil.actualization.dto.generated.UpdatedPayment;
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
class ActualizationServiceTest {
  @Mock
  private ActualizationClient actualizationClientMock;

  private ActualizationService actualizationService;

  @BeforeEach
  void setUp() {
    actualizationService = new ActualizationServiceImpl(actualizationClientMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(actualizationClientMock);
  }

  @Test
  void whenActualizationThenReturnUpdatedPayment() {
    // Given
    OrgSilServiceDTO orgSilServiceDTO = mock(OrgSilServiceDTO.class);
    UserInfo loggedUser = mock(UserInfo.class);
    String accessToken = "accessToken";
    Payment payment = new Payment();
    UpdatedPayment updatedPayment = new UpdatedPayment();

    when(actualizationClientMock.actualization(orgSilServiceDTO, loggedUser, accessToken, payment))
           .thenReturn(updatedPayment);

    // When
    UpdatedPayment result = actualizationService.actualization(orgSilServiceDTO, loggedUser, accessToken, payment);

    // Then
    assertSame(updatedPayment, result);
  }

}
