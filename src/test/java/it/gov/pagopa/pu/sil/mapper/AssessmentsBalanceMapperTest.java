package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.sil.dto.generated.BalanceDTO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AssessmentsBalanceMapperTest {
  private final AssessmentsBalanceMapper mapper = new AssessmentsBalanceMapper();

  @Test
  void testMap2BalanceDTO() {
    AssessmentsBalanceView view = new AssessmentsBalanceView()
      .officeCode("OFF1")
      .debtPositionTypeOrgCode("DPT1")
      .sectionCode("SEC1")
      .assessmentCode("ASS1")
      .amountCents(12345L);

    BalanceDTO dto = mapper.map2BalanceDTO(view);

    assertNotNull(dto);
    assertEquals("OFF1", dto.getOffice());
    assertEquals(1, dto.getDebtPositionTypeOrgs().size());
    assertEquals("DPT1", dto.getDebtPositionTypeOrgs().getFirst().getDebtPositionTypeOrgCode());
    assertEquals(1, dto.getDebtPositionTypeOrgs().getFirst().getSections().size());
    assertEquals("SEC1", dto.getDebtPositionTypeOrgs().getFirst().getSections().getFirst().getSectionCode());
    assertEquals(1, dto.getDebtPositionTypeOrgs().getFirst().getSections().getFirst().getAssessments().size());
    assertEquals("ASS1", dto.getDebtPositionTypeOrgs().getFirst().getSections().getFirst().getAssessments().getFirst().getAssessmentCode());
    assertEquals(12345L, dto.getDebtPositionTypeOrgs().getFirst().getSections().getFirst().getAssessments().getFirst().getAmountCents());
  }
}
