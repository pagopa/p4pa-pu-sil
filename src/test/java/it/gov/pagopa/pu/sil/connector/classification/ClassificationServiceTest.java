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
  void findAssessmentsBalanceViewByOrganizationIdAndIuf() {
    // Given
    Long organizationId = 1L;
    String iuf = "IUF1";
    String accessToken = "ACCESSTOKEN";

    List<AssessmentsBalanceView> expectedBalanceViews = List.of();
    CollectionModelAssessmentsBalanceViewEmbedded embedded = new CollectionModelAssessmentsBalanceViewEmbedded();
    CollectionModelAssessmentsBalanceView expectedBalanceView = new CollectionModelAssessmentsBalanceView(embedded, null);

    Mockito.when(clientMock.findClosedAssessmentsBalanceViewByOrganizationIdAndIuf(organizationId, iuf, accessToken))
           .thenReturn(expectedBalanceView);
    // When
    List<AssessmentsBalanceView> result = service.findClosedAssessmentsBalanceViewByOrganizationIdAndIuf(organizationId, iuf, accessToken);
    // Then
    assertEquals(expectedBalanceViews, result);
  }

  @Test
  void findAssessmentsBalanceViewByOrganizationIdAndBill() {
    // Given
    Long organizationId = 1L;
    String billCode = "BILL1";
    String billYear = "2023";
    String accessToken = "ACCESSTOKEN";

    List<AssessmentsBalanceView> expectedBalanceViews = List.of();
    CollectionModelAssessmentsBalanceViewEmbedded embedded = new CollectionModelAssessmentsBalanceViewEmbedded();
    CollectionModelAssessmentsBalanceView expectedBalanceView = new CollectionModelAssessmentsBalanceView(embedded, null);

    Mockito.when(clientMock.findClosedAssessmentsBalanceViewByOrganizationIdAndBill(organizationId, billCode, billYear, accessToken))
           .thenReturn(expectedBalanceView);
    // When
    List<AssessmentsBalanceView> result = service.findClosedAssessmentsBalanceViewByOrganizationIdAndBill(organizationId, billCode, billYear, accessToken);
    // Then
    assertEquals(expectedBalanceViews, result);
  }
}
