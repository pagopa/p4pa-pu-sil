package it.gov.pagopa.pu.sil.service.singleimport;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.mapper.DebtPositionMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.debtposition.ManageDebtPositionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DebtPositionCreationService {
  private final OrganizationService organizationService;
  private final DebtPositionMapper debtPositionMapper;
  private final ManageDebtPositionService manageDebtPositionService;

  public DebtPositionCreationService(OrganizationService organizationService,
                                     DebtPositionMapper debtPositionMapper,
                                     ManageDebtPositionService manageDebtPositionService) {
    this.organizationService = organizationService;
    this.debtPositionMapper = debtPositionMapper;
    this.manageDebtPositionService = manageDebtPositionService;
  }

  public Triple<DebtPositionDTO, String, RegistryOutcome> handleInsert(
      it.gov.pagopa.pu.sil.dto.generated.DebtPositionDTO request,
      String orgIpaCode,
      UserInfo userInfo,
      String accessToken) {

    AuthorizationService.validateAdminRole(orgIpaCode, userInfo);
    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);

    Organization organization = organizationService.getOrganizationById(organizationId, accessToken)
      .orElse(null);
    if (organization == null || !OrganizationStatus.ACTIVE.equals(organization.getStatus())) {
      throw new IllegalArgumentException("L'ente non è valido o non è abilitato");
    }

    DebtPositionDTO mappedDebtPositionDTO = debtPositionMapper.mapRequestToDebtPosition(request, organization, accessToken);
    //create debt position (and wait for the workflow to complete)
    DebtPositionDTO createdDebtPositionDTO = manageDebtPositionService.createSyncedDebtPositions(List.of(mappedDebtPositionDTO), accessToken).getFirst();
    //retrieve the IUV
    String iuv = createdDebtPositionDTO.getPaymentOptions().getFirst().getInstallments().getFirst().getIuv();

    return Triple.of(createdDebtPositionDTO, iuv, RegistryOutcome.OK);
  }
}
