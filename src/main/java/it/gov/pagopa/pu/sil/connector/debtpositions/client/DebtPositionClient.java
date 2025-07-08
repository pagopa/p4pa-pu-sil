package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@Slf4j
public class DebtPositionClient {

  private final DebtPositionsApisHolder debtPositionsApisHolder;

  public DebtPositionClient(DebtPositionsApisHolder debtPositionsApisHolder) {
    this.debtPositionsApisHolder = debtPositionsApisHolder;
  }

  public DebtPositionTypeOrg getDebtPositionTypeOrgByOrganizationIdAndCode(Long organizationId, String debtPositionTypeOrgCode, String accessToken) {
    try {
      return debtPositionsApisHolder
        .getDebtPositionTypeOrgSearchControllerApi(accessToken)
        .crudDebtPositionTypeOrgsFindByOrganizationIdAndCode(organizationId, debtPositionTypeOrgCode);
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Cannot find DeptPositionTypeOrg having orgId[{}] and code[{}]", organizationId, debtPositionTypeOrgCode, e);
      return null;
    }
  }

  public DebtPositionType getDebtPositionTypeById(Long debtPositionType, String accessToken) {
    try {
      return debtPositionsApisHolder
        .getDebtPositionTypeEntityControllerApi(accessToken)
        .crudGetDebtpositiontype(String.valueOf(debtPositionType));
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Cannot find DeptPositionType having id[{}]", debtPositionType, e);
      return null;
    }
  }

  public Long countExistingInstallmentsByIudIuvNav(Long organizationId, String iud, String iuv, String nav, String accessToken) {
    return debtPositionsApisHolder
      .getInstallmentNoPiiSearchControllerApi(accessToken)
      .crudInstallmentsCountExistingInstallments(organizationId, iud, iuv, nav);
  }

  public ResponseEntity<DebtPositionDTO> createDebtPosition(DebtPositionDTO debtPositionDTO, String accessToken) {
    return debtPositionsApisHolder
      .getDebtPositionApi(accessToken)
      .createDebtPositionWithHttpInfo(debtPositionDTO, false);
  }

  public List<InstallmentDTO> getInstallmentsByOrganizationIdAndNav(Long organizationId, String nav, List<DebtPositionOrigin> debtPositionOrigin, String accessToken) {
    return debtPositionsApisHolder
      .getInstallmentApi(accessToken)
      .getInstallmentsByOrganizationIdAndNav(organizationId, nav, debtPositionOrigin);
  }

  public DebtPositionDTO getDebtPositionByInstallmentId(Long installmentId, String accessToken) {
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

  public ReceiptDTO getReceiptById(Long receiptId, String accessToken) {
    try {
      return debtPositionsApisHolder
        .getReceiptApi(accessToken)
        .getReceipt(receiptId);
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Cannot find ReceiptDTO having receiptId[{}]", receiptId, e);
      return null;
    }
  }

  public InstallmentNoPII findAuthorizedByTransferSemanticKey(
      Long organizationId,
      String iuv,
      String iur,
      int transferIndex,
      String operatorExternalUserId,
      String accessToken) {
    try {
      return debtPositionsApisHolder
        .getInstallmentNoPiiSearchControllerApi(accessToken)
        .crudInstallmentsFindAuthorizedByTransferSemanticKey(organizationId, iuv, iur, String.valueOf(transferIndex), operatorExternalUserId);
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Cannot find InstallmentNoPII by semantic key", e);
      return null;
    }
  }
}
