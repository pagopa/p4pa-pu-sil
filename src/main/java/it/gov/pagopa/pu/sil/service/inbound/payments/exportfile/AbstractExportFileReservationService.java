package it.gov.pagopa.pu.sil.service.inbound.payments.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractExportFileReservationService {
    public static final String IDENTIFICATIVO_TIPO_DOVUTO_SECONDARIO = "SECONDARIO";

    protected final DebtPositionTypeService debtPositionTypeService;

    protected AbstractExportFileReservationService(DebtPositionTypeService debtPositionTypeService) {
        this.debtPositionTypeService = debtPositionTypeService;
    }

    protected Long getOrganizationIdFromUserInfo(UserInfo userInfo, String orgIpaCode) {
        return AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);
    }

    protected DebtPositionTypeOrg getAndValidateDebtPositionTypeOrg(Long organizationId, String debtPositionTypeOrgCode, String accessToken) {
        DebtPositionTypeOrg debtPositionTypeOrg = debtPositionTypeService.getDebtPositionTypeOrgByOrgIdAndType(
                organizationId, debtPositionTypeOrgCode, accessToken);
        if (debtPositionTypeOrg == null) {
          throw new ExportFileServiceException(
            ErrorCodeConstants.ERROR_CODE_INVALID_DEBT_POSITION_TYPE_ORG_CODE,
            "Cannot find debtPositionTypeOrg having code " + debtPositionTypeOrgCode + " for organizationId: " + organizationId);
        } else if (Boolean.FALSE.equals(debtPositionTypeOrg.getFlagActive())) {
            throw new ExportFileServiceException(
              ErrorCodeConstants.ERROR_CODE_INVALID_DEBT_POSITION_TYPE_ORG_STATUS,
              "DebtPositionTypeOrg not enabled: " + debtPositionTypeOrgCode);
        }
        return debtPositionTypeOrg;
    }
}

