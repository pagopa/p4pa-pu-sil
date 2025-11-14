package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface DebtPositionService {

  /**
   * Creates a new mixed debt position.
   *
   * @param mixedDebtPositionDTO the DTO containing the details of the mixed debt position to create
   * @param accessToken the access token for authentication
   * @return a pair containing the created DebtPositionDTO and the workflow ID from the response headers
   */
  Pair<DebtPositionDTO, String> createMixedDebtPosition(MixedDebtPositionDTO mixedDebtPositionDTO, String accessToken);

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

  /**
   * Retrieves a DebtPositionDTO by its installment ID.
   *
   * @param installmentId the ID of the installment
   * @param accessToken the access token for authentication
   * @return the DebtPositionDTO associated with the given installment ID
   */
  DebtPositionDTO getDebtPositionDTOByInstallmentId(Long installmentId, String accessToken);

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

  DebtPosition getDebtPositionByInstallmentId(Long installmentId, String accessToken);

  /**
   * Retrieves a list of DebtPositionDTOs by debtor fiscal code and debtor entity type.
   *
   * @param debtorFiscalCode the fiscal code of the debtor
   * @param debtorEntityType the entity type of the debtor
   * @param organizationIds the IDs of the organizations
   * @param status the status of the installments to filter by
   * @param dateFilter the date interval filter (containing start and end dates) for filtering debt positions (inclusive)
   * @param accessToken the access token for authentication
   * @return a list of DebtPositionDTOs matching the criteria
   */
  List<DebtPositionDTO> getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
    String debtorFiscalCode,
    PersonEntityType debtorEntityType,
    List<Long> organizationIds,
    List<String> debtPositionTypeOrgCodesToExclude,
    InstallmentStatus status,
    OffsetDateTimeIntervalFilter dateFilter,
    String accessToken
  );
}
