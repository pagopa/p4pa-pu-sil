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
  public List<AssessmentsBalanceView> findClosedAssessmentsBalanceViewByOrganizationIdAndIuf(Long organizationId, String iuf, String accessToken) {
    CollectionModelAssessmentsBalanceView collectionModelAssessmentsBalanceView = client.findClosedAssessmentsBalanceViewByOrganizationIdAndIuf(organizationId, iuf, accessToken);
    return Objects.requireNonNull(collectionModelAssessmentsBalanceView.getEmbedded()).getAssessmentsBalanceViews();
  }

  @Override
  public List<AssessmentsBalanceView> findClosedAssessmentsBalanceViewByOrganizationIdAndBill(Long organizationId, String billCode, String billYear, String accessToken) {
    CollectionModelAssessmentsBalanceView collectionModelAssessmentsBalanceView = client.findClosedAssessmentsBalanceViewByOrganizationIdAndBill(organizationId, billCode, billYear, accessToken);
    return Objects.requireNonNull(collectionModelAssessmentsBalanceView.getEmbedded()).getAssessmentsBalanceViews();
  }
}
