package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.mapper.ClassificationsExportFileRequestMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILPrenotaExportFlussoRiconciliazione;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILPrenotaExportFlussoRiconciliazioneRisposta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PivotSILPrenotaExportFlussoRiconciliazioneServiceTest {
  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @Mock
  private ExportFileService exportFileServiceMock;
  @Mock
  private DebtPositionService debtPositionServiceMock;

  private ClassificationsExportFileRequestMapper classificationsExportFileRequestMapper;

  private PivotSILPrenotaExportFlussoRiconciliazioneService service;

  @BeforeEach
  void setUp() {
    classificationsExportFileRequestMapper = new ClassificationsExportFileRequestMapper();
    service = new PivotSILPrenotaExportFlussoRiconciliazioneService(exportFileServiceMock, debtPositionServiceMock, classificationsExportFileRequestMapper);
  }

  @Test
  void givenUserNotAdminWhenDoReservationThenKo() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("userId1");
    String orgIpaCode = "ORG1";
    String accessToken = "token1";
    PivotSILPrenotaExportFlussoRiconciliazione request = podamFactory.manufacturePojo(PivotSILPrenotaExportFlussoRiconciliazione.class);
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);

      assertThrows(UnauthorizedException.class, () ->
        service.doReservation(
          userInfo,
          accessToken,
          orgIpaCode,
          request
        )
      );
      verifyNoInteractions(
        exportFileServiceMock,
        debtPositionServiceMock);
    }
  }

  @Test
  void givenUserAdminWhenDoReservationThenOk() throws DatatypeConfigurationException {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("userId1");
    String orgIpaCode = "ORG1";
    String accessToken = "token1";
    Long organizationId = 123L;

    GregorianCalendar fromCal = new GregorianCalendar(2023, 0, 1); // Jan 1, 2023
    GregorianCalendar toCal = new GregorianCalendar(2023, 11, 31); // Dec 31, 2023
    XMLGregorianCalendar fromXml = DatatypeFactory.newInstance().newXMLGregorianCalendar(fromCal);
    XMLGregorianCalendar toXml = DatatypeFactory.newInstance().newXMLGregorianCalendar(toCal);
    PivotSILPrenotaExportFlussoRiconciliazione request = podamFactory.manufacturePojo(PivotSILPrenotaExportFlussoRiconciliazione.class);
    request.setImportoTesoreria("100,00");
    request.setDataUltimoAggiornamentoDa(fromXml);
    request.setDataUltimoAggiornamentoA(toXml);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(organizationId);

      ClassificationsExportFileRequestDTO classificationsExportFileRequestDTO = classificationsExportFileRequestMapper.mapToExportFileRequest(organizationId, request);
      when(exportFileServiceMock.createClassificationsExportFile(classificationsExportFileRequestDTO, accessToken)).thenReturn(456L);
      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(new DebtPositionTypeOrg().flagActive(true));


      PivotSILPrenotaExportFlussoRiconciliazioneRisposta result = service.doReservation(
        userInfo,
        accessToken,
        orgIpaCode,
        request
      );

      assertNotNull(result);
      assertEquals(String.valueOf(456L), result.getRequestToken());
      assertEquals(request.getDataUltimoAggiornamentoA(), result.getDataA());
      verify(exportFileServiceMock).createClassificationsExportFile(any(), eq(accessToken));
    }
  }

  @Test
  void givenUserIsAdminAndDebtPositionInvalidWhenDoReservationThenException() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin4");
    String orgIpaCode = "ORG4";
    Long organizationId = 123L;
    String accessToken = "token4";
    PivotSILPrenotaExportFlussoRiconciliazione request = podamFactory.manufacturePojo(PivotSILPrenotaExportFlussoRiconciliazione.class);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(organizationId);

      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(null);

      ExportFileServiceException exception = assertThrows(ExportFileServiceException.class, () ->
        service.doReservation(
          userInfo,
          accessToken,
          orgIpaCode,
          request
        )
      );

      assertEquals(SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, exception.getFault());

      verifyNoInteractions(exportFileServiceMock);
    }
  }

  @Test
  void givenUserIsAdminAndDebtPositionNotEnabledWhenDoReservationThenException() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin4");
    String orgIpaCode = "ORG4";
    Long organizationId = 123L;
    String accessToken = "token4";
    PivotSILPrenotaExportFlussoRiconciliazione request = podamFactory.manufacturePojo(PivotSILPrenotaExportFlussoRiconciliazione.class);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(organizationId);

      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(new DebtPositionTypeOrg().flagActive(false));

      ExportFileServiceException exception = assertThrows(ExportFileServiceException.class, () ->
        service.doReservation(
          userInfo,
          accessToken,
          orgIpaCode,
          request
        )
      );

      assertEquals(SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO, exception.getFault());

      verifyNoInteractions(exportFileServiceMock);
    }
  }

  @Test
  void givenUserIsAdminAndInvalidDateIntervalExceptionWhenDoReservationThenException() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin5");
    String orgIpaCode = "ORG5";
    Long organizationId = 123L;
    String accessToken = "token5";
    PivotSILPrenotaExportFlussoRiconciliazione request = podamFactory.manufacturePojo(PivotSILPrenotaExportFlussoRiconciliazione.class);
    request.setImportoTesoreria("100,00");

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(organizationId);

      ExportFileClientException errorException = new ExportFileClientException(
        ProcessExecutionsErrorDTO.CodeEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE, "error");

      ClassificationsExportFileRequestDTO classificationsExportFileRequestDTO = classificationsExportFileRequestMapper.mapToExportFileRequest(organizationId, request);
      when(exportFileServiceMock.createClassificationsExportFile(classificationsExportFileRequestDTO, accessToken))
        .thenThrow(errorException);
      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(new DebtPositionTypeOrg().flagActive(true));

      ExportFileClientException exception = assertThrows(ExportFileClientException.class, () ->
        service.doReservation(
          userInfo,
          accessToken,
          orgIpaCode,
          request
        )
      );

      assertEquals(ProcessExecutionsErrorDTO.CodeEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE, exception.getCode());
    }
  }
}
