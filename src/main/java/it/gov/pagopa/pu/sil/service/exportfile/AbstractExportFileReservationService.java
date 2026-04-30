package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
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

    protected DebtPositionTypeOrg getAndValidateDebtPositionTypeOrg(Long organizationId, String debtPositionTypeOrgCode, String accessToken, SilFaults invalidFault, SilFaults notEnabledFault) {
        DebtPositionTypeOrg debtPositionTypeOrg = debtPositionTypeService.getDebtPositionTypeOrgByOrgIdAndType(
                organizationId, debtPositionTypeOrgCode, accessToken);
        if (debtPositionTypeOrg == null) {
            throw new ExportFileServiceException(invalidFault, "Tipo dovuto non valido: " + debtPositionTypeOrgCode);
        } else if (Boolean.FALSE.equals(debtPositionTypeOrg.getFlagActive())) {
            throw new ExportFileServiceException(notEnabledFault, "Tipo dovuto non abilitato: " + debtPositionTypeOrgCode);
        }
        return debtPositionTypeOrg;
    }
}

