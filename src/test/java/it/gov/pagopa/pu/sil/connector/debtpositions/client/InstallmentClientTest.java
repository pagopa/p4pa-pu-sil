package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.controller.generated.InstallmentApi;
import it.gov.pagopa.pu.debtpositions.controller.generated.InstallmentNoPiiSearchControllerApi;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentNoPII;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApisHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class InstallmentClientTest {

  @Mock
  private DebtPositionsApisHolder apisHolderMock;
  @Mock
  private InstallmentApi installmentApiMock;
  @Mock
  private InstallmentNoPiiSearchControllerApi installmentNoPiiSearchControllerApiMock;

  private InstallmentClient client;

  @BeforeEach
  void setUp() {
    client = new InstallmentClient(apisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      apisHolderMock,
      installmentApiMock,
      installmentNoPiiSearchControllerApiMock
      );
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

  @Test
  void whenFindAuthorizedByTransferSemanticKeyThenNotFound(){
    //Given
    String accessToken = "ACCESSTOKEN";
    Long organizationId = 1L;
    String iuv = "IUV";
    String iur = "IUR";
    int transferIndex = 1;
    String operatorExternalUserId = "OPERATOR_ID";

    Mockito.when(apisHolderMock.getInstallmentNoPiiSearchControllerApi(accessToken))
      .thenReturn(installmentNoPiiSearchControllerApiMock);
    Mockito.when(installmentNoPiiSearchControllerApiMock.crudInstallmentsFindAuthorizedByTransferSemanticKey(
      organizationId, iuv, iur, String.valueOf(transferIndex), operatorExternalUserId))
      .thenThrow(HttpClientErrorException.NotFound.class);

    // When
    InstallmentNoPII result = client.findAuthorizedByTransferSemanticKey(
      organizationId, iuv, iur, transferIndex, operatorExternalUserId, accessToken);

    // Then
    Assertions.assertNull(result);
  }

}
