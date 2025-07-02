package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
@Service
public class PaaSILPrenotaExportFlussoIncrementaleConRicevutaService extends AbstractExportFileReservationService {

  private final ExportFileService exportFileService;

  public PaaSILPrenotaExportFlussoIncrementaleConRicevutaService(ExportFileService exportFileService,
                                                                 DebtPositionService debtPositionService) {
    super(debtPositionService);
    this.exportFileService = exportFileService;
  }

  @SuppressWarnings("java:S107")
  public Long doReservation(
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode,
    String fileVersion,
    OffsetDateTime from,
    OffsetDateTime to,
    String debtPositionTypeOrgCode,
    boolean incremental) {

    checkAdminRole(orgIpaCode, userInfo);
    Long organizationId = getOrganizationIdFromUserInfo(userInfo, orgIpaCode);
    DebtPositionTypeOrg debtPositionTypeOrg = getAndValidateDebtPositionTypeOrg(
      organizationId,
      debtPositionTypeOrgCode,
      accessToken,
      SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO,
      SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO
    );

    PaidExportFileRequestDTO requestDTO = mapToExportRequest(organizationId, fileVersion, from, to, debtPositionTypeOrg.getDebtPositionTypeOrgId(), incremental);
    Long exportFileId = exportFileService.createPaidExportFile(requestDTO, accessToken);
    log.debug("Export file created with ID: {}", exportFileId);
    return exportFileId;
  }

  private PaidExportFileRequestDTO mapToExportRequest(Long organizationId,
                                                      String fileVersion,
                                                      OffsetDateTime from,
                                                      OffsetDateTime to,
                                                      Long debtPositionTypeOrgId,
                                                      boolean incremental) {
    PaidExportFileRequestDTO ret = new PaidExportFileRequestDTO()
      .organizationId(organizationId)
      .exportFileType(PaidExportFileRequestDTO.ExportFileTypeEnum.PAID)
      .fileVersion(fileVersion);

    if (incremental) {
      ret.filterFields(new PaidExportFileFilter()
        .installmentUpdateDateTime(new OffsetDateTimeIntervalFilter()
          .from(from)
          .to(to))
        .debtPositionTypeOrgId(debtPositionTypeOrgId));
    } else {
      ret.filterFields(new PaidExportFileFilter()
        .paymentDateTime(new OffsetDateTimeIntervalFilter()
          .from(from)
          .to(to))
        .debtPositionTypeOrgId(debtPositionTypeOrgId));
    }
    return ret;
  }
}
