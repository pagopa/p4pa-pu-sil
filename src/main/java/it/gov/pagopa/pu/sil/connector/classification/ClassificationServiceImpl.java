package it.gov.pagopa.pu.sil.connector.classification;

import it.gov.pagopa.pu.classification.dto.generated.*;
import it.gov.pagopa.pu.sil.connector.classification.client.ClassificationClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ClassificationServiceImpl implements ClassificationService {
  private final ClassificationClient client;

  public ClassificationServiceImpl(ClassificationClient client) {
    this.client = client;
  }

  @Override
  public Treasury findTreasuryBySemanticKey(Long organizationId, String billCode, String billYear, String accessToken) {
    return client.findTreasuryBySemanticKey(organizationId, billCode, billYear, accessToken);
  }

  @Override
  public List<PaymentsReporting> findPaymentsReportingByOrganizationIdAndIuf(Long organizationId, String iuf, String accessToken) {
    CollectionModelPaymentsReporting collectionModelPaymentsReporting = client.findPaymentsReportingByOrganizationIdAndIuf(organizationId, iuf, accessToken);
    return Objects.requireNonNull(collectionModelPaymentsReporting.getEmbedded()).getPaymentsReportings();
  }

  public List<AssessmentsBalanceView> findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(String organizationId, List<String> iuds, String accessToken) {
    CollectionModelAssessmentsBalanceView collectionModelAssessmentsBalanceView = client.findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(organizationId, iuds, accessToken);
    return Objects.requireNonNull(collectionModelAssessmentsBalanceView.getEmbedded()).getAssessmentsBalanceViews();
  }
}
