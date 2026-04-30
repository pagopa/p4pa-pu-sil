package it.gov.pagopa.pu.sil.connector.classification.client;

import it.gov.pagopa.pu.classification.dto.generated.CollectionModelAssessmentsBalanceView;
import it.gov.pagopa.pu.sil.connector.classification.config.ClassificationApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AssessmentClient {
  private final ClassificationApisHolder classificationApisHolder;

  public AssessmentClient(ClassificationApisHolder classificationApisHolder) {
    this.classificationApisHolder = classificationApisHolder;
  }

  public CollectionModelAssessmentsBalanceView findClosedAssessmentsBalanceViewByOrganizationIdAndIuf(Long organizationId, String iuf, String accessToken) {
    log.info("Finding assessments balance view by organization ID and IUF");
    return classificationApisHolder.getAssessmentsBalanceViewSearchControllerApi(accessToken)
      .crudAssessmentsBalanceViewFindClosedByOrganizationIdAndIuf(organizationId, iuf);
  }

  public CollectionModelAssessmentsBalanceView findClosedAssessmentsBalanceViewByOrganizationIdAndBill(Long organizationId, String billCode, String billYear, String accessToken) {
    log.info("Finding assessments balance view by organization ID, bill code and bill year");
    return classificationApisHolder.getAssessmentsBalanceViewSearchControllerApi(accessToken)
      .crudAssessmentsBalanceViewFindClosedByOrganizationIdAndBill(organizationId, billCode, billYear);
  }
}
