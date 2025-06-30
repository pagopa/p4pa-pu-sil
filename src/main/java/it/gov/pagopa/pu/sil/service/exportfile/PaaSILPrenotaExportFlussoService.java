package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
public class PaaSILPrenotaExportFlussoService {

  private final ExportFileService exportFileService;
  private final DebtPositionService debtPositionService;

  public PaaSILPrenotaExportFlussoService(ExportFileService exportFileService,
                                          DebtPositionService debtPositionService) {
    this.exportFileService = exportFileService;
    this.debtPositionService = debtPositionService;
  }

  public Long paaSILPrenotaExportFlusso(
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode,
    String fileVersion,
    OffsetDateTime from,
    OffsetDateTime to,
    String debtPositionTypeOrgCode) {

    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);

    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call export file for organization {}", clientId, orgIpaCode);
      throw new UnauthorizedException("Utente non autorizzato");
    }

    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);

    DebtPositionTypeOrg debtPositionTypeOrg = debtPositionService.getDebtPositionTypeOrgByOrgIdAndType(
      organizationId, debtPositionTypeOrgCode, accessToken);

    if (debtPositionTypeOrg == null) {
      throw new ExportFileServiceException(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, "Tipo dovuto non valido: " + debtPositionTypeOrgCode);
    } else if (Boolean.FALSE.equals(debtPositionTypeOrg.getFlagActive())) {
      throw new ExportFileServiceException(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO, "Tipo dovuto non abilitato: " + debtPositionTypeOrgCode);
    }

    PaidExportFileRequestDTO requestDTO = mapToExportRequest(organizationId, fileVersion, from, to, debtPositionTypeOrg.getDebtPositionTypeOrgId());

    Long exportFileId = exportFileService.createPaidExportFile(requestDTO, accessToken);
    log.debug("Export file created with ID: {}", exportFileId);

    return exportFileId;
  }

  private PaidExportFileRequestDTO mapToExportRequest(Long organizationId,
                                                      String fileVersion,
                                                      OffsetDateTime from,
                                                      OffsetDateTime to,
                                                      Long debtPositionTypeOrgId) {
    return new PaidExportFileRequestDTO()
      .organizationId(organizationId)
      .exportFileType(PaidExportFileRequestDTO.ExportFileTypeEnum.PAID)
      .fileVersion(fileVersion)
      .filterFields(new PaidExportFileFilter()
        .paymentDateTime(new OffsetDateTimeIntervalFilter()
          .from(from)
          .to(to))
        .debtPositionTypeOrgId(debtPositionTypeOrgId));
  }
}
