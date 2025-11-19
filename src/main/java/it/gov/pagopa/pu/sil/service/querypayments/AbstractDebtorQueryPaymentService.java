package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static it.gov.pagopa.pu.sil.util.Constants.EXCLUDED_DEBT_POSITION_TYPE_CODES;

/**
 * Abstract base class for PaaSIL query payment services.
 * Encapsulates common business logic for services that query payment information by debtor fiscal code.
 * This class handles authorization, organization resolution, and debt position retrieval.
 *
 * @param <I>  the request type parameter
 * @param <O> the response type parameter
 */
@Slf4j
public abstract class AbstractDebtorQueryPaymentService<I, O> {
  private final String fileShareBaseUrl;
  protected final DebtPositionService debtPositionService;
  protected final OrganizationService organizationService;

  protected AbstractDebtorQueryPaymentService(@Value("${public-base-url.fileshare}") String fileShareBaseUrl,
                                              DebtPositionService debtPositionService,
                                              OrganizationService organizationService) {
    this.fileShareBaseUrl = fileShareBaseUrl;
    this.debtPositionService = debtPositionService;
    this.organizationService = organizationService;
  }

  /**
   * Template method that orchestrates the common business logic for processing requests.
   * Subclasses should implement the abstract methods to customize behavior for specific operations.
   *
   * @param request     the incoming SOAP request
   * @param userInfo    the authenticated user information
   * @param accessToken the access token for calling downstream services
   * @return the response object
   */
  public final O processRequest(I request, UserInfo userInfo, String accessToken) {
    // Validate authorization
    AuthorizationService.validateBrokerAdminRole(userInfo);

    // Resolve organizations
    List<Organization> organizations = resolveOrganizations(request, userInfo, accessToken);

    // Create response object
    O response = createResponse();

    // Process organizations and debt positions
    organizations.forEach(organization ->
      processByOrganization(request, organization, accessToken, response)
    );

    return response;
  }

  /**
   * Resolves the list of organizations based on the request.
   * If a specific organization code is provided, validates and returns that organization.
   * Otherwise, returns all active organizations for the broker.
   *
   * @param request     the incoming request
   * @param userInfo    the authenticated user information
   * @param accessToken the access token for calling downstream services
   * @return the list of organizations to process
   */
  protected List<Organization> resolveOrganizations(I request, UserInfo userInfo, String accessToken) {
    String codIpaEnte = getCodIpaEnte(request);

    if (codIpaEnte != null) {
      Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, codIpaEnte);
      Organization organization = organizationService.getOrganizationById(organizationId, accessToken)
        .filter(o -> OrganizationStatus.ACTIVE.equals(o.getStatus()))
        .orElseThrow(() -> new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "L'ente non è valido o non è abilitato"));
      AuthorizationService.isOrganizationHandledByBroker(organization.getBrokerId(), userInfo);
      return List.of(organization);
    } else {
      return organizationService.findByBrokerIdAndStatus(userInfo.getBrokerId(), OrganizationStatus.ACTIVE, accessToken);
    }
  }

  /**
   * Fetches debt positions based on the debtor information and date filters from the request.
   *
   * @param request         the incoming request
   * @param organizationIds the list of organization IDs to filter by
   * @param accessToken     the access token for calling downstream services
   * @return the list of debt positions matching the criteria
   */
  protected List<DebtPositionDTO> fetchDebtPositions(I request, List<Long> organizationIds, String accessToken) {
    OffsetDateTimeIntervalFilter dateFilter = getDateFilter(request);

    return debtPositionService.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
      getDebtorFiscalCode(request),
      getDebtorEntityType(request),
      organizationIds,
      EXCLUDED_DEBT_POSITION_TYPE_CODES,
      getInstallmentStatus(),
      dateFilter,
      accessToken
    );
  }

  /**
   * Processes a single organization with its debt positions.
   * Fetches the debt positions for the organization and delegates to the subclass
   * implementation to populate the response.
   *
   * @param request      the incoming request
   * @param organization the organization to process
   * @param accessToken  the access token for calling downstream services
   * @param response     the response object to populate
   */
  protected void processByOrganization(I request, Organization organization,
                                       String accessToken, O response) {
    List<DebtPositionDTO> debtPositions = fetchDebtPositions(request, List.of(organization.getOrganizationId()), accessToken);
    gatherToResponse(request, organization, debtPositions, accessToken, response);
  }

  /**
   * Composes the receipt download URL for a given organization and receipt ID.
   *
   * @param organizationId the organization ID
   * @param receiptId      the receipt ID
   * @return the composed URL to download the receipt PDF
   */
  protected String composeReceiptDownloadUrl(Long organizationId, Long receiptId) {
    return UriComponentsBuilder.fromUriString(fileShareBaseUrl)
      .path("/organization/{organizationId}/rt/{receiptId}")
      .buildAndExpand(organizationId, receiptId)
      .toUriString();
  }

  // @TODO: da implementare con la P4ADEV-4043
  protected String composeCheckoutUrl(String checkoutCart) {
    return checkoutCart;
  }

  protected abstract String getCodIpaEnte(I request);
  protected abstract String getDebtorFiscalCode(I request);
  protected abstract PersonEntityType getDebtorEntityType(I request);
  protected abstract InstallmentStatus getInstallmentStatus();
  protected abstract OffsetDateTimeIntervalFilter getDateFilter(I request);
  protected abstract O createResponse();
  protected abstract void gatherToResponse(I request, Organization organization,
                                           List<DebtPositionDTO> debtPositions, String accessToken, O response);
}

