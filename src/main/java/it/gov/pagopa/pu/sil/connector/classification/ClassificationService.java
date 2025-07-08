package it.gov.pagopa.pu.sil.connector.classification;

import it.gov.pagopa.pu.classification.dto.generated.*;

import java.util.List;

public interface ClassificationService {
  Treasury findTreasuryBySemanticKey(Long organizationId, String billCode, String billYear, String accessToken);
  List<PaymentsReporting> findPaymentsReportingByOrganizationIdAndIuf(Long organizationId, String iuf, String accessToken);
  List<AssessmentsBalanceView> findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(String organizationId, List<String> iuds, String accessToken);
}
