package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile;
import it.gov.pagopa.pu.sil.controller.generated.MassiveExportApi;
import it.gov.pagopa.pu.sil.dto.generated.ExportFileResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.ExportRequestDTO;
import it.gov.pagopa.pu.sil.mapper.MassiveExportRequestMapper;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.exportfile.PaaSILPrenotaExportFlussoIncrementaleConRicevutaService;
import it.gov.pagopa.pu.sil.service.exportfile.PivotSILPrenotaExportFlussoRiconciliazioneService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@RestController
public class MassiveExportController implements MassiveExportApi {

  private final PaaSILPrenotaExportFlussoIncrementaleConRicevutaService paaSILPrenotaExportFlussoIncrementaleConRicevutaService;
  private final PivotSILPrenotaExportFlussoRiconciliazioneService pivotSILPrenotaExportFlussoRiconciliazioneService;

  public MassiveExportController(PaaSILPrenotaExportFlussoIncrementaleConRicevutaService paaSILPrenotaExportFlussoIncrementaleConRicevutaService,
                                 PivotSILPrenotaExportFlussoRiconciliazioneService pivotSILPrenotaExportFlussoRiconciliazioneService) {
    this.paaSILPrenotaExportFlussoIncrementaleConRicevutaService = paaSILPrenotaExportFlussoIncrementaleConRicevutaService;
    this.pivotSILPrenotaExportFlussoRiconciliazioneService = pivotSILPrenotaExportFlussoRiconciliazioneService;
  }

  @Override
  public ResponseEntity<ExportFileResponseDTO> massiveExportRequest(String orgFiscalCode, ExportRequestDTO exportRequestDTO) {

    ExportFile.ExportFileTypeEnum exportFileType = exportRequestDTO.getExportFileType();

    log.info("Received massive export request for orgFiscalCode: {}, fileType: {}", orgFiscalCode, exportFileType);

    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);

    ExportFileResponseDTO ret = new ExportFileResponseDTO();

    MassiveExportRequestMapper massiveExportRequestMapper = new MassiveExportRequestMapper(
      exportRequestDTO.getExportFilters(), exportRequestDTO.getExportFlags());

    switch (exportFileType) {
      case PAID -> {
        log.debug("Processing PAID export request for orgIpaCode: {}", orgIpaCode);

        boolean incremental = massiveExportRequestMapper.getFlagFieldValue(MassiveExportRequestMapper.INCREMENTAL).orElse(false);

        String fileVersion = massiveExportRequestMapper.getFilterFieldValue(MassiveExportRequestMapper.FILE_VERSION).orElse(null);

        OffsetDateTime from = incremental
          ? massiveExportRequestMapper.parseFilterDate(MassiveExportRequestMapper.FROM).orElse(null)
          : massiveExportRequestMapper.parseFilterDate(MassiveExportRequestMapper.FROM)
          .map(o -> o.truncatedTo(ChronoUnit.DAYS)).orElse(null);
        OffsetDateTime to = incremental
          ? massiveExportRequestMapper.parseFilterDate(MassiveExportRequestMapper.TO).orElse(null)
          : massiveExportRequestMapper.parseFilterDate(MassiveExportRequestMapper.TO)
          .map(o -> o.truncatedTo(ChronoUnit.DAYS)).orElse(null);

        String debtPositionTypeOrgCode = massiveExportRequestMapper
          .getFilterFieldValue(MassiveExportRequestMapper.DEBT_POSITION_TYPE_ORG_CODE).orElse(null);

        Long exportFileId = paaSILPrenotaExportFlussoIncrementaleConRicevutaService.doReservation(userInfo, accessToken, orgIpaCode,
          fileVersion, from, to, debtPositionTypeOrgCode, incremental);

        ret.exportId(exportFileId.toString());
      }

      case CLASSIFICATIONS -> {
        log.debug("Processing CLASSIFICATIONS export request for orgIpaCode: {}", orgIpaCode);

        String iud = massiveExportRequestMapper.getFilterFieldValue(MassiveExportRequestMapper.IUD).orElse(null);

        OffsetDateTime lastUpdateDateFrom = massiveExportRequestMapper
          .parseFilterDate(MassiveExportRequestMapper.LAST_UPDATE_DATE_FROM).orElse(null);
        OffsetDateTime lastUpdateDateTo = massiveExportRequestMapper
          .parseFilterDate(MassiveExportRequestMapper.LAST_UPDATE_DATE_TO).orElse(null);

        Pair<Long, OffsetDateTime> reservationResponse =
          pivotSILPrenotaExportFlussoRiconciliazioneService.doReservation(
            userInfo, accessToken, orgIpaCode, iud, lastUpdateDateFrom, lastUpdateDateTo,
            () -> massiveExportRequestMapper.mapToClassificationsExportFileRequest(iud, lastUpdateDateFrom, lastUpdateDateTo));

        ret.exportId(reservationResponse.getLeft().toString());
      }

      default -> throw new IllegalArgumentException("Unsupported export file type: " + exportFileType);
    }

    return ResponseEntity.ok(ret);
  }
}
