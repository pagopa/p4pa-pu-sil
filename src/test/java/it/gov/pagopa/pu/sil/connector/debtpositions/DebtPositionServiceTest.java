package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionType;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.client.DebtPositionClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class DebtPositionServiceTest {

  @Mock
  private DebtPositionClient clientMock;

  private DebtPositionService service;

  @BeforeEach
  void init(){
    service = new DebtPositionServiceImpl(clientMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(clientMock);
  }

  @Test
  void whenGetDebtPositionTypeOrgByIdThenInvokeClient(){
    // Given
    Long debtPositionTypeOrgId = 1L;
    String debtPositionTypeOrgCode = "CODE";
    String accessToken = "ACCESSTOKEN";
    DebtPositionTypeOrg expectedResult = new DebtPositionTypeOrg();

    Mockito.when(clientMock.getDebtPositionTypeOrgByOrganizationIdAndCode(Mockito.same(debtPositionTypeOrgId), Mockito.same(debtPositionTypeOrgCode) ,Mockito.same(accessToken)))
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

}
