package it.gov.pagopa.pu.sil.connector.organization.client;

import it.gov.pagopa.pu.organization.client.generated.TaxonomySearchControllerApi;
import it.gov.pagopa.pu.organization.dto.generated.Taxonomy;
import it.gov.pagopa.pu.sil.connector.organization.config.OrganizationApisHolder;
import it.gov.pagopa.pu.sil.exception.common.RestInvokeNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.mockito.Mockito.when;

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

        when(organizationApisHolderMock.getTaxonomyCodeDtoSearchControllerApi(accessToken))
                .thenReturn(taxonomySearchControllerApiMock);
        when(taxonomySearchControllerApiMock.crudTaxonomiesFindByTaxonomyCode(taxonomyCode))
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

    when(organizationApisHolderMock.getTaxonomyCodeDtoSearchControllerApi(accessToken))
      .thenReturn(taxonomySearchControllerApiMock);
    when(taxonomySearchControllerApiMock.crudTaxonomiesFindByTaxonomyCode(taxonomyCode))
      .thenThrow(new RestInvokeNotFoundException("APPNAME", HttpStatus.NOT_FOUND, "ERROR", "ERRORCODE", "ERRORMESSAGE"));

    // When
    Taxonomy result = taxonomySearchClient.findByTaxonomyCode(taxonomyCode, accessToken);

    // Then
    Assertions.assertNull(result);
  }
}
