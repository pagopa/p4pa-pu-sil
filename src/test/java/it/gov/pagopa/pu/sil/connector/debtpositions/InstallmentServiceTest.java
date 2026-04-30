package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentNoPII;
import it.gov.pagopa.pu.sil.connector.debtpositions.client.InstallmentClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static it.gov.pagopa.pu.sil.util.Constants.ORDINARY_DEBT_POSITION_ORIGINS;

@ExtendWith(MockitoExtension.class)
class InstallmentServiceTest {

  @Mock
  private InstallmentClient clientMock;

  private InstallmentService service;

  @BeforeEach
  void init() {
    service = new InstallmentServiceImpl(clientMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(clientMock);
  }

  @Test
  void whenCountExistingInstallmentsByIudIuvNavThenReturnCorrectCount() {
    // Given
    Long organizationId = 1L;
    String iud = "IUD";
    String iuv = "IUV";
    String nav = "NAV";
    String accessToken = "ACCESSTOKEN";

    Mockito.when(clientMock.isInstallmentExistsByIudIuvNav(
        Mockito.same(organizationId),
        Mockito.same(iud),
        Mockito.same(iuv),
        Mockito.same(nav),
        Mockito.same(ORDINARY_DEBT_POSITION_ORIGINS),
        Mockito.same(accessToken)))
      .thenReturn(Boolean.TRUE);

    // When
    Boolean result = service.isInstallmentExistsByIudIuvNav(organizationId, iud, iuv, nav, ORDINARY_DEBT_POSITION_ORIGINS, accessToken);

    // Then
    Assertions.assertSame(Boolean.TRUE, result);
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
    Assertions.assertSame(expectedResult, result);
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
        organizationId, iuv, iur, transferIndex, operatorExternalUserId, ORDINARY_DEBT_POSITION_ORIGINS, accessToken))
      .thenReturn(expectedResult);

    // When
    InstallmentNoPII result = service.findAuthorizedByTransferSemanticKey(
        organizationId, iuv, iur, transferIndex, operatorExternalUserId, ORDINARY_DEBT_POSITION_ORIGINS, accessToken);
    // Then
    Assertions.assertSame(expectedResult, result);
  }

}
