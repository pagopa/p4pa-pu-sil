package it.gov.pagopa.pu.sil.connector.organization.service;

import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.organization.client.OrgSilServiceEntityClient;
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
class OrgSilServiceComponentTest {
  @Mock
  private OrgSilServiceEntityClient orgSilServiceEntityClientMock;

  private OrgSilServiceComponent orgSilServiceComponent;

  @BeforeEach
  void setUp() {
    orgSilServiceComponent = new OrgSilServiceComponentImpl(orgSilServiceEntityClientMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(orgSilServiceEntityClientMock);
  }

  @Test
  void givenValidOrgSilServiceIdWhenGetOrgSilServiceByIdThenOk() {
    // Given
    String orgsilserviceid = "ORGSILSERVICEID";
    String accessToken = "ACCESSTOKEN";
    OrgSilService expectedResult = new OrgSilService();

    Mockito.when(orgSilServiceEntityClientMock.findById(orgsilserviceid, accessToken))
      .thenReturn(expectedResult);

    // When
    Optional<OrgSilService> result = orgSilServiceComponent.getOrgSilServiceById(orgsilserviceid, accessToken);

    // Then
    Assertions.assertTrue(result.isPresent());
    Assertions.assertSame(expectedResult, result.get());
  }

  @Test
  void givenNonExistentOrgSilServiceIdWhenGetOrgSilServiceByIdThenEmpty() {
    // Given
    String orgsilserviceid = "ORGSILSERVICEID";
    String accessToken = "ACCESSTOKEN";

    Mockito.when(orgSilServiceEntityClientMock.findById(orgsilserviceid, accessToken))
      .thenReturn(null);

    // When
    Optional<OrgSilService> result = orgSilServiceComponent.getOrgSilServiceById(orgsilserviceid, accessToken);
    // Then
    Assertions.assertTrue(result.isEmpty());
  }
}
