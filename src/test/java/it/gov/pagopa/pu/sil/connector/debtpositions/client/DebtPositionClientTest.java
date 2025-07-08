package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.controller.generated.*;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApisHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class DebtPositionClientTest {

  @Mock
  private DebtPositionsApisHolder apisHolderMock;
  @Mock
  private DebtPositionTypeOrgSearchControllerApi debtPositionTypeOrgSearchControllerApiMock;
  @Mock
  private DebtPositionTypeEntityControllerApi debtPositionTypeEntityControllerApiMock;
  @Mock
  private InstallmentApi installmentApiMock;
  @Mock
  private InstallmentNoPiiSearchControllerApi installmentNoPiiSearchControllerApiMock;
  @Mock
  private DebtPositionApi debtPositionApiMock;
  @Mock
  private ReceiptApi receiptApiMock;


  private DebtPositionClient client;

  @BeforeEach
  void setUp() {
    client = new DebtPositionClient(apisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      apisHolderMock,
      debtPositionTypeOrgSearchControllerApiMock,
      debtPositionTypeEntityControllerApiMock,
      installmentApiMock
      );
  }

  @Test
  void whenGetDebtPositionTypeOrgByOrganizationIdAndCodeThenInvokeApi(){
    //Given
    String accessToken = "ACCESSTOKEN";
    long debtPositionTypeOrgId = 1L;
    String debtPositionTypeOrgCode = "CODE";
    DebtPositionTypeOrg expectedResult = new DebtPositionTypeOrg();

    Mockito.when(apisHolderMock.getDebtPositionTypeOrgSearchControllerApi(accessToken))
      .thenReturn(debtPositionTypeOrgSearchControllerApiMock);
    Mockito.when(debtPositionTypeOrgSearchControllerApiMock.crudDebtPositionTypeOrgsFindByOrganizationIdAndCode(debtPositionTypeOrgId, debtPositionTypeOrgCode))
      .thenReturn(expectedResult);

    // When
    DebtPositionTypeOrg result = client.getDebtPositionTypeOrgByOrganizationIdAndCode(debtPositionTypeOrgId, debtPositionTypeOrgCode, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

  @Test
  void givenNotExistentTypeOrgWhenGetDebtPositionTypeOrgByOrganizationIdAndCodeThenNull(){
    //Given
    String accessToken = "ACCESSTOKEN";
    long debtPositionTypeOrgId = 1L;
    String debtPositionTypeOrgCode = "CODE";

    Mockito.when(apisHolderMock.getDebtPositionTypeOrgSearchControllerApi(accessToken))
      .thenReturn(debtPositionTypeOrgSearchControllerApiMock);
    Mockito.when(debtPositionTypeOrgSearchControllerApiMock.crudDebtPositionTypeOrgsFindByOrganizationIdAndCode(debtPositionTypeOrgId, debtPositionTypeOrgCode))
      .thenThrow(HttpClientErrorException.NotFound.class);

    // When
    DebtPositionTypeOrg response = client.getDebtPositionTypeOrgByOrganizationIdAndCode(debtPositionTypeOrgId, debtPositionTypeOrgCode, accessToken);

    // Then
    Assertions.assertNull(response);
  }

  @Test
  void whenCountExistingInstallmentsByIudIuvNavThenInvokeApi() {
      // Given
      String accessToken = "ACCESSTOKEN";
      long organizationId = 1L;
      String iud = "IUD";
      String iuv = "IUV";
      String nav = "NAV";
      long expectedCount = 5L;

      Mockito.when(apisHolderMock.getInstallmentNoPiiSearchControllerApi(accessToken))
          .thenReturn(installmentNoPiiSearchControllerApiMock);
      Mockito.when(installmentNoPiiSearchControllerApiMock.crudInstallmentsCountExistingInstallments(organizationId, iud, iuv, nav))
          .thenReturn(expectedCount);

      // When
      Long result = client.countExistingInstallmentsByIudIuvNav(organizationId, iud, iuv, nav, accessToken);

      // Then
      Assertions.assertEquals(expectedCount, result);
  }

  @ParameterizedTest
  @ValueSource(longs = {1L, 2L})
  void whenGetDebtPositionTypeByIdThenInvokeApi(Long debtPositionTypeId) {
    // Given
    String accessToken = "ACCESSTOKEN";
    DebtPositionType expectedResult;

    Mockito.when(apisHolderMock.getDebtPositionTypeEntityControllerApi(accessToken))
      .thenReturn(debtPositionTypeEntityControllerApiMock);
    if(debtPositionTypeId == 2L) {
      Mockito.when(debtPositionTypeEntityControllerApiMock.crudGetDebtpositiontype(String.valueOf(debtPositionTypeId)))
        .thenThrow(HttpClientErrorException.NotFound.class);
      expectedResult = null;
    } else {
      expectedResult = new DebtPositionType();
      Mockito.when(debtPositionTypeEntityControllerApiMock.crudGetDebtpositiontype(String.valueOf(debtPositionTypeId))).thenReturn(expectedResult);
    }

    //when
    DebtPositionType result = client.getDebtPositionTypeById(debtPositionTypeId, accessToken);

    //then
    Assertions.assertEquals(expectedResult, result);
  }

  @Test
  void whenGetInstallmentsByOrganizationIdAndNavThenInvokeApi() {
    // Given
    String accessToken = "ACCESSTOKEN";
    List<InstallmentDTO> expectedResult = List.of(new InstallmentDTO());

    Mockito.when(apisHolderMock.getInstallmentApi(accessToken))
      .thenReturn(installmentApiMock);
    Mockito.when(installmentApiMock.getInstallmentsByOrganizationIdAndNav(1L, "NAV", null)).thenReturn(expectedResult);

    //when
    List<InstallmentDTO> result = client.getInstallmentsByOrganizationIdAndNav(1L, "NAV", null, accessToken);

    //then
    Assertions.assertEquals(expectedResult, result);
  }

  @Test
  void whenCreateDebtPositionThenInvokeApi() {
    // Given
    String accessToken = "ACCESSTOKEN";
    DebtPositionDTO debtPositionDTO = new DebtPositionDTO();
    ResponseEntity<DebtPositionDTO> expectedResponse = ResponseEntity.ok(debtPositionDTO);

    Mockito.when(apisHolderMock.getDebtPositionApi(accessToken))
      .thenReturn(debtPositionApiMock);
    Mockito.when(debtPositionApiMock.createDebtPositionWithHttpInfo(debtPositionDTO, false))
      .thenReturn(expectedResponse);

    // When
    ResponseEntity<DebtPositionDTO> result = client.createDebtPosition(debtPositionDTO, accessToken);

    // Then
    Assertions.assertEquals(expectedResponse, result);
  }

  @ParameterizedTest
  @ValueSource(longs = {1L, 2L})
  void whenGetDebtPositionByInstallmentIdThenInvokeApi(Long installmentId) {
    // Given
    String accessToken = "ACCESSTOKEN";
    DebtPositionDTO expectedResult;

    Mockito.when(apisHolderMock.getDebtPositionApi(accessToken))
      .thenReturn(debtPositionApiMock);
    if(installmentId == 2L) {
      Mockito.when(debtPositionApiMock.getDebtPositionByInstallmentId(installmentId))
        .thenThrow(HttpClientErrorException.NotFound.class);
      expectedResult = null;
    } else {
      expectedResult = new DebtPositionDTO();
      Mockito.when(debtPositionApiMock.getDebtPositionByInstallmentId(installmentId)).thenReturn(expectedResult);
    }

    // When
    DebtPositionDTO result = client.getDebtPositionByInstallmentId(installmentId, accessToken);

    // Then
    Assertions.assertEquals(expectedResult, result);
  }

  @Test
  void whenGetDebtPositionsByOrganizationIdAndIuvThenInvokeApi() {
    // Given
    String accessToken = "ACCESSTOKEN";
    Long organizationId = 1L;
    String iuv = "IUV";
    List<DebtPositionOrigin> debtPositionOrigin = List.of(DebtPositionOrigin.ORDINARY);
    List<DebtPositionDTO> expectedResult = List.of(new DebtPositionDTO());

    Mockito.when(apisHolderMock.getDebtPositionApi(accessToken))
      .thenReturn(debtPositionApiMock);
    Mockito.when(debtPositionApiMock.getDebtPositionsByOrganizationIdAndIuv(organizationId, iuv, debtPositionOrigin))
      .thenReturn(expectedResult);

    // When
    List<DebtPositionDTO> result = client.getDebtPositionsByOrganizationIdAndIuv(organizationId, iuv, debtPositionOrigin, accessToken);

    // Then
    Assertions.assertEquals(expectedResult, result);
  }

  @Test
  void whenGetDebtPositionsByOrganizationIdAndIudThenInvokeApi() {
    // Given
    String accessToken = "ACCESSTOKEN";
    Long organizationId = 1L;
    String iud = "IUD";
    List<DebtPositionOrigin> debtPositionOrigin = List.of(DebtPositionOrigin.ORDINARY);
    List<DebtPositionDTO> expectedResult = List.of(new DebtPositionDTO());

    Mockito.when(apisHolderMock.getDebtPositionApi(accessToken))
      .thenReturn(debtPositionApiMock);
    Mockito.when(debtPositionApiMock.getDebtPositionsByOrganizationIdAndIud(organizationId, iud, debtPositionOrigin))
      .thenReturn(expectedResult);

    // When
    List<DebtPositionDTO> result = client.getDebtPositionsByOrganizationIdAndIud(organizationId, iud, debtPositionOrigin, accessToken);

    // Then
    Assertions.assertEquals(expectedResult, result);
  }

  @ParameterizedTest
  @ValueSource(longs = {1L, 2L})
  void whenGetReceiptByIdThenInvokeApi(Long receiptId) {
    // Given
    String accessToken = "ACCESSTOKEN";
    ReceiptDTO expectedResult;

    Mockito.when(apisHolderMock.getReceiptApi(accessToken))
      .thenReturn(receiptApiMock);
    if(receiptId == 2L) {
      Mockito.when(receiptApiMock.getReceipt(receiptId))
        .thenThrow(HttpClientErrorException.NotFound.class);
      expectedResult = null;
    } else {
      expectedResult = new ReceiptDTO();
      Mockito.when(receiptApiMock.getReceipt(receiptId)).thenReturn(expectedResult);
    }

    // When
    ReceiptDTO result = client.getReceiptById(receiptId, accessToken);

    // Then
    Assertions.assertEquals(expectedResult, result);
  }

  @Test
  void whenFindAuthorizedByTransferSemanticKeyThenInvokeApi() {
    // Given
    String accessToken = "ACCESSTOKEN";
    Long organizationId = 1L;
    String iuv = "IUV";
    String iur = "IUR";
    int transferIndex = 1;
    String operatorExternalUserId = "OPERATOR_ID";

    InstallmentNoPII expectedResult = new InstallmentNoPII();

    Mockito.when(apisHolderMock.getInstallmentNoPiiSearchControllerApi(accessToken))
      .thenReturn(installmentNoPiiSearchControllerApiMock);
    Mockito.when(installmentNoPiiSearchControllerApiMock.crudInstallmentsFindAuthorizedByTransferSemanticKey(
        organizationId, iuv, iur, String.valueOf(transferIndex), operatorExternalUserId))
      .thenReturn(expectedResult);

    // When
    InstallmentNoPII result = client.findAuthorizedByTransferSemanticKey(
        organizationId, iuv, iur, transferIndex, operatorExternalUserId, accessToken);

    // Then
    Assertions.assertEquals(expectedResult, result);
  }
}
