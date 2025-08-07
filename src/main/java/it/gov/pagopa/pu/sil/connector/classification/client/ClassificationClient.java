package it.gov.pagopa.pu.sil.connector.classification.client;

import it.gov.pagopa.pu.classification.dto.generated.CollectionModelAssessmentsBalanceView;
import it.gov.pagopa.pu.classification.dto.generated.CollectionModelPaymentsReporting;
import it.gov.pagopa.pu.classification.dto.generated.Treasury;
import it.gov.pagopa.pu.sil.connector.classification.config.ClassificationApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@Slf4j
@Service
public class ClassificationClient {
  private final ClassificationApisHolder classificationApisHolder;

  public ClassificationClient(ClassificationApisHolder classificationApisHolder) {
    this.classificationApisHolder = classificationApisHolder;
  }

  public CollectionModelAssessmentsBalanceView findClosedAssessmentsBalanceViewByOrganizationIdAndIuf(Long organizationId, String iuf, String accessToken) {
    log.info("Finding assessments balance view by organization ID and IUF");
    return classificationApisHolder.getAssessmentsBalanceViewSearchControllerApi(accessToken)
      .crudAssessmentsBalanceViewFindClosedByOrganizationIdAndIuf(String.valueOf(organizationId), iuf);
  }

  public CollectionModelAssessmentsBalanceView findClosedAssessmentsBalanceViewByOrganizationIdAndBill(Long organizationId, String billCode, String billYear, String accessToken) {
    log.info("Finding assessments balance view by organization ID, bill code and bill year");
    return classificationApisHolder.getAssessmentsBalanceViewSearchControllerApi(accessToken)
      .crudAssessmentsBalanceViewFindClosedByOrganizationIdAndBill(String.valueOf(organizationId), billCode, billYear);
  }
}
