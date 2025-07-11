package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface DebtPositionService {
  DebtPositionTypeOrg getDebtPositionTypeOrgByOrgIdAndType(Long organizationId, String debtPositionTypeOrgCode, String accessToken);
  DebtPositionType getDebtPositionTypeById(Long debtPositionType, String accessToken);
  Long countExistingInstallmentsByIudIuvNav(Long organizationId, String iud, String iuv, String nav, String accessToken);
  /**
   * Creates a new debt position.
   *
   * @param debtPositionDTO the DTO containing the details of the debt position to create
   * @param accessToken the access token for authentication
   * @return a pair containing the created DebtPositionDTO and the workflow ID from the response headers
   */
  Pair<DebtPositionDTO, String> createDebtPosition(DebtPositionDTO debtPositionDTO, String accessToken);

  /**
   * Manages the installments of a debt position.
   *
   * @param debtPositionId the ID of the debt position
   * @param manageDebtPositionDTO the DTO containing the details for installments to manage
   * @param accessToken the access token for authentication
   * @return a pair containing the updated DebtPositionDTO and the workflow ID from the response headers
   */
  Pair<DebtPositionDTO, String> manageDebtPositionInstallments(Long debtPositionId, ManageDebtPositionDTO manageDebtPositionDTO, String accessToken);
                                                                 /**
   * Retrieves a list of InstallmentDTOs based on organization ID, NAV, and debt position origins.
   *
   * @param organizationId the ID of the organization
   * @param nav the NAV (Navigation) identifier
   * @param debtPositionOrigin the list of debt position origins to filter by
   * @param accessToken the access token for authentication
   * @return a list of InstallmentDTOs matching the criteria
   */
  List<InstallmentDTO> getInstallmentsByOrganizationIdAndNav(Long organizationId, String nav, List<DebtPositionOrigin> debtPositionOrigin, String accessToken);

  /**
   * Retrieves a DebtPositionDTO by its installment ID.
   *
   * @param installmentId the ID of the installment
   * @param accessToken the access token for authentication
   * @return the DebtPositionDTO associated with the given installment ID
   */
  DebtPositionDTO getDebtPositionByInstallmentId(Long installmentId, String accessToken);

  /**
   * Retrieves a list of DebtPositionDTOs by organization ID and IUV.
   *
   * @param organizationId the ID of the organization
   * @param iuv the IUV (Identificativo Unico di Versamento)
   * @param debtPositionOrigin the list of debt position origins to filter by
   * @param accessToken the access token for authentication
   * @return a list of DebtPositionDTOs matching the criteria
   */
  List<DebtPositionDTO> getDebtPositionsByOrganizationIdAndIuv(Long organizationId, String iuv, List<DebtPositionOrigin> debtPositionOrigin, String accessToken);

  /**
   * Retrieves a list of DebtPositionDTOs by organization ID and IUD.
   *
   * @param organizationId the ID of the organization
   * @param iud the IUD (Identificativo Unico di Debito)
   * @param debtPositionOrigin the list of debt position origins to filter by
   * @param accessToken the access token for authentication
   * @return a list of DebtPositionDTOs matching the criteria
   */
  List<DebtPositionDTO> getDebtPositionsByOrganizationIdAndIud(Long organizationId, String iud, List<DebtPositionOrigin> debtPositionOrigin, String accessToken);

  /**
   * Retrieves a receipt by its ID.
   *
   * @param receiptId the ID of the receipt
   * @param accessToken the access token for authentication
   * @return the ReceiptDTO associated with the given receipt ID
   */
  ReceiptDTO getReceiptById(Long receiptId, String accessToken);

  /**
   * Finds an InstallmentNoPII by transfer semantic key.
   *
   * @param organizationId the ID of the organization
   * @param iuv the IUV (Identificativo Unico di Versamento)
   * @param iur the IUR (Identificativo Unico di Riscossione)
   * @param transferIndex the index of the transfer
   * @param operatorExternalUserId the external user ID of the operator
   * @param accessToken the access token for authentication
   * @return an InstallmentNoPII if found, otherwise null
   */
  InstallmentNoPII findAuthorizedByTransferSemanticKey(
    Long organizationId,
    String iuv,
    String iur,
    int transferIndex,
    String operatorExternalUserId,
    String accessToken);
}
