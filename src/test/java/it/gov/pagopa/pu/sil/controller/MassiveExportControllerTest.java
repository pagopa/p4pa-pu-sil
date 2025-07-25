package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.dto.generated.ClassificationsExportRequestDTO;
import it.gov.pagopa.pu.sil.dto.generated.ExportFileResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.PaidExportRequestDTO;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.exportfile.ClassificationsExportFileReservationService;
import it.gov.pagopa.pu.sil.service.exportfile.PaidExportFileReservationService;
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

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MassiveExportControllerTest {
  @Mock
  private PaidExportFileReservationService paidExportFileReservationService;
  @Mock
  private ClassificationsExportFileReservationService classificationsExportFileReservationService;

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

      PaidExportRequestDTO paidExportRequestDTO = new PaidExportRequestDTO();

      when(paidExportFileReservationService
        .doReservation(userInfo, accessToken, userIpaCode, paidExportRequestDTO))
        .thenReturn(Long.valueOf(expectedExportId));

      // When
      ResponseEntity<ExportFileResponseDTO> response = controller
        .massivePaidExportRequest(orgFiscalCode, paidExportRequestDTO);

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

      ClassificationsExportRequestDTO classificationsExportRequestDTO = new ClassificationsExportRequestDTO();

      when(classificationsExportFileReservationService
        .doReservation(userInfo, accessToken, userIpaCode, classificationsExportRequestDTO))
        .thenReturn(Long.valueOf(expectedExportId));

      // When
      ResponseEntity<ExportFileResponseDTO> response = controller
        .massiveClassificationsExportRequest(orgFiscalCode, classificationsExportRequestDTO);

      // Then
      Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
      Assertions.assertNotNull(response.getBody());
      Assertions.assertEquals(expectedExportId, response.getBody().getExportId());
    }

  }

}
