package it.gov.pagopa.pu.sil.connector.classification.client;

import it.gov.pagopa.pu.classification.client.generated.AssessmentsBalanceViewSearchControllerApi;
import it.gov.pagopa.pu.classification.dto.generated.CollectionModelAssessmentsBalanceView;
import it.gov.pagopa.pu.sil.connector.classification.config.ClassificationApisHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ClassificationClientTest {
  @Mock
  private ClassificationApisHolder apisHolder;
  @Mock
  private AssessmentsBalanceViewSearchControllerApi assessmentsBalanceViewSearchControllerApiMock;

  private ClassificationClient client;

  @BeforeEach
  void setUp() { client = new ClassificationClient(apisHolder);  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(
        apisHolder,
        assessmentsBalanceViewSearchControllerApiMock
    );
  }

  @Test
  void whenFindClosedAssessmentsBalanceViewByOrganizationIdAndIufThenInvokeApi() {
    // Given
    Long organizationId = 1L;
    String iuf = "IUF1";
    String accessToken = "ACCESSTOKEN";

    CollectionModelAssessmentsBalanceView expectedBalanceView =
        new CollectionModelAssessmentsBalanceView();

    Mockito.when(apisHolder.getAssessmentsBalanceViewSearchControllerApi(accessToken))
        .thenReturn(assessmentsBalanceViewSearchControllerApiMock);
    Mockito.when(assessmentsBalanceViewSearchControllerApiMock.crudAssessmentsBalanceViewFindClosedByOrganizationIdAndIuf(String.valueOf(organizationId), iuf))
        .thenReturn(expectedBalanceView);
    // When
    CollectionModelAssessmentsBalanceView result =
        client.findClosedAssessmentsBalanceViewByOrganizationIdAndIuf(organizationId, iuf, accessToken);
    // Then
    assertEquals(expectedBalanceView, result);
  }

  @Test
  void whenFindClosedAssessmentsBalanceViewByOrganizationIdAndBillThenInvokeApi() {
    // Given
    Long organizationId = 1L;
    String billCode = "BILL123";
    String billYear = "2023";
    String accessToken = "ACCESSTOKEN";

    CollectionModelAssessmentsBalanceView expectedBalanceView =
      new CollectionModelAssessmentsBalanceView();

    Mockito.when(apisHolder.getAssessmentsBalanceViewSearchControllerApi(accessToken))
      .thenReturn(assessmentsBalanceViewSearchControllerApiMock);
    Mockito.when(assessmentsBalanceViewSearchControllerApiMock
        .crudAssessmentsBalanceViewFindClosedByOrganizationIdAndBill(String.valueOf(organizationId), billCode, billYear))
      .thenReturn(expectedBalanceView);
    // When
    CollectionModelAssessmentsBalanceView result =
      client.findClosedAssessmentsBalanceViewByOrganizationIdAndBill(organizationId, billCode, billYear, accessToken);
    // Then
    assertEquals(expectedBalanceView, result);
  }
}
