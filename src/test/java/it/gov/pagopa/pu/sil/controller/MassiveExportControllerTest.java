package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.dto.generated.ClassificationsExportRequestDTO;
import it.gov.pagopa.pu.sil.dto.generated.ExportFileResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.PaidExportRequestDTO;
import it.gov.pagopa.pu.sil.security.SecurityUtilsTest;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.exportfile.ClassificationsExportFileReservationService;
import it.gov.pagopa.pu.sil.service.exportfile.PaidExportFileReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MassiveExportControllerTest {
  @Mock
  private PaidExportFileReservationService paidExportFileReservationServiceMock;
  @Mock
  private ClassificationsExportFileReservationService classificationsExportFileReservationServiceMock;

  @InjectMocks
  private MassiveExportController controller;

  private final String accessToken = "fakeAccessToken";
  private final String orgFiscalCode = "ORG123456789";
  private final String orgIpaCode = "userIpaCode";
  private UserInfo userInfo;

  @BeforeEach
  void setUp() {
    userInfo = AuthorizationServiceTest.buildAdminUser(1L, orgFiscalCode, orgIpaCode);
    userInfo.setMappedExternalUserId("fakeExternalUser");
    SecurityUtilsTest.configureSecurityContext(accessToken, userInfo);

  }

  @AfterEach
  void clear(){
    SecurityUtilsTest.clearSecurityContext();
  }

  @Test
  void whenMassiveExportRequestPaidThenOk() {
    // Given
    String expectedExportId = "1";

    PaidExportRequestDTO paidExportRequestDTO = new PaidExportRequestDTO();

    when(paidExportFileReservationServiceMock
      .doReservation(userInfo, accessToken, orgIpaCode, paidExportRequestDTO))
      .thenReturn(Long.valueOf(expectedExportId));

    // When
    ResponseEntity<ExportFileResponseDTO> response = controller
      .massivePaidExportRequest(orgFiscalCode, paidExportRequestDTO);

    // Then
    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertNotNull(response.getBody());
    Assertions.assertEquals(expectedExportId, response.getBody().getExportId());

  }

  @Test
  void whenMassiveExportRequestClassificationsThenOk() {
    // Given
    String expectedExportId = "1";

    ClassificationsExportRequestDTO classificationsExportRequestDTO = new ClassificationsExportRequestDTO();

    when(classificationsExportFileReservationServiceMock
      .doReservation(userInfo, accessToken, orgIpaCode, classificationsExportRequestDTO))
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
