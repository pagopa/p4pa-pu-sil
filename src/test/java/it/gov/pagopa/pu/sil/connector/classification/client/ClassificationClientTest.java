package it.gov.pagopa.pu.sil.connector.classification.client;

import it.gov.pagopa.pu.classification.client.generated.AssessmentsBalanceViewSearchControllerApi;
import it.gov.pagopa.pu.classification.client.generated.PaymentsReportingSearchControllerApi;
import it.gov.pagopa.pu.classification.client.generated.TreasurySearchControllerApi;
import it.gov.pagopa.pu.classification.dto.generated.CollectionModelAssessmentsBalanceView;
import it.gov.pagopa.pu.classification.dto.generated.CollectionModelPaymentsReporting;
import it.gov.pagopa.pu.classification.dto.generated.Treasury;
import it.gov.pagopa.pu.sil.connector.classification.config.ClassificationApisHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassificationClientTest {
  @Mock
  private ClassificationApisHolder apisHolder;
  @Mock
  private TreasurySearchControllerApi treasurySearchControllerApiMock;
  @Mock
  private PaymentsReportingSearchControllerApi paymentsReportingSearchControllerApiMock;
  @Mock
  private AssessmentsBalanceViewSearchControllerApi assessmentsBalanceViewSearchControllerApiMock;

  private ClassificationClient client;

  @BeforeEach
  void setUp() { client = new ClassificationClient(apisHolder);  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(
        apisHolder,
        treasurySearchControllerApiMock,
        paymentsReportingSearchControllerApiMock,
        assessmentsBalanceViewSearchControllerApiMock
    );
  }

  @Test
  void whenFindTreasuryBySemanticKeyThenInvokeApi() {
    // Given
    Long organizationId = 1L;
    String billCode = "BILL_CODE";
    String billYear = "2025";
    String accessToken = "ACCESSTOKEN";
    Treasury expectedTreasury = new Treasury();

    Mockito.when(apisHolder.getTreasurySearchControllerApi(accessToken))
        .thenReturn(treasurySearchControllerApiMock);
    Mockito.when(treasurySearchControllerApiMock.crudTreasuryFindBySemanticKey(organizationId, billCode, billYear))
        .thenReturn(expectedTreasury);

    // When
    Treasury result = client.findTreasuryBySemanticKey(organizationId, billCode, billYear, accessToken);

    // Then
    assertEquals(expectedTreasury, result);
  }

  @Test
  void whenFindPaymentsReportingByOrganizationIdAndIufThenInvokeApi() {
    // Given
    Long organizationId = 1L;
    String iuf = "IUF";
    String accessToken = "ACCESSTOKEN";
    CollectionModelPaymentsReporting expectedPaymentsReporting =
        new CollectionModelPaymentsReporting();
    Mockito.when(apisHolder.getPaymentsReportingSearchControllerApi(accessToken))
        .thenReturn(paymentsReportingSearchControllerApiMock);
    Mockito.when(paymentsReportingSearchControllerApiMock.crudPaymentsReportingFindByOrganizationIdAndIuf(organizationId, iuf))
        .thenReturn(expectedPaymentsReporting);

    // When
    CollectionModelPaymentsReporting result =
        client.findPaymentsReportingByOrganizationIdAndIuf(organizationId, iuf, accessToken);
    // Then
    assertEquals(expectedPaymentsReporting, result);
  }

  @Test
  void whenFindClosedAssessmentsBalanceViewByOrganizationIdAndIudsThenInvokeApi() {
    // Given
    String organizationId = "1";
    List<String> iuds = List.of("IUD1", "IUD2");
    String accessToken = "ACCESSTOKEN";

    CollectionModelAssessmentsBalanceView expectedBalanceView =
        new CollectionModelAssessmentsBalanceView();

    Mockito.when(apisHolder.getAssessmentsBalanceViewSearchControllerApi(accessToken))
        .thenReturn(assessmentsBalanceViewSearchControllerApiMock);
    Mockito.when(assessmentsBalanceViewSearchControllerApiMock.crudAssessmentsBalanceViewFindClosedByOrganizationIdAndIuds(organizationId, iuds))
        .thenReturn(expectedBalanceView);
    // When
    CollectionModelAssessmentsBalanceView result =
        client.findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(organizationId, iuds, accessToken);
    // Then
    assertEquals(expectedBalanceView, result);
  }
}
