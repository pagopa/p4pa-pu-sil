package it.gov.pagopa.pu.sil.connector.organization.config;

import it.gov.pagopa.pu.organization.client.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationErrorDTO;
import it.gov.pagopa.pu.organization.generated.ApiClient;
import it.gov.pagopa.pu.organization.generated.BaseApi;
import it.gov.pagopa.pu.sil.config.rest.HttpClientErrorJsonBodyHandler;
import it.gov.pagopa.pu.sil.connector.organization.mapper.OrganizationErrorDTOMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OrganizationApisHolder {

  private final OrganizationSearchControllerApi organizationSearchControllerApi;
  private final OrganizationEntityControllerApi organizationEntityControllerApi;
  private final TaxonomySearchControllerApi taxonomySearchControllerApi;
  private final BrokerEntityControllerApi brokerEntityControllerApi;
  private final BrokerSearchControllerApi brokerSearchControllerApi;
  private final OrganizationSilServiceApi organizationSilServiceApi;

  private final ThreadLocal<String> bearerTokenHolder = new ThreadLocal<>();

  public OrganizationApisHolder(
    OrganizationApiClientConfig clientConfig,
    RestTemplateBuilder restTemplateBuilder,
    JsonMapper jsonMapper
  ) {
    RestTemplate restTemplate = restTemplateBuilder.build();
    ApiClient apiClient = new ApiClient(restTemplate);
    apiClient.setBasePath(clientConfig.getBaseUrl());
    apiClient.setBearerToken(bearerTokenHolder::get);
    apiClient.setMaxAttemptsForRetry(Math.max(1, clientConfig.getMaxAttempts()));
    apiClient.setWaitTimeMillis(clientConfig.getWaitTimeMillis());
    restTemplate.setErrorHandler(new HttpClientErrorJsonBodyHandler<>(jsonMapper, "ORGANIZATION", clientConfig.isPrintBodyWhenError(),
      OrganizationErrorDTO.class, OrganizationErrorDTOMapper::map));

    this.organizationSilServiceApi = new OrganizationSilServiceApi(apiClient);
    this.organizationSearchControllerApi = new OrganizationSearchControllerApi(apiClient);
    this.organizationEntityControllerApi = new OrganizationEntityControllerApi(apiClient);
    this.taxonomySearchControllerApi = new TaxonomySearchControllerApi(apiClient);
    this.brokerEntityControllerApi = new BrokerEntityControllerApi(apiClient);
    this.brokerSearchControllerApi = new BrokerSearchControllerApi(apiClient);
  }

  @PreDestroy
  public void unload() {
    bearerTokenHolder.remove();
  }

  /**
   * It will return a {@link OrganizationSearchControllerApi} instrumented with the provided accessToken. Use null if auth is not required
   */
  public OrganizationSearchControllerApi getOrganizationSearchControllerApi(String accessToken) {
    return getApi(accessToken, organizationSearchControllerApi);
  }

  public OrganizationEntityControllerApi getOrganizationEntityControllerApi(String accessToken) {
    return getApi(accessToken, organizationEntityControllerApi);
  }

  public TaxonomySearchControllerApi getTaxonomyCodeDtoSearchControllerApi(String accessToken) {
    return getApi(accessToken, taxonomySearchControllerApi);
  }

  public BrokerEntityControllerApi getBrokerEntityControllerApi(String accessToken) {
    return getApi(accessToken, brokerEntityControllerApi);
  }

  public BrokerSearchControllerApi getBrokerSearchControllerApi(String accessToken) {
    return getApi(accessToken, brokerSearchControllerApi);
  }

  public OrganizationSilServiceApi getOrganizationSilServiceApi(String accessToken) {
    return getApi(accessToken, organizationSilServiceApi);
  }

  private <T extends BaseApi> T getApi(String accessToken, T api) {
    bearerTokenHolder.set(accessToken);
    return api;
  }
}
