package it.gov.pagopa.pu.sil.connector.organization.service;

import it.gov.pagopa.pu.organization.dto.generated.Taxonomy;
import it.gov.pagopa.pu.sil.connector.organization.client.TaxonomySearchClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class TaxonomyServiceTest {

    @Mock
    private TaxonomySearchClient taxonomySearchClient;

    private TaxonomyService taxonomyService;

    private final String accessToken = "ACCESSTOKEN";

    @BeforeEach
    void init(){
        taxonomyService = new TaxonomyServiceImpl(taxonomySearchClient);
    }

    @AfterEach
    void verifyNoMoreInteractions(){
        Mockito.verifyNoMoreInteractions(
          taxonomySearchClient
        );
    }

    @Test
    void givenNotExistentFiscalCodeWhenGetOrganizationByFiscalCodeThenEmpty(){
        // Given
        String taxonomyCode = "TAXONOMYCODE";
        Mockito.when(taxonomySearchClient.findByTaxonomyCode(taxonomyCode, accessToken))
                .thenReturn(null);

        // When
        Optional<Taxonomy> result = taxonomyService.getTaxonomyByTaxonomyCode(taxonomyCode, accessToken);

        // Then
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void givenExistentFiscalCodeWhenGetOrganizationByFiscalCodeThenEmpty(){
        // Given
        String taxonomyCode = "TAXONOMYCODE";
        Taxonomy expectedResult = new Taxonomy();
        Mockito.when(taxonomySearchClient.findByTaxonomyCode(taxonomyCode, accessToken))
                .thenReturn(expectedResult);

        // When
        Optional<Taxonomy> result = taxonomyService.getTaxonomyByTaxonomyCode(taxonomyCode, accessToken);

        // Then
        Assertions.assertTrue(result.isPresent());
        Assertions.assertSame(expectedResult, result.get());
    }
}
