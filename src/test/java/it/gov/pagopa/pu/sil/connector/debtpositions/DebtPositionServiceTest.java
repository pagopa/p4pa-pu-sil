package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.sil.connector.debtpositions.client.DebtPositionClient;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class DebtPositionServiceTest {

  @Mock
  private DebtPositionClient clientMock;

  private DebtPositionService service;

  @BeforeEach
  void init() {
    service = new DebtPositionServiceImpl(clientMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(clientMock);
  }

  @Test
  void whenGetDebtPositionDTOByInstallmentIdThenReturnDebtPositionDTODTO() {
    // Given
    Long installmentId = 1L;
    String accessToken = "ACCESSTOKEN";
    DebtPositionDTO expectedResult = new DebtPositionDTO();

    Mockito.when(clientMock.getDebtPositionDTOByInstallmentId(installmentId, accessToken)).thenReturn(expectedResult);

    // When
    DebtPositionDTO result = service.getDebtPositionDTOByInstallmentId(installmentId, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

  @Test
  void whenGetDebtPositionsByOrganizationIdAndIuvThenReturnListOfDebtPositionDTO() {
    // Given
    Long organizationId = 1L;
    String iuv = "IUV";
    List<DebtPositionOrigin> debtPositionOrigin = null;
    String accessToken = "ACCESSTOKEN";
    List<DebtPositionDTO> expectedResult = List.of(new DebtPositionDTO());

    Mockito.when(clientMock.getDebtPositionsByOrganizationIdAndIuv(organizationId, iuv, debtPositionOrigin, accessToken)).thenReturn(expectedResult);

    // When
    List<DebtPositionDTO> result = service.getDebtPositionsByOrganizationIdAndIuv(organizationId, iuv, debtPositionOrigin, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

  @Test
  void whenGetDebtPositionsByOrganizationIdAndIudThenReturnListOfDebtPositionDTO() {
    // Given
    Long organizationId = 1L;
    String iud = "IUD";
    List<DebtPositionOrigin> debtPositionOrigin = null;
    String accessToken = "ACCESSTOKEN";
    List<DebtPositionDTO> expectedResult = List.of(new DebtPositionDTO());

    Mockito.when(clientMock.getDebtPositionsByOrganizationIdAndIud(organizationId, iud, debtPositionOrigin, accessToken)).thenReturn(expectedResult);

    // When
    List<DebtPositionDTO> result = service.getDebtPositionsByOrganizationIdAndIud(organizationId, iud, debtPositionOrigin, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

  @Test
  void whenCreateDebtPositionThenReturnDebtPositionDTO() {
    // Given
    DebtPositionDTO debtPositionDTO = new DebtPositionDTO();
    String accessToken = "ACCESSTOKEN";
    ResponseEntity<DebtPositionDTO> expectedResult = ResponseEntity.ok().header("X-Workflow-Id", "workflow-id").body(debtPositionDTO);

    Mockito.when(clientMock.createDebtPosition(debtPositionDTO, accessToken)).thenReturn(expectedResult);

    // When
    Pair<DebtPositionDTO, String> result = service.createDebtPosition(debtPositionDTO, accessToken);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertSame(expectedResult.getBody(), result.getLeft());
    Assertions.assertSame("workflow-id", result.getRight());
  }

  @Test
  void whenManageDebtPositionInstallmentsThenReturnDebtPositionDTO() {
    // Given
    Long debtPositionId = 1L;
    String accessToken = "ACCESSTOKEN";
    ManageDebtPositionDTO manageDebtPositionDTO = new ManageDebtPositionDTO();
    ResponseEntity<DebtPositionDTO> expectedResult = ResponseEntity.ok().header("X-Workflow-Id", "workflow-id").body(new DebtPositionDTO());

    Mockito.when(clientMock.manageDebtPositionInstallments(debtPositionId, manageDebtPositionDTO, accessToken)).thenReturn(expectedResult);

    // When
    Pair<DebtPositionDTO, String> result = service.manageDebtPositionInstallments(debtPositionId, manageDebtPositionDTO, accessToken);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertSame(expectedResult.getBody(), result.getLeft());
    Assertions.assertSame("workflow-id", result.getRight());
  }

  @Test
  void whenGetDebtPositionByInstallmentIdThenReturnDebtPositionDTODTO() {
    // Given
    Long installmentId = 1L;
    String accessToken = "ACCESSTOKEN";
    DebtPosition expectedResult = new DebtPosition();

    Mockito.when(clientMock.getDebtPositionByInstallmentId(installmentId, accessToken)).thenReturn(expectedResult);

    // When
    DebtPosition result = service.getDebtPositionByInstallmentId(installmentId, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

  @Test
  void whenCreateMixedDebtPositionThenReturnDebtPositionDTO() {
    // Given
    MixedDebtPositionDTO mixedDebtPositionDTO = new MixedDebtPositionDTO();
    DebtPositionDTO expectedDebtPositionDTO = new DebtPositionDTO();
    String accessToken = "ACCESSTOKEN";
    ResponseEntity<DebtPositionDTO> expectedResult = ResponseEntity.ok().header("X-Workflow-Id", "workflow-id").body(expectedDebtPositionDTO);

    Mockito.when(clientMock.createMixedDebtPosition(mixedDebtPositionDTO, accessToken)).thenReturn(expectedResult);

    // When
    Pair<DebtPositionDTO, String> result = service.createMixedDebtPosition(mixedDebtPositionDTO, accessToken);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertSame(expectedResult.getBody(), result.getLeft());
    Assertions.assertSame("workflow-id", result.getRight());
  }

  @Test
  void whenGetDebtPositionsByDebtorFiscalCodeAndDebtorEntityTypeThenReturnListOfDebtPositionDTO() {
    // Given
    String debtorFiscalCode = "FISCALCODE";
    PersonEntityType debtorEntityType = PersonEntityType.F;
    String accessToken = "ACCESSTOKEN";
    List<DebtPositionDTO> expectedResult = List.of(new DebtPositionDTO());

    Mockito.when(clientMock.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
      debtorFiscalCode,
      debtorEntityType,
      null,
      accessToken
    )).thenReturn(expectedResult);

    // When
    List<DebtPositionDTO> result = service.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(debtorFiscalCode, debtorEntityType, null, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }
}
