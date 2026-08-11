package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.client.generated.InstallmentApi;
import it.gov.pagopa.pu.debtpositions.client.generated.InstallmentNoPiiSearchControllerApi;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentNoPII;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApisHolder;
import it.gov.pagopa.pu.sil.exception.common.RestInvokeNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static it.gov.pagopa.pu.sil.util.Constants.ORDINARY_DEBT_POSITION_ORIGINS;
import static org.mockito.Mockito.when;

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

      when(apisHolderMock.getInstallmentNoPiiSearchControllerApi(accessToken))
          .thenReturn(installmentNoPiiSearchControllerApiMock);
      when(installmentNoPiiSearchControllerApiMock.crudInstallmentsIsInstallmentExists(organizationId, iud, iuv, nav, ORDINARY_DEBT_POSITION_ORIGINS))
          .thenReturn(Boolean.TRUE);

      // When
      Boolean result = client.isInstallmentExistsByIudIuvNav(organizationId, iud, iuv, nav, ORDINARY_DEBT_POSITION_ORIGINS, accessToken);

      // Then
      Assertions.assertEquals(Boolean.TRUE, result);
  }

  @Test
  void whenGetInstallmentsByOrganizationIdAndNavThenInvokeApi() {
    // Given
    String accessToken = "ACCESSTOKEN";
    List<InstallmentDTO> expectedResult = List.of(new InstallmentDTO());

    when(apisHolderMock.getInstallmentApi(accessToken))
      .thenReturn(installmentApiMock);
    when(installmentApiMock.getInstallmentsByOrganizationIdAndNav(1L, "NAV", null)).thenReturn(expectedResult);

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

    when(apisHolderMock.getInstallmentNoPiiSearchControllerApi(accessToken))
      .thenReturn(installmentNoPiiSearchControllerApiMock);
    when(installmentNoPiiSearchControllerApiMock.crudInstallmentsFindAuthorizedByTransferSemanticKey(
        organizationId, iuv, iur, transferIndex, operatorExternalUserId, ORDINARY_DEBT_POSITION_ORIGINS))
      .thenReturn(expectedResult);

    // When
    InstallmentNoPII result = client.findAuthorizedByTransferSemanticKey(
        organizationId, iuv, iur, transferIndex, operatorExternalUserId, ORDINARY_DEBT_POSITION_ORIGINS, accessToken);

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

    when(apisHolderMock.getInstallmentNoPiiSearchControllerApi(accessToken))
      .thenReturn(installmentNoPiiSearchControllerApiMock);
    when(installmentNoPiiSearchControllerApiMock.crudInstallmentsFindAuthorizedByTransferSemanticKey(
      organizationId, iuv, iur, transferIndex, operatorExternalUserId, ORDINARY_DEBT_POSITION_ORIGINS))
      .thenThrow(new RestInvokeNotFoundException("APPNAME", HttpStatus.NOT_FOUND, "ERROR", "ERRORCODE", "ERRORMESSAGE"));

    // When
    InstallmentNoPII result = client.findAuthorizedByTransferSemanticKey(
      organizationId, iuv, iur, transferIndex, operatorExternalUserId, ORDINARY_DEBT_POSITION_ORIGINS, accessToken);

    // Then
    Assertions.assertNull(result);
  }

}
