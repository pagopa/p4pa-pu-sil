package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile;
import it.gov.pagopa.pu.sil.dto.generated.ExportFileResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.ExportRequestDTO;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.exportfile.PaaSILPrenotaExportFlussoIncrementaleConRicevutaService;
import it.gov.pagopa.pu.sil.service.exportfile.PivotSILPrenotaExportFlussoRiconciliazioneService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MassiveExportControllerTest {
  @Mock
  private PaaSILPrenotaExportFlussoIncrementaleConRicevutaService paaSILPrenotaExportFlussoIncrementaleConRicevutaService;
  @Mock
  private PivotSILPrenotaExportFlussoRiconciliazioneService pivotSILPrenotaExportFlussoRiconciliazioneService;

  @InjectMocks
  private MassiveExportController controller;

  private UserInfo userInfo;

  @BeforeEach
  void setUp() {
    userInfo = new UserInfo();
    userInfo.setMappedExternalUserId("fakeExternalUser");
    Authentication authentication = new UsernamePasswordAuthenticationToken(userInfo, "fakeAccessToken");
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  void whenMassiveExportRequestPaidThenOk() {
    // Given
    String orgFiscalCode = "ORG123456789";
    String accessToken = "fakeAccessToken";
    String userIpaCode = "userIpaCode";

    String expectedExportId = "1";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode))
        .thenReturn(userIpaCode);

      when(paaSILPrenotaExportFlussoIncrementaleConRicevutaService
        .doReservation(eq(userInfo), eq(accessToken), eq(userIpaCode), any(), any(), any(), any(), anyBoolean()))
        .thenReturn(Long.valueOf(expectedExportId));

      // When
      ResponseEntity<ExportFileResponseDTO> response = controller
        .massiveExportRequest(orgFiscalCode, new ExportRequestDTO().exportFileType(ExportFile.ExportFileTypeEnum.PAID));

      // Then
      Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
      Assertions.assertNotNull(response.getBody());
      Assertions.assertEquals(expectedExportId, response.getBody().getExportId());
    }

  }

  @Test
  void whenMassiveExportRequestClassificationsThenOk() {
    // Given
    String orgFiscalCode = "ORG123456789";
    String accessToken = "fakeAccessToken";
    String userIpaCode = "userIpaCode";

    String expectedExportId = "1";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode))
        .thenReturn(userIpaCode);

      when(pivotSILPrenotaExportFlussoRiconciliazioneService
        .doReservation(eq(userInfo), eq(accessToken), eq(userIpaCode), any(), any(), any(), any()))
        .thenReturn(Pair.of(Long.valueOf(expectedExportId), null));

      // When
      ResponseEntity<ExportFileResponseDTO> response = controller
        .massiveExportRequest(orgFiscalCode, new ExportRequestDTO().exportFileType(ExportFile.ExportFileTypeEnum.CLASSIFICATIONS));

      // Then
      Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
      Assertions.assertNotNull(response.getBody());
      Assertions.assertEquals(expectedExportId, response.getBody().getExportId());
    }

  }

}
