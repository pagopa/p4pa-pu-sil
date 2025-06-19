package it.gov.pagopa.pu.sil.connector.organization.client;

import it.gov.pagopa.pu.organization.client.generated.TaxonomySearchControllerApi;
import it.gov.pagopa.pu.organization.dto.generated.Taxonomy;
import it.gov.pagopa.pu.sil.connector.organization.config.OrganizationApisHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class TaxonomySearchClientTest {

    @Mock
    private OrganizationApisHolder organizationApisHolderMock;
    @Mock
    private TaxonomySearchControllerApi taxonomySearchControllerApiMock;

    private TaxonomySearchClient taxonomySearchClient;

    @BeforeEach
    void setUp() {
        taxonomySearchClient = new TaxonomySearchClient(organizationApisHolderMock);
    }

    @AfterEach
    void verifyNoMoreInteractions(){
        Mockito.verifyNoMoreInteractions(
          organizationApisHolderMock,
          taxonomySearchControllerApiMock
        );
    }

    @Test
    void whenFindByTaxonomyCodeThenInvokeWithAccessToken(){
        // Given
        String accessToken = "ACCESSTOKEN";
        String taxonomyCode = "TAXONOMYCODE";
        Taxonomy expectedResult = new Taxonomy();

        Mockito.when(organizationApisHolderMock.getTaxonomyCodeDtoSearchControllerApi(accessToken))
                .thenReturn(taxonomySearchControllerApiMock);
        Mockito.when(taxonomySearchControllerApiMock.crudTaxonomiesFindByTaxonomyCode(taxonomyCode))
                .thenReturn(expectedResult);

        // When
        Taxonomy result = taxonomySearchClient.findByTaxonomyCode(taxonomyCode, accessToken);

        // Then
        Assertions.assertSame(expectedResult, result);
    }

  @Test
  void givenNotExistentTaxonomyCodeWhenFindByTaxonomyCodeThenNull(){
    // Given
    String accessToken = "ACCESSTOKEN";
    String taxonomyCode = "TAXONOMYCODE";

    Mockito.when(organizationApisHolderMock.getTaxonomyCodeDtoSearchControllerApi(accessToken))
      .thenReturn(taxonomySearchControllerApiMock);
    Mockito.when(taxonomySearchControllerApiMock.crudTaxonomiesFindByTaxonomyCode(taxonomyCode))
      .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "NotFound", null, null, null));

    // When
    Taxonomy result = taxonomySearchClient.findByTaxonomyCode(taxonomyCode, accessToken);

    // Then
    Assertions.assertNull(result);
  }
}
