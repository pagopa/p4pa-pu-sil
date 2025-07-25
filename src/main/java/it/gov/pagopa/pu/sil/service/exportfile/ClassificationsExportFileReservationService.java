package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.dto.generated.ClassificationsExportRequestDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class ClassificationsExportFileReservationService extends AbstractExportFileReservationService {

  private final ExportFileService exportFileService;

  public ClassificationsExportFileReservationService(ExportFileService exportFileService,
                                                     DebtPositionTypeService debtPositionTypeService) {
    super(debtPositionTypeService);
    this.exportFileService = exportFileService;
  }

  @SuppressWarnings("java:S107")
  public Long doReservation(UserInfo userInfo, String accessToken, String orgIpaCode,
                            ClassificationsExportRequestDTO classificationsExportRequestDTO) {

    AuthorizationService.validateAdminRole(orgIpaCode, userInfo);
    Long organizationId = getOrganizationIdFromUserInfo(userInfo, orgIpaCode);

    Optional.ofNullable(classificationsExportRequestDTO.getExportFilters().getDebtPositionTypeOrgCodes())
      .ifPresent(debtPositionTypeOrgCodes ->
          debtPositionTypeOrgCodes.forEach(debtPositionTypeOrgCode ->
            getAndValidateDebtPositionTypeOrg(
              organizationId,
              debtPositionTypeOrgCode,
              accessToken,
              SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO,
              SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO)));

    ClassificationsExportFileRequestDTO requestDTO = mapToExportRequest(organizationId, classificationsExportRequestDTO);
    Long exportFileId = exportFileService.createClassificationsExportFile(requestDTO, accessToken);
    log.debug("Export file created with ID: {}", exportFileId);
    return exportFileId;
  }

  private ClassificationsExportFileRequestDTO mapToExportRequest(Long organizationId, ClassificationsExportRequestDTO classificationsExportRequestDTO) {
    return new ClassificationsExportFileRequestDTO()
      .organizationId(organizationId)
      .exportFileType(ClassificationsExportFileRequestDTO.ExportFileTypeEnum.CLASSIFICATIONS)
      .fileVersion(classificationsExportRequestDTO.getFileVersion())
      .filterFields(classificationsExportRequestDTO.getExportFilters());
  }
}
