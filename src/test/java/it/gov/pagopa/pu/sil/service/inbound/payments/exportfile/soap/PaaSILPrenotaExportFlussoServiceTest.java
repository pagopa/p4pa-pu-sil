package it.gov.pagopa.pu.sil.service.inbound.payments.exportfile.soap;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileFilter.DebtPositionOriginsEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO.ExportFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.exception.common.InvalidValueException;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILPrenotaExportFlusso;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import uk.co.jemos.podam.api.PodamFactory;

import java.time.OffsetDateTime;
import java.util.List;

import static it.gov.pagopa.pu.sil.service.inbound.payments.exportfile.AbstractExportFileReservationService.IDENTIFICATIVO_TIPO_DOVUTO_SECONDARIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaaSILPrenotaExportFlussoServiceTest {

  @Mock
  private ExportFileService exportFileServiceMock;
  @Mock
  private DebtPositionTypeService debtPositionTypeServiceMock;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  private PaaSILPrenotaExportFlussoService service;

  @BeforeEach
  void setUp() {
    service = new PaaSILPrenotaExportFlussoService(
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
  void givenUserNotAdminWhenPaaSILPrenotaExportFlussoThenKo() {
    String orgIpaCode = "ORG1";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", "OTHERIPACODE");
    userInfo.setUserId("userId1");
    String accessToken = "token1";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";

    assertThrows(AuthorizationDeniedException.class, () ->
      service.paaSILPrenotaExportFlusso(
        userInfo,
        accessToken,
        orgIpaCode,
        fileVersion,
        from,
        to,
        debtPositionTypeOrgCode
      )
    );
    verifyNoInteractions(
      exportFileServiceMock,
      debtPositionTypeServiceMock);
  }

  @Test
  void givenUserIsAdminWhenPaaSILPrenotaExportFlussoThenOk() {
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

    PaidExportFileRequestDTO expectedMappedRequestDto = new PaidExportFileRequestDTO()
      .organizationId(organizationId)
      .exportFileType(ExportFileTypeEnum.PAID)
      .fileVersion(fileVersion)
      .filterFields(new it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileFilter()
        .paymentDateTime(new OffsetDateTimeIntervalFilter().from(from).to(to))
        .debtPositionTypeOrgId(debtPositionTypeOrg.getDebtPositionTypeOrgId())
        .debtPositionOrigins(
          List.of(DebtPositionOriginsEnum.ORDINARY,
            DebtPositionOriginsEnum.ORDINARY_SIL,
            DebtPositionOriginsEnum.SPONTANEOUS,
            DebtPositionOriginsEnum.SPONTANEOUS_SIL,
            DebtPositionOriginsEnum.SPONTANEOUS_MIXED,
            DebtPositionOriginsEnum.SPONTANEOUS_PSP,
            DebtPositionOriginsEnum.RECEIPT_FILE,
            DebtPositionOriginsEnum.RECEIPT_PAGOPA,
            DebtPositionOriginsEnum.REPORTING_PAGOPA)));
    when(exportFileServiceMock.createPaidExportFile(expectedMappedRequestDto, accessToken)).thenReturn(456L);


    PaaSILPrenotaExportFlusso request = podamFactory.manufacturePojoWithFullData(PaaSILPrenotaExportFlusso.class);
    request.setIdentificativoTipoDovuto(String.valueOf(1L));

    Long result = service.paaSILPrenotaExportFlusso(
      userInfo,
      accessToken,
      orgIpaCode,
      fileVersion,
      from,
      to,
      debtPositionTypeOrgCode
    );

    assertNotNull(result);
    assertEquals(456L, result);
  }

  @Test
  void givenSecondaryOrgWhenPaaSILPrenotaExportFlussoThenOk() {
    String orgIpaCode = "ORG2";
    Long organizationId = 123L;
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", orgIpaCode);
    userInfo.setUserId("admin2");
    String accessToken = "token2";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();

    PaidExportFileRequestDTO expectedMappedRequestDto = new PaidExportFileRequestDTO()
      .organizationId(organizationId)
      .exportFileType(ExportFileTypeEnum.PAID)
      .fileVersion(fileVersion)
      .filterFields(new it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileFilter()
        .paymentDateTime(new OffsetDateTimeIntervalFilter().from(from).to(to))
        .debtPositionTypeOrgId(null)
        .debtPositionOrigins(
          List.of(DebtPositionOriginsEnum.SECONDARY_ORG)));
    when(exportFileServiceMock.createPaidExportFile(expectedMappedRequestDto, accessToken)).thenReturn(456L);

    PaaSILPrenotaExportFlusso request = podamFactory.manufacturePojoWithFullData(PaaSILPrenotaExportFlusso.class);
    request.setIdentificativoTipoDovuto(String.valueOf(1L));

    Long result = service.paaSILPrenotaExportFlusso(
      userInfo,
      accessToken,
      orgIpaCode,
      fileVersion,
      from,
      to,
      IDENTIFICATIVO_TIPO_DOVUTO_SECONDARIO
    );

    assertNotNull(result);
    assertEquals(456L, result);

    verify(debtPositionTypeServiceMock, never()).getDebtPositionTypeOrgByOrgIdAndType(any(), any(), any());
  }

  @Test
  void givenUserIsAdminAndDebtPositionInvalidWhenPaaSILPrenotaExportFlussoThenException() {
    String orgIpaCode = "ORG4";
    Long organizationId = 123L;
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", orgIpaCode);
    userInfo.setUserId("admin4");
    String accessToken = "token4";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";

    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
      .thenReturn(null);

    ExportFileServiceException exception = assertThrows(ExportFileServiceException.class, () ->
      service.paaSILPrenotaExportFlusso(
        userInfo,
        accessToken,
        orgIpaCode,
        fileVersion,
        from,
        to,
        debtPositionTypeOrgCode
      )
    );

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_DEBT_POSITION_TYPE_ORG_CODE, exception.getCode());
  }

  @Test
  void givenUserIsAdminAndInvalidDateIntervalExceptionWhenPaaSILPrenotaExportFlussoThenException() {
    String orgIpaCode = "ORG5";
    Long organizationId = 123L;
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", orgIpaCode);
    userInfo.setUserId("admin5");
    String accessToken = "token5";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";

    ExportFileClientException errorException = new ExportFileClientException(new InvalidValueException(
      ProcessExecutionsErrorDTO.CategoryEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE.getValue(), "")
    );

    when(exportFileServiceMock.createPaidExportFile(any(), eq(accessToken)))
      .thenThrow(errorException);
    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
      .thenReturn(new DebtPositionTypeOrg().flagActive(true));

    ExportFileClientException exception = assertThrows(ExportFileClientException.class, () ->
      service.paaSILPrenotaExportFlusso(
        userInfo,
        accessToken,
        orgIpaCode,
        fileVersion,
        from,
        to,
        debtPositionTypeOrgCode
      )
    );

    assertEquals(ProcessExecutionsErrorDTO.CategoryEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE.getValue(), exception.getCode());
  }

}
