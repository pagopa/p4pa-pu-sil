package it.gov.pagopa.pu.sil.connector.organization.client;

import it.gov.pagopa.pu.organization.client.generated.OrgSilServiceEntityControllerApi;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrgSilServiceEntityClientTest {
  @Mock
  private OrganizationApisHolder organizationApisHolderMock;
  @Mock
  private OrgSilServiceEntityControllerApi orgSilServiceEntityControllerApiMock;

  private OrgSilServiceEntityClient client;

  @BeforeEach
  void setUp() {
    client = new OrgSilServiceEntityClient(organizationApisHolderMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(
      organizationApisHolderMock,
      orgSilServiceEntityControllerApiMock
    );
  }

  @Test
  void whenFindByIdThenInvokeWithAccessToken() {
    // Given
    String accessToken = "ACCESSTOKEN";
    String orgSilServiceId = "ORGSILSERVICEID";
    OrgSilService expectedResult = new OrgSilService();

    Mockito.when(organizationApisHolderMock.getOrgSilServiceEntityControllerApi(accessToken))
           .thenReturn(orgSilServiceEntityControllerApiMock);
    Mockito.when(orgSilServiceEntityControllerApiMock.crudGetOrgsilservice(orgSilServiceId))
           .thenReturn(expectedResult);

    // When
    OrgSilService result = client.findById(orgSilServiceId, accessToken);

    // Then
    assertSame(expectedResult, result);
  }

  @Test
  void whenFindByIdAndNotFoundThenReturnNull() {
    // Given
    String accessToken = "ACCESSTOKEN";
    String orgSilServiceId = "ORGSILSERVICEID";
    Mockito.when(organizationApisHolderMock.getOrgSilServiceEntityControllerApi(accessToken))
      .thenReturn(orgSilServiceEntityControllerApiMock);
    Mockito.when(orgSilServiceEntityControllerApiMock.crudGetOrgsilservice(orgSilServiceId))
      .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "NotFound", null, null, null));
    // When
    OrgSilService result = client.findById(orgSilServiceId, accessToken);
    // Then
    assertNull(result);
  }
}
