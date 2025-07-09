package it.gov.pagopa.pu.sil.connector.classification;

import it.gov.pagopa.pu.classification.dto.generated.*;
import it.gov.pagopa.pu.sil.connector.classification.client.ClassificationClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassificationServiceTest {
  @Mock
  private ClassificationClient clientMock;

  private ClassificationService service;

  @BeforeEach
  void setUp() {
    service = new ClassificationServiceImpl(clientMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(clientMock);
  }

  @Test
  void whenFindTreasuryBySemanticKeyThenReturnTreasury() {
    // Given
    Long organizationId = 1L;
    String billCode = "BILL_CODE";
    String billYear = "2025";
    String accessToken = "ACCESSTOKEN";
    Treasury expectedTreasury = new Treasury();

    Mockito.when(clientMock.findTreasuryBySemanticKey(organizationId, billCode, billYear, accessToken))
           .thenReturn(expectedTreasury);
    // When
    Optional<Treasury> result = service.findTreasuryBySemanticKey(organizationId, billCode, billYear, accessToken);
    // Then
    assertEquals(Optional.of(expectedTreasury), result);
  }

  @Test
  void findPaymentsReportingByOrganizationIdAndIuf() {
    // Given
    Long organizationId = 1L;
    String iuf = "IUF123";
    String accessToken = "ACCESSTOKEN";

    List<PaymentsReporting> expectedPaymentsReportingList = List.of();
    PagedModelPaymentsReportingEmbedded embedded = new PagedModelPaymentsReportingEmbedded();
    CollectionModelPaymentsReporting expectedPaymentsReporting = new CollectionModelPaymentsReporting(embedded, null);
    Mockito.when(clientMock.findPaymentsReportingByOrganizationIdAndIuf(organizationId, iuf, accessToken))
           .thenReturn(expectedPaymentsReporting);
    // When
    List<PaymentsReporting> result = service.findPaymentsReportingByOrganizationIdAndIuf(organizationId, iuf, accessToken);
    // Then
    assertEquals(expectedPaymentsReportingList, result);
  }

  @Test
  void findAssessmentsBalanceViewByOrganizationIdAndIuds() {
    // Given
    Long organizationId = 1L;
    List<String> iuds = List.of("IUD1", "IUD2");
    String accessToken = "ACCESSTOKEN";

    List<AssessmentsBalanceView> expectedBalanceViews = List.of();
    CollectionModelAssessmentsBalanceViewEmbedded embedded = new CollectionModelAssessmentsBalanceViewEmbedded();
    CollectionModelAssessmentsBalanceView expectedBalanceView = new CollectionModelAssessmentsBalanceView(embedded, null);

    Mockito.when(clientMock.findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(organizationId, iuds, accessToken))
           .thenReturn(expectedBalanceView);
    // When
    List<AssessmentsBalanceView> result = service.findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(organizationId, iuds, accessToken);
    // Then
    assertEquals(expectedBalanceViews, result);
  }
}
