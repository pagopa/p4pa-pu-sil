package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.controller.generated.DebtPositionApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.DebtPositionSearchControllerApi;
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

import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class DebtPositionClientTest {

  @Mock
  private DebtPositionsApisHolder apisHolderMock;
  @Mock
  private DebtPositionApi debtPositionApiMock;
  @Mock
  private DebtPositionSearchControllerApi debtPositionSearchControllerApiMock;


  private DebtPositionClient client;

  @BeforeEach
  void setUp() {
    client = new DebtPositionClient(apisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      apisHolderMock,
      debtPositionApiMock,
      debtPositionSearchControllerApiMock
      );
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
  @ValueSource(longs = {1L})
  void whenGetDebtPositionDTOByInstallmentIdThenInvokeApi(Long installmentId) {
    // Given
    String accessToken = "ACCESSTOKEN";
    DebtPositionDTO expectedResult;

    Mockito.when(apisHolderMock.getDebtPositionApi(accessToken))
      .thenReturn(debtPositionApiMock);

    expectedResult = new DebtPositionDTO();
      Mockito.when(debtPositionApiMock.getDebtPositionByInstallmentId(installmentId)).thenReturn(expectedResult);

    // When
    DebtPositionDTO result = client.getDebtPositionDTOByInstallmentId(installmentId, accessToken);

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

  @Test
  void whenManageDebtPositionInstallmentsThenInvokeApi() {
    // Given
    String accessToken = "ACCESSTOKEN";
    Long debtPositionId = 1L;
    DebtPositionDTO expectedResult = new DebtPositionDTO();
    ManageDebtPositionDTO manageDebtPositionDTO = new ManageDebtPositionDTO();

    Mockito.when(apisHolderMock.getDebtPositionApi(accessToken))
      .thenReturn(debtPositionApiMock);
    Mockito.when(debtPositionApiMock.manageDebtPositionInstallmentsWithHttpInfo(debtPositionId, manageDebtPositionDTO))
      .thenReturn(ResponseEntity.ok(expectedResult));

    // When
    ResponseEntity<DebtPositionDTO> result = client.manageDebtPositionInstallments(debtPositionId, manageDebtPositionDTO, accessToken);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertEquals(expectedResult, result.getBody());
  }

  @ParameterizedTest
  @ValueSource(longs = {1L})
  void whenGetDebtPositionByInstallmentIdThenInvokeApi(Long installmentId) {
    // Given
    String accessToken = "ACCESSTOKEN";
    DebtPosition expectedResult;

    Mockito.when(apisHolderMock.getDebtPositionSearchControllerApi(accessToken))
      .thenReturn(debtPositionSearchControllerApiMock);

    expectedResult = new DebtPosition();
      Mockito.when(debtPositionSearchControllerApiMock.crudDebtPositionsFindByInstallmentId(installmentId)).thenReturn(expectedResult);


    // When
    DebtPosition result = client.getDebtPositionByInstallmentId(installmentId, accessToken);

    // Then
    Assertions.assertEquals(expectedResult, result);
  }

  @Test
  void whenCreateMixedDebtPositionThenInvokeApi() {
    // Given
    String accessToken = "ACCESSTOKEN";
    DebtPositionDTO debtPositionDTO = new DebtPositionDTO();
    MixedDebtPositionDTO mixedDebtPositionDTO = new MixedDebtPositionDTO();
    ResponseEntity<DebtPositionDTO> expectedResponse = ResponseEntity.ok(debtPositionDTO);

    Mockito.when(apisHolderMock.getDebtPositionApi(accessToken))
      .thenReturn(debtPositionApiMock);
    Mockito.when(debtPositionApiMock.createMixedDebtPositionWithHttpInfo(mixedDebtPositionDTO))
      .thenReturn(expectedResponse);

    // When
    ResponseEntity<DebtPositionDTO> result = client.createMixedDebtPosition(mixedDebtPositionDTO, accessToken);

    // Then
    Assertions.assertEquals(expectedResponse, result);
  }

  @Test
  void whenGetDebtPositionsByDebtorFiscalCodeAndDebtorEntityTypeThenInvokeApi() {
    // Given
    String accessToken = "ACCESSTOKEN";
    String debtorFiscalCode = "12345678901";
    PersonEntityType debtorEntityType = PersonEntityType.F;
    List<String> debtPositionTypeOrgCodesToExclude = Collections.emptyList();
    List<DebtPositionDTO> expectedResult = List.of(new DebtPositionDTO());
    List<Long> organizationIds = List.of(1L);

    Mockito.when(apisHolderMock.getDebtPositionApi(accessToken))
      .thenReturn(debtPositionApiMock);
    Mockito.when(debtPositionApiMock.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(debtorFiscalCode, debtorEntityType, organizationIds, List.of(InstallmentStatus.UNPAID), null, debtPositionTypeOrgCodesToExclude, null, null))
      .thenReturn(expectedResult);

    // When
    List<DebtPositionDTO> result = client.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(debtorFiscalCode, debtorEntityType, organizationIds, debtPositionTypeOrgCodesToExclude, InstallmentStatus.UNPAID, null, null, accessToken);

    // Then
    Assertions.assertEquals(expectedResult, result);
  }
}
