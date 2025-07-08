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

  public Treasury findTreasuryBySemanticKey(Long organizationId, String billCode, String billYear, String accessToken) {
    log.info("Finding treasury by organization ID, bill code, and bill year");
    try {
      return classificationApisHolder.getTreasurySearchControllerApi(accessToken)
          .crudTreasuryFindBySemanticKey(organizationId, billCode, billYear);
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Cannot find Treasury having organizationId {}, billCode {} and billYear {}", organizationId, billCode, billYear, e);
      return null;
    }
  }

  public CollectionModelPaymentsReporting findPaymentsReportingByOrganizationIdAndIuf(Long organizationId, String iuf, String accessToken) {
    log.info("Finding payments reporting by organization ID and IUF");
    return classificationApisHolder.getPaymentsReportingSearchControllerApi(accessToken)
        .crudPaymentsReportingFindByOrganizationIdAndIuf(organizationId, iuf);
  }

  public CollectionModelAssessmentsBalanceView findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(String organizationId, List<String> iuds, String accessToken) {
    log.info("Finding assessments balance view by organization ID and IUDs");
    return classificationApisHolder.getAssessmentsBalanceViewSearchControllerApi(accessToken)
        .crudAssessmentsBalanceViewFindClosedByOrganizationIdAndIuds(organizationId, iuds);
  }
}
