package it.gov.pagopa.pu.sil.connector.classification;

import it.gov.pagopa.pu.classification.dto.generated.*;

import java.util.List;

public interface AssessmentService {
  List<AssessmentsBalanceView> findClosedAssessmentsBalanceViewByOrganizationIdAndIuf(Long organizationId, String iuf, String accessToken);
  List<AssessmentsBalanceView> findClosedAssessmentsBalanceViewByOrganizationIdAndBill(Long organizationId, String billCode, String billYear, String accessToken);
}
