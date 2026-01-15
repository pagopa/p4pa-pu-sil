package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.controller.generated.DebtPositionTypeEntityControllerApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.DebtPositionTypeOrgSearchControllerApi;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionType;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
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
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class DebtPositionTypeClientTest {

  @Mock
  private DebtPositionsApisHolder apisHolderMock;
  @Mock
  private DebtPositionTypeOrgSearchControllerApi debtPositionTypeOrgSearchControllerApiMock;
  @Mock
  private DebtPositionTypeEntityControllerApi debtPositionTypeEntityControllerApiMock;

  private DebtPositionTypeClient client;

  @BeforeEach
  void setUp() {
    client = new DebtPositionTypeClient(apisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      apisHolderMock,
      debtPositionTypeOrgSearchControllerApiMock,
      debtPositionTypeEntityControllerApiMock
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

    Assertions.assertThrows(HttpClientErrorException.NotFound.class, () -> {
      client.getDebtPositionTypeOrgByOrganizationIdAndCode(debtPositionTypeOrgId, debtPositionTypeOrgCode, accessToken);
    });
  }

  @ParameterizedTest
  @ValueSource(longs = {1L})
  void whenGetDebtPositionTypeByIdThenInvokeApi(Long debtPositionTypeId) {
    // Given
    String accessToken = "ACCESSTOKEN";
    DebtPositionType expectedResult;

    Mockito.when(apisHolderMock.getDebtPositionTypeEntityControllerApi(accessToken))
      .thenReturn(debtPositionTypeEntityControllerApiMock);

    expectedResult = new DebtPositionType();
      Mockito.when(debtPositionTypeEntityControllerApiMock.crudGetDebtpositiontype(String.valueOf(debtPositionTypeId))).thenReturn(expectedResult);

    //when
    DebtPositionType result = client.getDebtPositionTypeById(debtPositionTypeId, accessToken);

    //then
    Assertions.assertEquals(expectedResult, result);
  }

  @ParameterizedTest
  @ValueSource(longs = {1L})
  void whenGetDebtPositionTypeOrgByIdThenInvokeApi(Long installmentId) {
    // Given
    String accessToken = "ACCESSTOKEN";
    DebtPositionTypeOrg expectedResult;

    Mockito.when(apisHolderMock.getDebtPositionTypeOrgSearchControllerApi(accessToken))
      .thenReturn(debtPositionTypeOrgSearchControllerApiMock);

    expectedResult = new DebtPositionTypeOrg();
      Mockito.when(debtPositionTypeOrgSearchControllerApiMock.crudDebtPositionTypeOrgsGetDebtPositionTypeOrgByInstallmentId(installmentId)).thenReturn(expectedResult);

    //when
    DebtPositionTypeOrg result = client.getDebtPositionTypeOrgByInstallmentId(installmentId, accessToken);

    //then
    Assertions.assertEquals(expectedResult, result);
  }

}
