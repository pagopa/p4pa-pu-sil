package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public abstract class AbstractExportFileReservationService {
    protected final DebtPositionService debtPositionService;

    protected AbstractExportFileReservationService(DebtPositionService debtPositionService) {
        this.debtPositionService = debtPositionService;
    }

    protected void checkAdminRole(String orgIpaCode, UserInfo userInfo) {
        String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);
        if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
            log.error("ClientId [{}] not authorized to call export file for organization {}", clientId, orgIpaCode);
            throw new UnauthorizedException("Utente non autorizzato");
        }
    }

    protected Long getOrganizationIdFromUserInfo(UserInfo userInfo, String orgIpaCode) {
        return AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);
    }

    protected DebtPositionTypeOrg getAndValidateDebtPositionTypeOrg(Long organizationId, String debtPositionTypeOrgCode, String accessToken, SilFaults invalidFault, SilFaults notEnabledFault) {
        DebtPositionTypeOrg debtPositionTypeOrg = debtPositionService.getDebtPositionTypeOrgByOrgIdAndType(
                organizationId, debtPositionTypeOrgCode, accessToken);
        if (debtPositionTypeOrg == null) {
            throw new ExportFileServiceException(invalidFault, "Tipo dovuto non valido: " + debtPositionTypeOrgCode);
        } else if (Boolean.FALSE.equals(debtPositionTypeOrg.getFlagActive())) {
            throw new ExportFileServiceException(notEnabledFault, "Tipo dovuto non abilitato: " + debtPositionTypeOrgCode);
        }
        return debtPositionTypeOrg;
    }
}

