package it.gov.pagopa.pu.sil.service.singleimport;

import it.gov.pagopa.pu.debtpositions.dto.generated.Action;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.ManageDebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.mapper.DebtPositionMapper;
import it.gov.pagopa.pu.sil.service.debtposition.ManageDebtPositionService;
import it.gov.pagopa.pu.sil.service.notice.NoticeService;
import jakarta.activation.DataHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DebtPositionCreationService extends BaseDebtPositionHandler<it.gov.pagopa.pu.sil.dto.generated.DebtPositionDTO, DebtPositionDTO> {
  private final DebtPositionMapper debtPositionMapper;

  public DebtPositionCreationService(OrganizationService organizationService,
                                     DebtPositionService debtPositionService,
                                     ManageDebtPositionService manageDebtPositionService,
                                     NoticeService noticeService,
                                     DebtPositionMapper debtPositionMapper) {
    super(organizationService, debtPositionService, manageDebtPositionService, noticeService);
    this.debtPositionMapper = debtPositionMapper;
  }

  @Override
  protected Pair<DebtPositionDTO, String> mapRequestToDebtPosition(it.gov.pagopa.pu.sil.dto.generated.DebtPositionDTO request, Organization organization, String accessToken) {
    DebtPositionDTO mappedDebtPositionDTO = debtPositionMapper.mapRequestToDebtPosition(request, organization, accessToken);
    return Pair.of(mappedDebtPositionDTO, Action.I.getValue());
  }

  @Override
  protected DebtPositionDTO mapToRespone(DebtPositionDTO debtPosition, String orgFiscalCode, DataHandler notice, String action) {
    return debtPosition;
  }

  @Override
  protected ManageDebtPositionDTO mapToManageDebtPositionDTO(DebtPositionDTO debtPositionOnDb, InstallmentDTO installmentToSync, String action) {
    // useless in this context
    return null;
  }
}
