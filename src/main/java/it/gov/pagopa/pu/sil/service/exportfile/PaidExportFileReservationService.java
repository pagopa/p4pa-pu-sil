package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileFilter.DebtPositionOriginsEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.dto.generated.PaidExportRequestDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaidExportFileReservationService extends
  AbstractExportFileReservationService {
  private final ExportFileService exportFileService;

  public PaidExportFileReservationService(ExportFileService exportFileService,
    DebtPositionTypeService debtPositionTypeService) {
    super(debtPositionTypeService);
    this.exportFileService = exportFileService;
  }

  public Long doReservation(
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode,
    PaidExportRequestDTO paidExportRequestDTO) {

    AuthorizationService.validateAdminRole(orgIpaCode, userInfo);
    Long organizationId = getOrganizationIdFromUserInfo(userInfo, orgIpaCode);

    Long debtPositionTypeOrgId = null;
    if (!IDENTIFICATIVO_TIPO_DOVUTO_SECONDARIO.equals(
      paidExportRequestDTO.getExportFilters().getDebtPositionTypeOrgCode())) {
      debtPositionTypeOrgId =
        Optional.ofNullable(
            paidExportRequestDTO.getExportFilters().getDebtPositionTypeOrgCode())
          .map(debtPositionTypeOrgCode -> getAndValidateDebtPositionTypeOrg(
            organizationId,
            debtPositionTypeOrgCode,
            accessToken,
            SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO,
            SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO
          )).map(DebtPositionTypeOrg::getDebtPositionTypeOrgId).orElse(null);
    }

    PaidExportFileRequestDTO requestDTO = mapToExportRequest(organizationId,
      debtPositionTypeOrgId, paidExportRequestDTO);
    Long exportFileId = exportFileService.createPaidExportFile(requestDTO,
      accessToken);
    log.debug("Export file created with ID: {}", exportFileId);
    return exportFileId;
  }

  private PaidExportFileRequestDTO mapToExportRequest(Long organizationId,
    Long debtPositionTypeOrgId,
    PaidExportRequestDTO paidExportRequestDTO) {
    PaidExportFileFilter filters = new PaidExportFileFilter()
      .installmentUpdateDateTime(
        paidExportRequestDTO.getExportFilters().getInstallmentUpdateDateTime())
      .paymentDateTime(
        paidExportRequestDTO.getExportFilters().getPaymentDateTime());

    if (IDENTIFICATIVO_TIPO_DOVUTO_SECONDARIO.equals(
      paidExportRequestDTO.getExportFilters().getDebtPositionTypeOrgCode())) {
      filters.setDebtPositionOrigins(
        List.of(DebtPositionOriginsEnum.SECONDARY_ORG));
    } else {
      filters.setDebtPositionTypeOrgId(debtPositionTypeOrgId);
      filters.setDebtPositionOrigins(
        Arrays.stream(DebtPositionOriginsEnum.values()).filter(
            o -> !DebtPositionOriginsEnum.SECONDARY_ORG.equals(o))
          .toList());
    }

    return new PaidExportFileRequestDTO()
      .organizationId(organizationId)
      .exportFileType(PaidExportFileRequestDTO.ExportFileTypeEnum.PAID)
      .fileVersion(paidExportRequestDTO.getFileVersion())
      .filterFields(filters);
  }
}
