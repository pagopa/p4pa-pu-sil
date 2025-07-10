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
  void whenGetDebtPositionTypeOrgByIdThenInvokeClient() {
    // Given
    Long debtPositionTypeOrgId = 1L;
    String debtPositionTypeOrgCode = "CODE";
    String accessToken = "ACCESSTOKEN";
    DebtPositionTypeOrg expectedResult = new DebtPositionTypeOrg();

    Mockito.when(clientMock.getDebtPositionTypeOrgByOrganizationIdAndCode(Mockito.same(debtPositionTypeOrgId), Mockito.same(debtPositionTypeOrgCode), Mockito.same(accessToken)))
      .thenReturn(expectedResult);

    // When
    DebtPositionTypeOrg result = service.getDebtPositionTypeOrgByOrgIdAndType(debtPositionTypeOrgId, debtPositionTypeOrgCode, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

  @Test
  void whenCountExistingInstallmentsByIudIuvNavThenReturnCorrectCount() {
    // Given
    Long organizationId = 1L;
    String iud = "IUD";
    String iuv = "IUV";
    String nav = "NAV";
    String accessToken = "ACCESSTOKEN";
    Long expectedCount = 10L;

    Mockito.when(clientMock.countExistingInstallmentsByIudIuvNav(
        Mockito.same(organizationId),
        Mockito.same(iud),
        Mockito.same(iuv),
        Mockito.same(nav),
        Mockito.same(accessToken)))
      .thenReturn(expectedCount);

    // When
    Long result = service.countExistingInstallmentsByIudIuvNav(organizationId, iud, iuv, nav, accessToken);

    // Then
    Assertions.assertEquals(expectedCount, result);
  }

  @Test
  void whenGetDebtPositionTypeByIdThenReturnDebtPositionType() {
    // Given
    String accessToken = "ACCESSTOKEN";
    Long debtPositionTypeId = 1L;
    DebtPositionType expectedResult = new DebtPositionType();

    Mockito.when(clientMock.getDebtPositionTypeById(debtPositionTypeId, accessToken)).thenReturn(expectedResult);

    //when
    DebtPositionType result = service.getDebtPositionTypeById(debtPositionTypeId, accessToken);

    //then
    Assertions.assertEquals(expectedResult, result);
  }

  @Test
  void whenGetInstallmentsByOrganizationIdAndNavThenReturnListOfInstallmentDTO() {
    // Given
    String accessToken = "ACCESSTOKEN";
    List<InstallmentDTO> expectedResult = List.of(new InstallmentDTO());

    Mockito.when(clientMock.getInstallmentsByOrganizationIdAndNav(1L, "NAV", null, accessToken)).thenReturn(expectedResult);

    //when
    List<InstallmentDTO> result = service.getInstallmentsByOrganizationIdAndNav(1L, "NAV", null, accessToken);

    //then
    Assertions.assertEquals(expectedResult, result);
  }

  @Test
  void whenGetDebtPositionByInstallmentIdThenReturnDebtPositionDTO() {
    // Given
    Long installmentId = 1L;
    String accessToken = "ACCESSTOKEN";
    DebtPositionDTO expectedResult = new DebtPositionDTO();

    Mockito.when(clientMock.getDebtPositionByInstallmentId(installmentId, accessToken)).thenReturn(expectedResult);

    // When
    DebtPositionDTO result = service.getDebtPositionByInstallmentId(installmentId, accessToken);

    // Then
    Assertions.assertEquals(expectedResult, result);
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
    Assertions.assertEquals(expectedResult, result);
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
    Assertions.assertEquals(expectedResult, result);
  }

  @Test
  void whenGetReceiptByIdThenReturnReceiptDTO() {
    // Given
    Long receiptId = 1L;
    String accessToken = "ACCESSTOKEN";
    ReceiptDTO expectedResult = new ReceiptDTO();

    Mockito.when(clientMock.getReceiptById(receiptId, accessToken)).thenReturn(expectedResult);

    // When
    ReceiptDTO result = service.getReceiptById(receiptId, accessToken);

    // Then
    Assertions.assertEquals(expectedResult, result);
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
    Assertions.assertEquals(expectedResult.getBody(), result.getLeft());
    Assertions.assertEquals("workflow-id", result.getRight());
  }

  @Test
  void whenFindAuthorizedByTransferSemanticKeyThenReturnInstallment() {
    // Given
    String accessToken = "ACCESSTOKEN";
    Long organizationId = 1L;
    String iuv = "IUV";
    String iur = "IUR";
    int transferIndex = 1;
    String operatorExternalUserId = "OPERATOR_ID";

    InstallmentNoPII expectedResult = new InstallmentNoPII();

    Mockito.when(clientMock.findAuthorizedByTransferSemanticKey(
        organizationId, iuv, iur, transferIndex, operatorExternalUserId, accessToken))
      .thenReturn(expectedResult);

    // When
    InstallmentNoPII result = service.findAuthorizedByTransferSemanticKey(
        organizationId, iuv, iur, transferIndex, operatorExternalUserId, accessToken);
    // Then
    Assertions.assertEquals(expectedResult, result);
  }
}
