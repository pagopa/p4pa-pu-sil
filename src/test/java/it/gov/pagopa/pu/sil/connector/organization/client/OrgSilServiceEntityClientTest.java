package it.gov.pagopa.pu.sil.connector.organization.client;

import it.gov.pagopa.pu.organization.client.generated.OrganizationSilServiceApi;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.organization.config.OrganizationApisHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
class OrgSilServiceEntityClientTest {

  @Mock
  private OrganizationApisHolder organizationApisHolderMock;
  @Mock
  private OrganizationSilServiceApi organizationSilServiceApiMock;

  private OrgSilServiceEntityClient client;

  @BeforeEach
  void setUp() {
    client = new OrgSilServiceEntityClient(organizationApisHolderMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(
      organizationApisHolderMock,
      organizationSilServiceApiMock
    );
  }

  @Test
  void whenFindByIdThenInvokeWithAccessToken() {
    // Given
    String accessToken = "ACCESSTOKEN";
    Long orgSilServiceId = 0L;
    OrgSilServiceDTO expectedResult = new OrgSilServiceDTO();

    Mockito.when(organizationApisHolderMock.getOrganizationSilServiceApi(accessToken))
           .thenReturn(organizationSilServiceApiMock);
    Mockito.when(organizationSilServiceApiMock.getOrgSilService(orgSilServiceId))
           .thenReturn(expectedResult);

    // When
    OrgSilServiceDTO result = client.findById(orgSilServiceId, accessToken);

    // Then
    assertSame(expectedResult, result);
  }

  @Test
  void whenFindByIdAndNotFoundThenReturnNull() {
    // Given
    String accessToken = "ACCESSTOKEN";
    Long orgSilServiceId = 0L;
    Mockito.when(organizationApisHolderMock.getOrganizationSilServiceApi(accessToken))
      .thenReturn(organizationSilServiceApiMock);
    Mockito.when(organizationSilServiceApiMock.getOrgSilService(orgSilServiceId))
      .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "NotFound", null, null, null));
    // When
    OrgSilServiceDTO result = client.findById(orgSilServiceId, accessToken);
    // Then
    assertNull(result);
  }
}
