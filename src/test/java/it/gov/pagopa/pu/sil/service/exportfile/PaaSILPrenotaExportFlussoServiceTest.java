package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.legacy.ExportFileLegacyFileVersion;
import it.gov.pagopa.pu.sil.exception.ExportFileRequestValidationException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILPrenotaExportFlusso;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaaSILPrenotaExportFlussoServiceTest {

  @Mock
  private ExportFileService exportFileServiceMock;
  @Mock
  private DebtPositionService debtPositionServiceMock;
  @Mock
  private ValidationService validationServiceMock;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  private PaaSILPrenotaExportFlussoService service;

  @BeforeEach
  void setUp() {
    service = new PaaSILPrenotaExportFlussoService(
      exportFileServiceMock,
      debtPositionServiceMock,
      validationServiceMock
    );
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(
      exportFileServiceMock,
      debtPositionServiceMock,
      validationServiceMock
    );
  }

  @Test
  void givenUserNotAdminWhenPaaSILPrenotaExportFlussoThenKo() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("userId1");
    String orgIpaCode = "ORG1";
    String accessToken = "token1";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);

      assertThrows(UnauthorizedException.class, () ->
        service.paaSILPrenotaExportFlusso(
          userInfo,
          accessToken,
          orgIpaCode,
          new PaaSILPrenotaExportFlusso()
        )
      );
      verifyNoInteractions(
        exportFileServiceMock,
        debtPositionServiceMock,
        validationServiceMock);
    }
  }

  @Test
  void givenUserIsAdminWhenPaaSILPrenotaExportFlussoThenOk() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin2");
    String orgIpaCode = "ORG2";
    Long organizationId = 123L;
    String accessToken = "token2";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(organizationId);

      when(exportFileServiceMock.createPaidExportFile(any(), eq(accessToken))).thenReturn(456L);
      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(new DebtPositionTypeOrg());
      when(validationServiceMock.validateDebtPositionTypeOrg(any(), any()))
        .thenReturn(null);

      PaaSILPrenotaExportFlusso request = podamFactory.manufacturePojoWithFullData(PaaSILPrenotaExportFlusso.class);
      request.setVersioneTracciato(ExportFileLegacyFileVersion.v1_0.getLegacyValue());
      request.setIdentificativoTipoDovuto(String.valueOf(1L));

      Long result = service.paaSILPrenotaExportFlusso(
        userInfo,
        accessToken,
        orgIpaCode,
        request
      );

      assertNotNull(result);
      assertEquals(456L, result);
      verify(exportFileServiceMock).createPaidExportFile(any(), eq(accessToken));
    }
  }

  @Test
  void givenNullUserInfoWhenPaaSILPrenotaExportFlussoThenOk() {
    UserInfo userInfo = null;
    String orgIpaCode = "ORG3";
    Long organizationId = 789L;
    String accessToken = "token3";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), isNull())).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(isNull(), eq(orgIpaCode))).thenReturn(organizationId);

      when(exportFileServiceMock.createPaidExportFile(any(), eq(accessToken))).thenReturn(999L);
      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(new DebtPositionTypeOrg());
      when(validationServiceMock.validateDebtPositionTypeOrg(any(), any()))
        .thenReturn(null);

      PaaSILPrenotaExportFlusso request = podamFactory.manufacturePojoWithFullData(PaaSILPrenotaExportFlusso.class);
      request.setVersioneTracciato(ExportFileLegacyFileVersion.v1_0.getLegacyValue());
      request.setIdentificativoTipoDovuto(String.valueOf(1L));

      Long result = service.paaSILPrenotaExportFlusso(
        userInfo,
        accessToken,
        orgIpaCode,
        request
      );

      assertNotNull(result);
      assertEquals(999L, result);
      verify(exportFileServiceMock).createPaidExportFile(any(), eq(accessToken));
    }
  }

  @Test
  void givenUserIsAdminAndDebtPositionInvalidWhenPaaSILPrenotaExportFlussoThenException() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin4");
    String orgIpaCode = "ORG4";
    Long organizationId = 123L;
    String accessToken = "token4";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(organizationId);

      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(new DebtPositionTypeOrg());
      when(validationServiceMock.validateDebtPositionTypeOrg(any(), any()))
        .thenReturn(Pair.of(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, "Invalid debt position type"));

      PaaSILPrenotaExportFlusso request = podamFactory.manufacturePojoWithFullData(PaaSILPrenotaExportFlusso.class);
      request.setVersioneTracciato(ExportFileLegacyFileVersion.v1_0.getLegacyValue());
      request.setIdentificativoTipoDovuto(String.valueOf(1L));

      ExportFileRequestValidationException exception = assertThrows(ExportFileRequestValidationException.class, () ->
        service.paaSILPrenotaExportFlusso(
          userInfo,
          accessToken,
          orgIpaCode,
          request
        )
      );

      assertEquals(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, exception.getFault());

      verifyNoInteractions(exportFileServiceMock);
    }
  }

  @Test
  void givenUserIsAdminAndDateFromIsNullWhenPaaSILPrenotaExportFlussoThenException() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin5");
    String orgIpaCode = "ORG5";
    Long organizationId = 123L;
    String accessToken = "token5";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(organizationId);

      PaaSILPrenotaExportFlusso request = podamFactory.manufacturePojoWithFullData(PaaSILPrenotaExportFlusso.class);
      request.setVersioneTracciato(ExportFileLegacyFileVersion.v1_0.getLegacyValue());
      request.setDateFrom(null);
      request.setIdentificativoTipoDovuto(String.valueOf(1L));

      ExportFileRequestValidationException exception = assertThrows(ExportFileRequestValidationException.class, () ->
        service.paaSILPrenotaExportFlusso(
          userInfo,
          accessToken,
          orgIpaCode,
          request
        )
      );

      assertEquals(SilFaults.PAA_DATE_FROM_NON_VALIDO, exception.getFault());

      verifyNoInteractions(exportFileServiceMock,
        debtPositionServiceMock,
        validationServiceMock);
    }
  }

}
