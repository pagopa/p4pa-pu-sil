package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.controller.generated.DebtPositionTypeOrgSearchControllerApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.InstallmentApi;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApisHolder;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class DebtPositionClientTest {

  @Mock
  private DebtPositionsApisHolder apisHolderMock;
  @Mock
  private DebtPositionTypeOrgSearchControllerApi debtPositionTypeOrgSearchControllerApiMock;
  @Mock
  private InstallmentApi installmentApiMock;


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
  void givenNotExistentTypeOrgWhenGetDebtPositionTypeOrgByOrganizationIdAndCodeThenException(){
    //Given
    String accessToken = "ACCESSTOKEN";
    long debtPositionTypeOrgId = 1L;
    String debtPositionTypeOrgCode = "CODE";

    Mockito.when(apisHolderMock.getDebtPositionTypeOrgSearchControllerApi(accessToken))
      .thenReturn(debtPositionTypeOrgSearchControllerApiMock);
    Mockito.when(debtPositionTypeOrgSearchControllerApiMock.crudDebtPositionTypeOrgsFindByOrganizationIdAndCode(debtPositionTypeOrgId, debtPositionTypeOrgCode))
      .thenThrow(HttpClientErrorException.NotFound.class);

    // Then
    Assertions.assertThrows(ApplicationException.class,
      () -> client.getDebtPositionTypeOrgByOrganizationIdAndCode(debtPositionTypeOrgId, debtPositionTypeOrgCode, accessToken));
  }

}
