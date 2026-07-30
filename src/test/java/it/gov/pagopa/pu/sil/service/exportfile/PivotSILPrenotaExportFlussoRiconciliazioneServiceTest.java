package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.exception.InvalidValueException;
import it.gov.pagopa.pu.sil.mapper.ClassificationsExportFileRequestMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.pivot.ente.CodiceClassificazioneType;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILPrenotaExportFlussoRiconciliazione;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILPrenotaExportFlussoRiconciliazioneRisposta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import uk.co.jemos.podam.api.PodamFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PivotSILPrenotaExportFlussoRiconciliazioneServiceTest {
  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @Mock
  private ExportFileService exportFileServiceMock;
  @Mock
  private DebtPositionTypeService debtPositionTypeServiceMock;

  private ClassificationsExportFileRequestMapper classificationsExportFileRequestMapper;

  private PivotSILPrenotaExportFlussoRiconciliazioneService service;

  @BeforeEach
  void setUp() {
    classificationsExportFileRequestMapper = new ClassificationsExportFileRequestMapper();
    service = new PivotSILPrenotaExportFlussoRiconciliazioneService(exportFileServiceMock, debtPositionTypeServiceMock, classificationsExportFileRequestMapper);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
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
    PivotSILPrenotaExportFlussoRiconciliazione request = podamFactory.manufacturePojo(PivotSILPrenotaExportFlussoRiconciliazione.class);

    // When, Then
    assertThrows(AuthorizationDeniedException.class, () ->
      service.doReservation(
        userInfo,
        accessToken,
        orgIpaCode,
        request
      )
    );
  }

  @Test
  void givenUserAdminWhenDoReservationThenOk() throws DatatypeConfigurationException {
    //Given
    String orgIpaCode = "ORG1";
    Long organizationId = 123L;
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", orgIpaCode);
    userInfo.setUserId("userId1");
    String accessToken = "token1";

    GregorianCalendar fromCal = new GregorianCalendar(2023, Calendar.JANUARY, 1);
    GregorianCalendar toCal = new GregorianCalendar(2023, Calendar.DECEMBER, 31);
    XMLGregorianCalendar fromXml = DatatypeFactory.newInstance().newXMLGregorianCalendar(fromCal);
    XMLGregorianCalendar toXml = DatatypeFactory.newInstance().newXMLGregorianCalendar(toCal);
    PivotSILPrenotaExportFlussoRiconciliazione request = podamFactory.manufacturePojo(PivotSILPrenotaExportFlussoRiconciliazione.class);
    request.setImportoTesoreria("100,00");
    request.setDataUltimoAggiornamentoDa(fromXml);
    request.setDataUltimoAggiornamentoA(toXml);
    CodiceClassificazioneType type = new CodiceClassificazioneType();
    type.getClassificaziones().add("RT_IUF");
    request.setCodiceClassificazione(type);

    ClassificationsExportFileRequestDTO classificationsExportFileRequestDTO = classificationsExportFileRequestMapper.mapToExportFileRequest(organizationId, request);
    when(exportFileServiceMock.createClassificationsExportFile(classificationsExportFileRequestDTO, accessToken)).thenReturn(456L);
    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
      .thenReturn(new DebtPositionTypeOrg().flagActive(true));

    // When
    PivotSILPrenotaExportFlussoRiconciliazioneRisposta result = service.doReservation(
      userInfo,
      accessToken,
      orgIpaCode,
      request
    );

    // Then
    assertNotNull(result);
    assertEquals(String.valueOf(456L), result.getRequestToken());
    assertEquals(request.getDataUltimoAggiornamentoA(), result.getDataA());
  }

  @Test
  void givenUserIsAdminAndDebtPositionInvalidWhenDoReservationThenException() {
    // Given
    String orgIpaCode = "ORG4";
    Long organizationId = 123L;
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", orgIpaCode);
    userInfo.setUserId("admin4");
    String accessToken = "token4";
    PivotSILPrenotaExportFlussoRiconciliazione request = podamFactory.manufacturePojo(PivotSILPrenotaExportFlussoRiconciliazione.class);
    request.getCodiceClassificazione().getClassificaziones().clear();
    request.getCodiceClassificazione().getClassificaziones().add("RT_IUF");
    request.setImportoTesoreria("100");

    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
      .thenReturn(null);

    // When
    ExportFileServiceException exception = assertThrows(ExportFileServiceException.class, () ->
      service.doReservation(
        userInfo,
        accessToken,
        orgIpaCode,
        request
      )
    );

    // Then
    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_DEBT_POSITION_TYPE_ORG_CODE, exception.getCode());
  }

  @Test
  void givenUserIsAdminAndDebtPositionNotEnabledWhenDoReservationThenException() {
    // Given
    String orgIpaCode = "ORG4";
    Long organizationId = 123L;
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", orgIpaCode);
    userInfo.setUserId("admin4");
    String accessToken = "token4";
    PivotSILPrenotaExportFlussoRiconciliazione request = podamFactory.manufacturePojo(PivotSILPrenotaExportFlussoRiconciliazione.class);
    request.getCodiceClassificazione().getClassificaziones().clear();
    request.getCodiceClassificazione().getClassificaziones().add("RT_IUF");
    request.setImportoTesoreria("100");

    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
      .thenReturn(new DebtPositionTypeOrg().flagActive(false));

    // When
    ExportFileServiceException exception = assertThrows(ExportFileServiceException.class, () ->
      service.doReservation(
        userInfo,
        accessToken,
        orgIpaCode,
        request
      )
    );

    // Then
    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_DEBT_POSITION_TYPE_ORG_STATUS, exception.getCode());
  }

  @Test
  void givenUserIsAdminAndInvalidDateIntervalExceptionWhenDoReservationThenException() {
    // Given
    String orgIpaCode = "ORG5";
    Long organizationId = 123L;
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", orgIpaCode);
    userInfo.setUserId("admin5");
    String accessToken = "token5";
    PivotSILPrenotaExportFlussoRiconciliazione request = podamFactory.manufacturePojo(PivotSILPrenotaExportFlussoRiconciliazione.class);
    request.setImportoTesoreria("100,00");
    CodiceClassificazioneType type = new CodiceClassificazioneType();
    type.getClassificaziones().add("RT_IUF");
    request.setCodiceClassificazione(type);

    ExportFileClientException errorException = new ExportFileClientException(new InvalidValueException(
      ProcessExecutionsErrorDTO.CategoryEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE.getValue(), "error")
    );

    ClassificationsExportFileRequestDTO classificationsExportFileRequestDTO = classificationsExportFileRequestMapper.mapToExportFileRequest(organizationId, request);
    when(exportFileServiceMock.createClassificationsExportFile(classificationsExportFileRequestDTO, accessToken))
      .thenThrow(errorException);
    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
      .thenReturn(new DebtPositionTypeOrg().flagActive(true));

    // When, Then
    ExportFileClientException exception = assertThrows(ExportFileClientException.class, () ->
      service.doReservation(
        userInfo,
        accessToken,
        orgIpaCode,
        request
      )
    );

    assertEquals(ProcessExecutionsErrorDTO.CategoryEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE.getValue(), exception.getCode());
  }
}
