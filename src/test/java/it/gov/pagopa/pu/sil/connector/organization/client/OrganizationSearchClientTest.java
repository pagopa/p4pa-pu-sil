package it.gov.pagopa.pu.sil.connector.organization.client;

import it.gov.pagopa.pu.organization.client.generated.OrganizationEntityControllerApi;
import it.gov.pagopa.pu.organization.client.generated.OrganizationSearchControllerApi;
import it.gov.pagopa.pu.organization.dto.generated.CollectionModelOrganization;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.organization.config.OrganizationApisHolder;
import it.gov.pagopa.pu.sil.exception.common.RestInvokeNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationSearchClientTest {

  @Mock
  private OrganizationApisHolder organizationApisHolderMock;
  @Mock
  private OrganizationSearchControllerApi organizationSearchControllerApiMock;
  @Mock
  private OrganizationEntityControllerApi organizationEntityControllerApiMock;

  private OrganizationSearchClient organizationSearchClient;

  @BeforeEach
  void setUp() {
    organizationSearchClient = new OrganizationSearchClient(organizationApisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      organizationApisHolderMock,
      organizationSearchControllerApiMock,
      organizationEntityControllerApiMock
    );
  }

  //region findByIpaCode test
  @Test
  void whenFindByIpaCodeThenInvokeWithAccessToken() {
    // Given
    String accessToken = "ACCESSTOKEN";
    String orgIpaCode = "ORGIPACODE";
    Organization expectedResult = new Organization();

    when(organizationApisHolderMock.getOrganizationSearchControllerApi(accessToken))
      .thenReturn(organizationSearchControllerApiMock);
    when(organizationSearchControllerApiMock.crudOrganizationsFindByIpaCode(orgIpaCode))
      .thenReturn(expectedResult);

    // When
    Organization result = organizationSearchClient.findByIpaCode(orgIpaCode, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

  @Test
  void givenNotExistentIpaCodeWhenFindByIpaCodeThenNull() {
    // Given
    String accessToken = "ACCESSTOKEN";
    String orgIpaCode = "ORGIPACODE";

    when(organizationApisHolderMock.getOrganizationSearchControllerApi(accessToken))
      .thenReturn(organizationSearchControllerApiMock);
    when(organizationSearchControllerApiMock.crudOrganizationsFindByIpaCode(orgIpaCode))
      .thenThrow(new RestInvokeNotFoundException("APPNAME", HttpStatus.NOT_FOUND, "ERROR", "ERRORCODE", "ERRORMESSAGE"));

    // When
    Organization result = organizationSearchClient.findByIpaCode(orgIpaCode, accessToken);

    // Then
    Assertions.assertNull(result);
  }
//endregion

  //region findByOrgFiscalCode test
  @Test
  void whenGetOrgFiscalCodeThenInvokeWithAccessToken() {
    // Given
    String accessToken = "ACCESSTOKEN";
    String orgFiscalCode = "ORGFISCALCODE";
    Organization expectedResult = new Organization();

    when(organizationApisHolderMock.getOrganizationSearchControllerApi(accessToken))
      .thenReturn(organizationSearchControllerApiMock);
    when(organizationSearchControllerApiMock.crudOrganizationsFindByOrgFiscalCode(orgFiscalCode))
      .thenReturn(expectedResult);

    // When
    Organization result = organizationSearchClient.findByOrgFiscalCode(orgFiscalCode, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

  @Test
  void givenNotExistentOrgFiscalCodeWhenGetOrgFiscalCodeThenNull() {
    // Given
    String accessToken = "ACCESSTOKEN";
    String orgFiscalCode = "ORGFISCALCODE";

    when(organizationApisHolderMock.getOrganizationSearchControllerApi(accessToken))
      .thenReturn(organizationSearchControllerApiMock);
    when(organizationSearchControllerApiMock.crudOrganizationsFindByOrgFiscalCode(orgFiscalCode))
      .thenThrow(new RestInvokeNotFoundException("APPNAME", HttpStatus.NOT_FOUND, "ERROR", "ERRORCODE", "ERRORMESSAGE"));

    // When
    Organization result = organizationSearchClient.findByOrgFiscalCode(orgFiscalCode, accessToken);

    // Then
    Assertions.assertNull(result);
  }
//endregion

  //region findByOrganizationId test
  @Test
  void whenFindByIdThenInvokeWithAccessToken() {
    // Given
    String accessToken = "ACCESSTOKEN";
    String orgId = "1";
    Organization expectedResult = new Organization();

    when(organizationApisHolderMock.getOrganizationEntityControllerApi(accessToken))
      .thenReturn(organizationEntityControllerApiMock);
    when(organizationEntityControllerApiMock.crudGetOrganization(orgId))
      .thenReturn(expectedResult);

    // When
    Organization result = organizationSearchClient.findByOrganizationId(Long.valueOf(orgId), accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

  @Test
  void givenNotExistentOrganizationIdWhenFindByIdThenNull() {
    // Given
    String accessToken = "ACCESSTOKEN";
    String orgId = "1";

    when(organizationApisHolderMock.getOrganizationEntityControllerApi(accessToken))
      .thenReturn(organizationEntityControllerApiMock);
    when(organizationEntityControllerApiMock.crudGetOrganization(orgId))
      .thenThrow(new RestInvokeNotFoundException("APPNAME", HttpStatus.NOT_FOUND, "ERROR", "ERRORCODE", "ERRORMESSAGE"));

    // When
    Organization result = organizationSearchClient.findByOrganizationId(Long.valueOf(orgId), accessToken);

    // Then
    Assertions.assertNull(result);
  }
//endregion

  //region findByBrokerIdAndStatus test
  @Test
  void givenApiThrowsWhenFindByBrokerIdAndStatusThenPropagate() {
    // Given
    String accessToken = "ACCESSTOKEN";
    Long brokerId = 10L;
    OrganizationStatus status = OrganizationStatus.ACTIVE;

    when(organizationApisHolderMock.getOrganizationSearchControllerApi(accessToken))
      .thenReturn(organizationSearchControllerApiMock);
    HttpClientErrorException ex = HttpClientErrorException.create(HttpStatus.NOT_FOUND, "NotFound", null, null, null);
    when(organizationSearchControllerApiMock.crudOrganizationsFindByBrokerIdAndStatus(brokerId, status))
      .thenThrow(ex);

    // When / Then
    Assertions.assertThrows(HttpClientErrorException.class,
      () -> organizationSearchClient.findByBrokerIdAndStatus(brokerId, status, accessToken));
  }

  @ParameterizedTest
  @EnumSource(OrganizationStatus.class)
  void whenFindByBrokerIdAndStatusWithAllStatusesThenOk(OrganizationStatus status) {
    // Given
    String accessToken = "ACCESSTOKEN";
    Long brokerId = 99L;
    CollectionModelOrganization expected = new CollectionModelOrganization();
    when(organizationApisHolderMock.getOrganizationSearchControllerApi(accessToken))
      .thenReturn(organizationSearchControllerApiMock);
    when(organizationSearchControllerApiMock.crudOrganizationsFindByBrokerIdAndStatus(brokerId, status))
      .thenReturn(expected);

    // When
    CollectionModelOrganization result = organizationSearchClient.findByBrokerIdAndStatus(brokerId, status, accessToken);

    // Then
    Assertions.assertSame(expected, result);
  }
  //endregion
}
