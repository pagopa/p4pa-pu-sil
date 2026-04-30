package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.LimitedTokenRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.sil.connector.auth.AuthnService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionCheckoutService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
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
  public static final String PU_BFF_APP_NAME = "p4pa-pu-bff";
  public static final String RESOURCE_RECEIPT = "receipt";
  private final String bffBaseUrl;
  protected final DebtPositionService debtPositionService;
  protected final OrganizationService organizationService;
  protected final AuthorizationService authorizationService;
  protected final DebtPositionCheckoutService debtPositionCheckoutService;
  protected final AuthnService authnService;

  protected AbstractDebtorQueryPaymentService(@Value("${public-base-url.bff}") String bffBaseUrl,
                                              DebtPositionService debtPositionService,
                                              OrganizationService organizationService,
                                              AuthorizationService authorizationService,
                                              DebtPositionCheckoutService debtPositionCheckoutService,
                                              AuthnService authnService) {
    this.bffBaseUrl = bffBaseUrl;
    this.debtPositionService = debtPositionService;
    this.organizationService = organizationService;
    this.authorizationService = authorizationService;
    this.debtPositionCheckoutService = debtPositionCheckoutService;
    this.authnService = authnService;
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

    // Transform request
    DebtorQueryPaymentRequest transformedRequest = transformRequest(request);

    // Resolve organizations
    List<Organization> organizations = resolveOrganizations(transformedRequest, userInfo, accessToken);

    // Fetch debt positions for all organizations
    List<DebtPositionDTO> debtPositions = fetchDebtPositions(transformedRequest, organizations, accessToken);

    // Process organizations and gather response
    return gatherToResponse(transformedRequest, organizations, debtPositions, accessToken);
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
  protected List<Organization> resolveOrganizations(DebtorQueryPaymentRequest request, UserInfo userInfo, String accessToken) {
    String ipaCode = request.getIpaCode();

    if (ipaCode != null) {
      Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, ipaCode);
      Organization organization = organizationService.getOrganizationById(organizationId, accessToken)
        .filter(o -> OrganizationStatus.ACTIVE.equals(o.getStatus()))
        .orElseThrow(() -> new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "L'ente non è valido o non è abilitato"));
      AuthorizationService.validateOrganizationBrokered(organization.getBrokerId(), userInfo);
      return List.of(organization);
    } else {
      return organizationService.findByBrokerIdAndStatus(userInfo.getBrokerId(), OrganizationStatus.ACTIVE, accessToken);
    }
  }

  /**
   * Fetches debt positions based on the debtor information and date filters from the request.
   *
   * @param request       the incoming request
   * @param organizations the list of organizations to filter by
   * @param accessToken   the access token for calling downstream services
   * @return the list of debt positions matching the criteria
   */
  protected List<DebtPositionDTO> fetchDebtPositions(DebtorQueryPaymentRequest request, List<Organization> organizations, String accessToken) {
    List<Long> organizationIds = organizations.stream()
      .map(Organization::getOrganizationId)
      .toList();

    return debtPositionService.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
      request.getDebtorFiscalCode(),
      request.getDebtorEntityType(),
      organizationIds,
      EXCLUDED_DEBT_POSITION_TYPE_CODES,
      request.getStatus(),
      request.getDateFilter(),
      accessToken
    );
  }

  /**
   * Composes the receipt download URL for a given organization and receipt ID.
   *
   * @param organizationId the organization ID
   * @param receiptId      the receipt ID
   * @return the composed URL to download the receipt PDF
   */
  protected String composeReceiptDownloadUrl(Long organizationId, String organizationIpaCode, Long receiptId) {
    LimitedTokenRequest limitedTokenRequest = LimitedTokenRequest.builder()
      .organizationId(organizationId)
      .app(PU_BFF_APP_NAME)
      .resource(RESOURCE_RECEIPT)
      .resourceId(receiptId.toString())
      .expireInSeconds(24L * 60 * 60)
      .singleUsage(false)
      .build();
    AccessToken limitedScopeToken = authorizationService.requestLimitedToken(limitedTokenRequest, authnService.getAccessToken(organizationIpaCode));
    return UriComponentsBuilder.fromUriString(bffBaseUrl)
      .path("/organization/{organizationId}/receipts/{receiptId}/pdf")
      .queryParam("token", limitedScopeToken.getAccessToken())
      .buildAndExpand(organizationId, receiptId)
      .toUriString();
  }

  protected String getCheckoutUrl(Long organizationId, String organizationIpaCode, String iuvs, String callbackUrl, String orgFiscalCode) {
    return debtPositionCheckoutService.composeDebtPositionsCheckoutUrl(organizationId, iuvs, callbackUrl, orgFiscalCode, authnService.getAccessToken(organizationIpaCode));
  }

  protected abstract DebtorQueryPaymentRequest transformRequest(I request);
  protected abstract O gatherToResponse(DebtorQueryPaymentRequest request, List<Organization> organizations,
                                        List<DebtPositionDTO> debtPositions, String accessToken);

  /**
   * Request class for debtor query payment operations.
   */
  @Getter
  @AllArgsConstructor
  public static class DebtorQueryPaymentRequest {
    private final String ipaCode;
    private final PersonEntityType debtorEntityType;
    private final String debtorFiscalCode;
    private final InstallmentStatus status;
    private final OffsetDateTime dateFrom;
    private final OffsetDateTime dateTo;

    public OffsetDateTimeIntervalFilter getDateFilter() {
      return new OffsetDateTimeIntervalFilter(dateFrom, dateTo);
    }
  }
}
