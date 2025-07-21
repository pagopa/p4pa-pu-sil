package it.gov.pagopa.pu.sil.service.queryassessments;

import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.sil.connector.classification.ClassificationService;
import it.gov.pagopa.pu.sil.connector.debtpositions.InstallmentService;
import it.gov.pagopa.pu.sil.dto.generated.GetAssessmentResponseDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.AssessmentNotFoundException;
import it.gov.pagopa.pu.sil.mapper.AssessmentsBalanceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class QueryAssessmentsService extends BaseQueryAssessmentsService<GetAssessmentResponseDTO> {
  private final AssessmentsBalanceMapper assessmentsBalanceMapper;

  public QueryAssessmentsService(ClassificationService classificationService,
                                 InstallmentService installmentService,
                                 AssessmentsBalanceMapper assessmentsBalanceMapper) {
    super(classificationService, installmentService);
    this.assessmentsBalanceMapper = assessmentsBalanceMapper;
  }

  @Override
  protected RuntimeException handleException(SilFaults fault, String message) {
    return new AssessmentNotFoundException(fault.description());
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
