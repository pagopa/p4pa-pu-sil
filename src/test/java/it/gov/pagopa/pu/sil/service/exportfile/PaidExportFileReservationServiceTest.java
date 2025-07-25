package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.dto.generated.PaidExportFileFilter;
import it.gov.pagopa.pu.sil.dto.generated.PaidExportRequestDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaidExportFileReservationServiceTest {

  @Mock
  private ExportFileService exportFileServiceMock;
  @Mock
  private DebtPositionTypeService debtPositionTypeServiceMock;

  private PaidExportFileReservationService service;

  @BeforeEach
  void setUp() {
    service = new PaidExportFileReservationService(
      exportFileServiceMock,
      debtPositionTypeServiceMock
    );
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(
      exportFileServiceMock,
      debtPositionTypeServiceMock
    );
  }

  @Test
  void givenUserNotAdminWhenDoReservationThenKo() {
    // Given
    String orgIpaCode = "ORG1";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", "OTHERIPACODE");
    userInfo.setUserId("userId1");
    String accessToken = "token1";

    PaidExportRequestDTO paidExportRequestDTO = new PaidExportRequestDTO();
    // When, Then
    assertThrows(AuthorizationDeniedException.class, () ->
      service.doReservation(
        userInfo,
        accessToken,
        orgIpaCode,
        paidExportRequestDTO)
    );
  }

  @Test
  void givenUserIsAdminWhenDoReservationThenOk() {
    // Given
    String orgIpaCode = "ORG2";
    Long organizationId = 123L;
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", orgIpaCode);
    userInfo.setUserId("admin2");
    String accessToken = "token2";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";

    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg().flagActive(true).debtPositionTypeOrgId(1L);

    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
      .thenReturn(debtPositionTypeOrg);
    when(exportFileServiceMock.createPaidExportFile(any(), eq(accessToken)))
      .thenReturn(456L);

    PaidExportRequestDTO paidExportRequestDTO = new PaidExportRequestDTO()
      .fileVersion(fileVersion)
      .exportFilters(new PaidExportFileFilter()
        .installmentUpdateDateTime(new OffsetDateTimeIntervalFilter()
          .from(from)
          .to(to))
        .debtPositionTypeOrgCode(debtPositionTypeOrgCode));
    // When
    Long result = service.doReservation(
      userInfo,
      accessToken,
      orgIpaCode,
      paidExportRequestDTO
    );

    // Then
    assertNotNull(result);
    assertEquals(456L, result);
  }

  @Test
  void givenUserIsAdminAndDebtPositionInvalidWhenDoReservationThenException() {
    // Given
    String orgIpaCode = "ORG4";
    Long organizationId = 123L;
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", orgIpaCode);
    userInfo.setUserId("admin4");
    String accessToken = "token4";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();

    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
      .thenReturn(null);

    PaidExportRequestDTO paidExportRequestDTO = new PaidExportRequestDTO()
      .fileVersion(fileVersion)
      .exportFilters(new PaidExportFileFilter()
        .installmentUpdateDateTime(new OffsetDateTimeIntervalFilter()
          .from(from)
          .to(to)));
    // When
    ExportFileServiceException exception = assertThrows(ExportFileServiceException.class, () ->
      service.doReservation(
        userInfo,
        accessToken,
        orgIpaCode,
        paidExportRequestDTO
      )
    );

    //Then
    assertEquals(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, exception.getFault());
  }

  @Test
  void givenUserIsAdminAndInvalidDateIntervalExceptionWhenDoReservationThenException() {
    // Given
    String orgIpaCode = "ORG5";
    Long organizationId = 123L;
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", orgIpaCode);
    userInfo.setUserId("admin5");
    String accessToken = "token5";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";

    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg().flagActive(true).debtPositionTypeOrgId(3L);

    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
      .thenReturn(debtPositionTypeOrg);
    ExportFileClientException errorException = new ExportFileClientException(
      ProcessExecutionsErrorDTO.CodeEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE, "");
    when(exportFileServiceMock.createPaidExportFile(any(), eq(accessToken)))
      .thenThrow(errorException);

    // When
    ExportFileClientException exception = assertThrows(ExportFileClientException.class, () ->
      service.doReservation(
        userInfo,
        accessToken,
        orgIpaCode,
        new PaidExportRequestDTO()
          .fileVersion(fileVersion)
          .exportFilters(new PaidExportFileFilter()
            .installmentUpdateDateTime(new OffsetDateTimeIntervalFilter()
              .from(from)
              .to(to))
            .debtPositionTypeOrgCode(debtPositionTypeOrgCode))
      )
    );

    // Then
    assertEquals(ProcessExecutionsErrorDTO.CodeEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE, exception.getCode());
  }
}
