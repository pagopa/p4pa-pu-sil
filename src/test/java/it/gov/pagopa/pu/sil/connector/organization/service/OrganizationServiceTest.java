package it.gov.pagopa.pu.sil.connector.organization.service;

import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.organization.client.OrganizationSearchClient;
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
class OrganizationServiceTest {

  @Mock
  private OrganizationSearchClient organizationSearchClientMock;

  private OrganizationService organizationService;

  private final String accessToken = "ACCESSTOKEN";

  @BeforeEach
  void init() {
    organizationService = new OrganizationServiceImpl(
      organizationSearchClientMock
    );
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      organizationSearchClientMock
    );
  }

  //region getOrganizationByFiscalCode tests
  @Test
  void givenNotExistentFiscalCodeWhenGetOrganizationByFiscalCodeThenEmpty() {
    // Given
    String orgFiscalCode = "ORGFISCALCODE";
    Mockito.when(organizationSearchClientMock.findByOrgFiscalCode(orgFiscalCode, accessToken))
      .thenReturn(null);

    // When
    Optional<Organization> result = organizationService.getOrganizationByFiscalCode(orgFiscalCode, accessToken);

    // Then
    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void givenExistentFiscalCodeWhenGetOrganizationByFiscalCodeThenEmpty() {
    // Given
    String orgFiscalCode = "ORGFISCALCODE";
    Organization expectedResult = new Organization();
    Mockito.when(organizationSearchClientMock.findByOrgFiscalCode(orgFiscalCode, accessToken))
      .thenReturn(expectedResult);

    // When
    Optional<Organization> result = organizationService.getOrganizationByFiscalCode(orgFiscalCode, accessToken);

    // Then
    Assertions.assertTrue(result.isPresent());
    Assertions.assertSame(expectedResult, result.get());
  }
//endregion

  //region getOrganizationByIpaCode tests
  @Test
  void givenNotExistentFiscalCodeWhenGetOrganizationByIpaCodeThenEmpty() {
    // Given
    String orgIpaCode = "ORGIPACODE";
    Mockito.when(organizationSearchClientMock.findByIpaCode(orgIpaCode, accessToken))
      .thenReturn(null);

    // When
    Optional<Organization> result = organizationService.getOrganizationByIpaCode(orgIpaCode, accessToken);

    // Then
    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void givenExistentFiscalCodeWhenGetOrganizationByIpaCodeThenEmpty() {
    // Given
    String orgIpaCode = "ORGIPACODE";
    Organization expectedResult = new Organization();
    Mockito.when(organizationSearchClientMock.findByIpaCode(orgIpaCode, accessToken))
      .thenReturn(expectedResult);

    // When
    Optional<Organization> result = organizationService.getOrganizationByIpaCode(orgIpaCode, accessToken);

    // Then
    Assertions.assertTrue(result.isPresent());
    Assertions.assertSame(expectedResult, result.get());
  }
//endregion

  @Test
  void givenNotExistentOrgIdWhenGetOrganizationByIdThenEmpty() {
    // Given
    Long orgId = 1L;
    Mockito.when(organizationSearchClientMock.findByOrganizationId(orgId, accessToken))
      .thenReturn(null);

    // When
    Optional<Organization> result = organizationService.getOrganizationById(orgId, accessToken);

    // Then
    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void givenExistentOrgIdWhenGetOrganizationByIdThenEmpty() {
    // Given
    Long orgId = 1L;
    Organization expectedResult = new Organization();
    Mockito.when(organizationSearchClientMock.findByOrganizationId(orgId, accessToken))
      .thenReturn(expectedResult);

    // When
    Optional<Organization> result = organizationService.getOrganizationById(orgId, accessToken);

    // Then
    Assertions.assertTrue(result.isPresent());
    Assertions.assertSame(expectedResult, result.get());
  }
}
