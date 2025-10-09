package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.MixedDebtPositionDTO;
import it.gov.pagopa.pu.sil.service.debtposition.ManageDebtPositionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstantPaymentsFacadeTest {
  @Mock
  private ManageDebtPositionService manageDebtPositionServiceMock;

  @InjectMocks
  private InstantPaymentsFacade instantPaymentsFacade;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @DisplayName("Test createDebtPositionsFromMapping with mixed and non-mixed PaymentRequestMappingResult")
  void testCreateDebtPositionsFromMapping(boolean mixed) {
    PaymentRequestMappingResult paymentRequestMappingResult = mock(PaymentRequestMappingResult.class);
    String accessToken = "token";
    List<DebtPositionDTO> expectedResult = Collections.singletonList(mock(DebtPositionDTO.class));

    if (mixed) {
      List<MixedDebtPositionDTO> mixedList = Collections.singletonList(mock(MixedDebtPositionDTO.class));
      when(paymentRequestMappingResult.isMixed()).thenReturn(true);
      when(paymentRequestMappingResult.mixedDebtPositions()).thenReturn(mixedList);
      when(manageDebtPositionServiceMock.createMixedDebtPositions(mixedList, accessToken)).thenReturn(expectedResult);
    } else {
      List<DebtPositionDTO> debtList = Collections.singletonList(mock(DebtPositionDTO.class));
      when(paymentRequestMappingResult.isMixed()).thenReturn(false);
      when(paymentRequestMappingResult.debtPositions()).thenReturn(debtList);
      when(manageDebtPositionServiceMock.createDebtPositions(debtList, accessToken)).thenReturn(expectedResult);
    }

    List<DebtPositionDTO> result = instantPaymentsFacade.createDebtPositionsFromMapping(paymentRequestMappingResult, accessToken);
    assertEquals(expectedResult, result);

    if (mixed) {
      verify(manageDebtPositionServiceMock, times(1)).createMixedDebtPositions(anyList(), eq(accessToken));
      verify(manageDebtPositionServiceMock, never()).createDebtPositions(anyList(), anyString());
    } else {
      verify(manageDebtPositionServiceMock, times(1)).createDebtPositions(anyList(), eq(accessToken));
      verify(manageDebtPositionServiceMock, never()).createMixedDebtPositions(anyList(), anyString());
    }
  }
}
