package it.gov.pagopa.pu.sil.connector.classification;

import it.gov.pagopa.pu.classification.dto.generated.*;

import java.util.List;
import java.util.Optional;

public interface ClassificationService {
  Optional<Treasury> findTreasuryBySemanticKey(Long organizationId, String billCode, String billYear, String accessToken);
  List<PaymentsReporting> findPaymentsReportingByOrganizationIdAndIuf(Long organizationId, String iuf, String accessToken);
  List<AssessmentsBalanceView> findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(Long organizationId, List<String> iuds, String accessToken);
}
