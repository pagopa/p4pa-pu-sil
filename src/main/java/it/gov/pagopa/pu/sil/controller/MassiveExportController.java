package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.sil.controller.generated.ExportApi;
import it.gov.pagopa.pu.sil.dto.generated.ExportFileResponseDTO;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.exportfile.PaaSILPrenotaExportFlussoIncrementaleConRicevutaService;
import it.gov.pagopa.pu.sil.service.exportfile.PivotSILPrenotaExportFlussoRiconciliazioneService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class MassiveExportController implements ExportApi {

  private final PaaSILPrenotaExportFlussoIncrementaleConRicevutaService paaSILPrenotaExportFlussoIncrementaleConRicevutaService;
  private final PivotSILPrenotaExportFlussoRiconciliazioneService pivotSILPrenotaExportFlussoRiconciliazioneService;

  public MassiveExportController(PaaSILPrenotaExportFlussoIncrementaleConRicevutaService paaSILPrenotaExportFlussoIncrementaleConRicevutaService,
                                 PivotSILPrenotaExportFlussoRiconciliazioneService pivotSILPrenotaExportFlussoRiconciliazioneService) {
    this.paaSILPrenotaExportFlussoIncrementaleConRicevutaService = paaSILPrenotaExportFlussoIncrementaleConRicevutaService;
    this.pivotSILPrenotaExportFlussoRiconciliazioneService = pivotSILPrenotaExportFlussoRiconciliazioneService;
  }

  @Override
  public ResponseEntity<ExportFileResponseDTO> massivePaidExportRequest(String orgFiscalCode, PaidExportFileRequestDTO paidExportFileRequestDTO) {

    log.info("Received massive export request for orgFiscalCode: {}, fileType: PAID", orgFiscalCode);

    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);

    ExportFileResponseDTO ret = new ExportFileResponseDTO();

    log.debug("Processing PAID export request for orgIpaCode: {}", orgIpaCode);

    Long exportFileId = paaSILPrenotaExportFlussoIncrementaleConRicevutaService.doReservation(userInfo, accessToken, orgIpaCode, paidExportFileRequestDTO);

    ret.exportId(exportFileId.toString());

    return ResponseEntity.ok(ret);
  }

  @Override
  public ResponseEntity<ExportFileResponseDTO> massiveClassificationsExportRequest(String orgFiscalCode, ClassificationsExportFileRequestDTO classificationsExportFileRequestDTO) {

    log.info("Received massive export request for orgFiscalCode: {}, fileType: CLASSIFICATIONS", orgFiscalCode);

    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);

    ExportFileResponseDTO ret = new ExportFileResponseDTO();

    log.debug("Processing CLASSIFICATIONS export request for orgIpaCode: {}", orgIpaCode);

    Long exportFileId = pivotSILPrenotaExportFlussoRiconciliazioneService.doReservation(
        userInfo, accessToken, orgIpaCode, classificationsExportFileRequestDTO);

    ret.exportId(exportFileId.toString());

    return ResponseEntity.ok(ret);
  }
}
