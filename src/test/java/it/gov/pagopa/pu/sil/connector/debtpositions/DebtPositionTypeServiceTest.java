package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionType;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.connector.debtpositions.client.DebtPositionTypeClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DebtPositionTypeServiceTest {

  @Mock
  private DebtPositionTypeClient clientMock;

  private DebtPositionTypeService service;

  @BeforeEach
  void init() {
    service = new DebtPositionTypeServiceImpl(clientMock);
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
  void whenGetDebtPositionTypeByIdThenReturnDebtPositionType() {
    // Given
    String accessToken = "ACCESSTOKEN";
    Long debtPositionTypeId = 1L;
    DebtPositionType expectedResult = new DebtPositionType();

    Mockito.when(clientMock.getDebtPositionTypeById(debtPositionTypeId, accessToken)).thenReturn(expectedResult);

    //when
    DebtPositionType result = service.getDebtPositionTypeById(debtPositionTypeId, accessToken);

    //then
    Assertions.assertSame(expectedResult, result);
  }

}
