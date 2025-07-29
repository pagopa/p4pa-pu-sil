package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFileStatus;
import it.gov.pagopa.pu.sil.controller.generated.ExportApi;
import it.gov.pagopa.pu.sil.dto.generated.*;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.exportfile.ClassificationsExportFileReservationService;
import it.gov.pagopa.pu.sil.service.exportfile.ExportFileProcessingStatusService;
import it.gov.pagopa.pu.sil.service.exportfile.PaidExportFileReservationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class MassiveExportController implements ExportApi {

  private final PaidExportFileReservationService paidExportFileReservationService;
  private final ClassificationsExportFileReservationService classificationsExportFileReservationService;
  private final ExportFileProcessingStatusService exportFileProcessingStatusService;

  public MassiveExportController(PaidExportFileReservationService paidExportFileReservationService,
                                 ClassificationsExportFileReservationService classificationsExportFileReservationService,
                                 ExportFileProcessingStatusService exportFileProcessingStatusService) {
    this.paidExportFileReservationService = paidExportFileReservationService;
    this.classificationsExportFileReservationService = classificationsExportFileReservationService;
    this.exportFileProcessingStatusService = exportFileProcessingStatusService;
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

  @Override
  public ResponseEntity<ExportStatusResponseDTO> massiveExportStatus(String orgFiscalCode, String exportId) {

    log.info("Received massive export status request for orgFiscalCode: {}", orgFiscalCode);

    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);

    ExportStatusResponseDTO ret = new ExportStatusResponseDTO();

    log.debug("Processing export status request for orgIpaCode: {}", orgIpaCode);

    Pair<ExportFileStatus, String> response = exportFileProcessingStatusService.getProcessingStatus(
      userInfo, accessToken, orgIpaCode, Long.valueOf(exportId), null);

    ret.setExportId(exportId);
    ret.status(response.getLeft());
    ret.downloadUrl(new DownloadUrl(response.getRight()));

    return ResponseEntity.ok(ret);
  }

}
