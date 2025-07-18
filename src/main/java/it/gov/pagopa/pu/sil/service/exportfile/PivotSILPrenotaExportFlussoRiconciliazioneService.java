package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.mapper.ClassificationsExportFileRequestMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILPrenotaExportFlussoRiconciliazione;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILPrenotaExportFlussoRiconciliazioneRisposta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PivotSILPrenotaExportFlussoRiconciliazioneService extends AbstractExportFileReservationService {
  private final ExportFileService exportFileService;
  private final ClassificationsExportFileRequestMapper classificationsExportFileRequestMapper;

  public PivotSILPrenotaExportFlussoRiconciliazioneService(ExportFileService exportFileService,
                                                           DebtPositionTypeService debtPositionTypeService,
                                                           ClassificationsExportFileRequestMapper classificationsExportFileRequestMapper) {
    super(debtPositionTypeService);
    this.exportFileService = exportFileService;
    this.classificationsExportFileRequestMapper = classificationsExportFileRequestMapper;
  }

  public PivotSILPrenotaExportFlussoRiconciliazioneRisposta doReservation(UserInfo userInfo, String accessToken, String orgIpaCode, PivotSILPrenotaExportFlussoRiconciliazione request) {
    AuthorizationService.validateAdminRole(orgIpaCode, userInfo);
    Long organizationId = getOrganizationIdFromUserInfo(userInfo, orgIpaCode);
    getAndValidateDebtPositionTypeOrg(
      organizationId,
      request.getIdUnivocoDovuto(),
      accessToken,
      SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO,
      SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO
    );
    ClassificationsExportFileRequestDTO requestDTO = classificationsExportFileRequestMapper.mapToExportFileRequest(organizationId, request);
    Long exportFileId = exportFileService.createClassificationsExportFile(requestDTO, accessToken);
    log.debug("Export file created with ID: {}", exportFileId);
    PivotSILPrenotaExportFlussoRiconciliazioneRisposta response = new PivotSILPrenotaExportFlussoRiconciliazioneRisposta();
    response.setDataA(request.getDataUltimoAggiornamentoDa() != null && request.getDataUltimoAggiornamentoA() != null ? request.getDataUltimoAggiornamentoA() : null);
    response.setRequestToken(exportFileId.toString());
    return response;
  }
}
