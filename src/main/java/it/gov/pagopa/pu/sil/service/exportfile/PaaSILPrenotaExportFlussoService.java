package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.veneto.regione.pagamenti.ente.PaaSILPrenotaExportFlusso;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
public class PaaSILPrenotaExportFlussoService {

  private final ExportFileService exportFileService;

  public PaaSILPrenotaExportFlussoService(ExportFileService exportFileService) {
    this.exportFileService = exportFileService;
  }

  public Long paaSILPrenotaExportFlusso(
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode,
    PaaSILPrenotaExportFlusso request) {

    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);

    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call export file for organization {}", clientId, orgIpaCode);
      throw new UnauthorizedException("Utente non autorizzato");
    }

    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);

    Optional<PaaSILPrenotaExportFlusso> optRequest = Optional.ofNullable(request);
    String fileVersion = optRequest.map(PaaSILPrenotaExportFlusso::getVersioneTracciato).orElse(null);
    OffsetDateTime from = optRequest.flatMap(r -> Optional.ofNullable(r.getDateFrom()))
      .map(x -> x.toGregorianCalendar().toZonedDateTime().toOffsetDateTime()).orElse(null);
    OffsetDateTime to = optRequest.flatMap(r -> Optional.ofNullable(r.getDateTo()))
      .map(x -> x.toGregorianCalendar().toZonedDateTime().toOffsetDateTime()).orElse(null);
    Long debtPositionTypeOrgId = optRequest.flatMap(r -> Optional.ofNullable(r.getIdentificativoTipoDovuto()))
      .map(Long::valueOf).orElse(null);

    PaidExportFileRequestDTO requestDTO = mapToExportRequest(organizationId, fileVersion, from, to, debtPositionTypeOrgId);

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
