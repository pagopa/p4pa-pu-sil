package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.controller.generated.ExportApi;
import it.gov.pagopa.pu.sil.dto.generated.ClassificationsExportRequestDTO;
import it.gov.pagopa.pu.sil.dto.generated.ExportFileResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.PaidExportRequestDTO;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.exportfile.ClassificationsExportFileReservationService;
import it.gov.pagopa.pu.sil.service.exportfile.PaidExportFileReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class MassiveExportController implements ExportApi {

  private final PaidExportFileReservationService paidExportFileReservationService;
  private final ClassificationsExportFileReservationService classificationsExportFileReservationService;

  public MassiveExportController(PaidExportFileReservationService paidExportFileReservationService,
                                 ClassificationsExportFileReservationService classificationsExportFileReservationService) {
    this.paidExportFileReservationService = paidExportFileReservationService;
    this.classificationsExportFileReservationService = classificationsExportFileReservationService;
  }

  @Override
  public ResponseEntity<ExportFileResponseDTO> massivePaidExportRequest(String orgFiscalCode, PaidExportRequestDTO paidExportRequestDTO) {

    log.info("Received massive export request for orgFiscalCode: {}, fileType: PAID", orgFiscalCode);

    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);

    ExportFileResponseDTO ret = new ExportFileResponseDTO();

    log.debug("Processing PAID export request for orgIpaCode: {}", orgIpaCode);

    Long exportFileId = paidExportFileReservationService.doReservation(userInfo, accessToken, orgIpaCode, paidExportRequestDTO);

    ret.exportId(exportFileId.toString());

    return ResponseEntity.ok(ret);
  }

  @Override
  public ResponseEntity<ExportFileResponseDTO> massiveClassificationsExportRequest(String orgFiscalCode, ClassificationsExportRequestDTO classificationsExportRequestDTO) {

    log.info("Received massive export request for orgFiscalCode: {}, fileType: CLASSIFICATIONS", orgFiscalCode);

    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);

    ExportFileResponseDTO ret = new ExportFileResponseDTO();

    log.debug("Processing CLASSIFICATIONS export request for orgIpaCode: {}", orgIpaCode);

    Long exportFileId = classificationsExportFileReservationService.doReservation(userInfo, accessToken, orgIpaCode, classificationsExportRequestDTO);

    ret.exportId(exportFileId.toString());

    return ResponseEntity.ok(ret);
  }
}
