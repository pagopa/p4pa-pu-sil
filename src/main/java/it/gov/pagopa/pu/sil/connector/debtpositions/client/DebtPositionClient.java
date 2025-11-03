package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Slf4j
public class DebtPositionClient {

  private final DebtPositionsApisHolder debtPositionsApisHolder;

  public DebtPositionClient(DebtPositionsApisHolder debtPositionsApisHolder) {
    this.debtPositionsApisHolder = debtPositionsApisHolder;
  }

  public ResponseEntity<DebtPositionDTO> createMixedDebtPosition(MixedDebtPositionDTO mixedDebtPositionDTO, String accessToken) {
    return debtPositionsApisHolder
      .getDebtPositionApi(accessToken)
      .createMixedDebtPositionWithHttpInfo(mixedDebtPositionDTO);
  }

  public ResponseEntity<DebtPositionDTO> createDebtPosition(DebtPositionDTO debtPositionDTO, String accessToken) {
    return debtPositionsApisHolder
      .getDebtPositionApi(accessToken)
      .createDebtPositionWithHttpInfo(debtPositionDTO, false);
  }

  public ResponseEntity<DebtPositionDTO> manageDebtPositionInstallments(Long debtPositionId, ManageDebtPositionDTO manageDebtPositionDTO, String accessToken) {
    return debtPositionsApisHolder
      .getDebtPositionApi(accessToken)
      .manageDebtPositionInstallmentsWithHttpInfo(debtPositionId, manageDebtPositionDTO);
  }

  public DebtPositionDTO getDebtPositionDTOByInstallmentId(Long installmentId, String accessToken) {
    try {
      return debtPositionsApisHolder
        .getDebtPositionApi(accessToken)
        .getDebtPositionByInstallmentId(installmentId);
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Cannot find DebtPositionDTO having installmentId[{}]", installmentId, e);
      return null;
    }
  }

  public List<DebtPositionDTO> getDebtPositionsByOrganizationIdAndIuv(Long organizationId, String iuv, List<DebtPositionOrigin> debtPositionOrigin, String accessToken) {
    return debtPositionsApisHolder
      .getDebtPositionApi(accessToken)
      .getDebtPositionsByOrganizationIdAndIuv(organizationId, iuv, debtPositionOrigin);
  }

  public List<DebtPositionDTO> getDebtPositionsByOrganizationIdAndIud(Long organizationId, String iud, List<DebtPositionOrigin> debtPositionOrigin, String accessToken) {
    return debtPositionsApisHolder
      .getDebtPositionApi(accessToken)
      .getDebtPositionsByOrganizationIdAndIud(organizationId, iud, debtPositionOrigin);
  }

  public DebtPosition getDebtPositionByInstallmentId(Long installmentId, String accessToken){
    try{
      return debtPositionsApisHolder.getDebtPositionSearchControllerApi(accessToken)
        .crudDebtPositionsFindByInstallmentId(installmentId);
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Cannot find DebtPosition having installmentId[{}]", installmentId, e);
      return null;
    }
  }

  public List<DebtPositionDTO> getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
    String debtorFiscalCode,
    PersonEntityType debtorEntityType,
    Long organizationId,
    List<String> debtPositionTypeOrgCodesToExclude,
    InstallmentStatus status,
    OffsetDateTime fromDate,
    OffsetDateTime toDate,
    String accessToken
  ) {
    return debtPositionsApisHolder
      .getDebtPositionApi(accessToken)
      .getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
        debtorFiscalCode,
        debtorEntityType,
        status != null ? List.of(status) : null,
        null,
        debtPositionTypeOrgCodesToExclude,
        organizationId,
        fromDate,
        toDate
      );
  }
}
