package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.sil.dto.generated.BalanceDTO;
import it.gov.pagopa.pu.sil.dto.generated.DebtPositionTypeOrgDTO;
import it.gov.pagopa.pu.sil.dto.generated.SectionDTO;
import it.gov.pagopa.pu.sil.dto.generated.AssessmentDTO;
import org.springframework.stereotype.Service;

@Service
public class AssessmentsBalanceMapper {

  public BalanceDTO map2BalanceDTO(AssessmentsBalanceView balance) {
    BalanceDTO balanceDTO = new BalanceDTO();
    balanceDTO.setOffice(balance.getOfficeCode());

    DebtPositionTypeOrgDTO debtPositionTypeOrgDTO = new DebtPositionTypeOrgDTO();
    debtPositionTypeOrgDTO.setDebtPositionTypeOrgCode(balance.getDebtPositionTypeOrgCode());

    SectionDTO sectionDTO = new SectionDTO();
    sectionDTO.setSectionCode(balance.getSectionCode());

    AssessmentDTO assessmentDTO = new AssessmentDTO();
    assessmentDTO.setAssessmentCode(balance.getAssessmentCode());
    assessmentDTO.setAmountCents(balance.getAmountCents());

    sectionDTO.addAssessmentsItem(assessmentDTO);
    debtPositionTypeOrgDTO.addSectionsItem(sectionDTO);
    balanceDTO.addDebtPositionTypeOrgsItem(debtPositionTypeOrgDTO);

    return balanceDTO;
  }
}
