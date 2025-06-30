package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILPrenotaExportFlussoRiconciliazione;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class PivotSILPrenotaExportFlussoRiconciliazioneService {
  private final ExportFileService exportFileService;
  private final DebtPositionService debtPositionService;

  public PivotSILPrenotaExportFlussoRiconciliazioneService(ExportFileService exportFileService, DebtPositionService debtPositionService) {
    this.exportFileService = exportFileService;
    this.debtPositionService = debtPositionService;
  }

  public Long doReservation(UserInfo userInfo, String accessToken, String orgIpaCode, PivotSILPrenotaExportFlussoRiconciliazione request) {

    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);

    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call export file for organization {}", clientId, orgIpaCode);
      throw new UnauthorizedException("Utente non autorizzato");
    }

    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);

    DebtPositionTypeOrg debtPositionTypeOrg = debtPositionService.getDebtPositionTypeOrgByOrgIdAndType(
      organizationId, request.getIdUnivocoDovuto(), accessToken);

    if (debtPositionTypeOrg == null) {
      throw new ExportFileServiceException(SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, "Tipo dovuto non valido: " + request.getIdUnivocoDovuto());
    } else if (Boolean.FALSE.equals(debtPositionTypeOrg.getFlagActive())) {
      throw new ExportFileServiceException(SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO, "Tipo dovuto non abilitato: " + request.getIdUnivocoDovuto());
    }
    ClassificationsExportFileRequestDTO requestDTO = mapToExportFileRequest(request, organizationId);

    Long exportFileId = exportFileService.createClassificationsExportFile(requestDTO, accessToken);
    log.debug("Export file created with ID: {}", exportFileId);

    return exportFileId;
  }

  private ClassificationsExportFileRequestDTO mapToExportFileRequest(
      PivotSILPrenotaExportFlussoRiconciliazione request,
      Long organizationId) {
      return new ClassificationsExportFileRequestDTO()
        .organizationId(organizationId)
        .exportFileType(ClassificationsExportFileRequestDTO.ExportFileTypeEnum.CLASSIFICATIONS)
        .fileVersion(request.getVersioneTracciato())
        .filterFields(new ClassificationsExportFileFilter());
  }
}
