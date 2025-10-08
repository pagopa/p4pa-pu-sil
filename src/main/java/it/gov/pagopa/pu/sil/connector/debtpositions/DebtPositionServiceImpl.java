package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.sil.connector.debtpositions.client.DebtPositionClient;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtIdentificativoUnivocoPersonaFG;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DebtPositionServiceImpl implements DebtPositionService {

  public static final String HEADER_X_WORKFLOW_ID = "x-workflow-id";

  private final DebtPositionClient client;

  public DebtPositionServiceImpl(DebtPositionClient client) {
    this.client = client;
  }

  @Override
  public Pair<DebtPositionDTO, String> createMixedDebtPosition(MixedDebtPositionDTO mixedDebtPositionDTO, String accessToken) {
    ResponseEntity<DebtPositionDTO> responseEntity = client.createMixedDebtPosition(mixedDebtPositionDTO, accessToken);
    return Pair.of(responseEntity.getBody(), responseEntity.getHeaders().getFirst(HEADER_X_WORKFLOW_ID));
  }

  @Override
  public Pair<DebtPositionDTO, String> createDebtPosition(DebtPositionDTO debtPositionDTO, String accessToken) {
    ResponseEntity<DebtPositionDTO> responseEntity = client.createDebtPosition(debtPositionDTO, accessToken);
    return Pair.of(responseEntity.getBody(), responseEntity.getHeaders().getFirst(HEADER_X_WORKFLOW_ID));
  }

  @Override
  public Pair<DebtPositionDTO, String> manageDebtPositionInstallments(Long debtPositionId, ManageDebtPositionDTO manageDebtPositionDTO, String accessToken) {
    ResponseEntity<DebtPositionDTO> responseEntity = client.manageDebtPositionInstallments(debtPositionId, manageDebtPositionDTO, accessToken);
    return Pair.of(responseEntity.getBody(), responseEntity.getHeaders().getFirst(HEADER_X_WORKFLOW_ID));
  }

  @Override
  public DebtPositionDTO getDebtPositionDTOByInstallmentId(Long installmentId, String accessToken) {
    return client.getDebtPositionDTOByInstallmentId(installmentId, accessToken);
  }

  @Override
  public List<DebtPositionDTO> getDebtPositionsByOrganizationIdAndIuv(Long organizationId, String iuv, List<DebtPositionOrigin> debtPositionOrigin, String accessToken) {
    return client.getDebtPositionsByOrganizationIdAndIuv(organizationId, iuv, debtPositionOrigin, accessToken);
  }

  @Override
  public List<DebtPositionDTO> getDebtPositionsByOrganizationIdAndIud(Long organizationId, String iud, List<DebtPositionOrigin> debtPositionOrigin, String accessToken) {
    return client.getDebtPositionsByOrganizationIdAndIud(organizationId, iud, debtPositionOrigin, accessToken);
  }

  @Override
  public DebtPosition getDebtPositionByInstallmentId(Long installmentId, String accessToken) {
    return client.getDebtPositionByInstallmentId(installmentId, accessToken);
  }

  @Override
  public List<DebtPositionDTO> getDebtPositionsByIdentificativoUnivocoPersonaFGAndOrganizationId(CtIdentificativoUnivocoPersonaFG identificativoUnivocoPersonaFG, Long organizationId, String accessToken) {
    return client.getDebtPositionsByIdentificativoUnivocoPersonaFGAndOrganizationId(identificativoUnivocoPersonaFG, organizationId, accessToken);
  }
}
