package it.gov.pagopa.pu.sil.service.inbound.payments.queryassessments;

import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.sil.connector.classification.AssessmentService;
import it.gov.pagopa.pu.sil.dto.generated.GetAssessmentResponseDTO;
import it.gov.pagopa.pu.sil.mapper.AssessmentsBalanceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class QueryAssessmentsService extends BaseQueryAssessmentsService<GetAssessmentResponseDTO> {
  private final AssessmentsBalanceMapper assessmentsBalanceMapper;

  public QueryAssessmentsService(AssessmentService assessmentService,
                                 AssessmentsBalanceMapper assessmentsBalanceMapper) {
    super(assessmentService);
    this.assessmentsBalanceMapper = assessmentsBalanceMapper;
  }

  @Override
  protected GetAssessmentResponseDTO mapToResponse(List<AssessmentsBalanceView> balances) {
    return GetAssessmentResponseDTO.builder()
        .balances(balances.stream()
            .map(assessmentsBalanceMapper::map2BalanceDTO)
            .toList())
        .build();
  }
}
