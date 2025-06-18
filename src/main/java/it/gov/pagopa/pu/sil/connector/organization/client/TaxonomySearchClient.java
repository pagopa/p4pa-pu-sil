package it.gov.pagopa.pu.sil.connector.organization.client;

import it.gov.pagopa.pu.organization.dto.generated.Taxonomy;
import it.gov.pagopa.pu.sil.connector.organization.config.OrganizationApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Service
public class TaxonomySearchClient {

  private final OrganizationApisHolder organizationApisHolder;

  public TaxonomySearchClient(OrganizationApisHolder organizationApisHolder) {
    this.organizationApisHolder = organizationApisHolder;
  }

  public Taxonomy findByTaxonomyCode(String taxonomyCode, String accessToken) {
    try{
      return organizationApisHolder.getTaxonomyCodeDtoSearchControllerApi(accessToken)
        .crudTaxonomiesFindByTaxonomyCode(taxonomyCode);
    } catch (HttpClientErrorException.NotFound e){
      log.info("Cannot find Taxonomy having taxonomyCode {}", taxonomyCode);
      return null;
    }
  }
}
