package it.gov.pagopa.pu.sil.mapper.soap;

import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.pivot.ente.CtAccertamento;
import it.veneto.regione.pagamenti.pivot.ente.CtBilancio;
import it.veneto.regione.pagamenti.pivot.ente.CtCapitolo;
import it.veneto.regione.pagamenti.pivot.ente.CtTipoDovuto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyAssessmentsBalanceMapperTest {
  LegacyAssessmentsBalanceMapper mapper = new LegacyAssessmentsBalanceMapper();

  @Test
  void whenMap2CtBilancioThenReturnCtBilancio() {
    AssessmentsBalanceView view = new AssessmentsBalanceView();
    view.setOfficeCode("OFF1");
    view.setDebtPositionTypeOrgCode("TYP1");
    view.setAssessmentCode("CAP1");
    view.setSectionCode("SEC1");
    view.setAmountCents(12345L);

    CtBilancio result = mapper.map2CtBilancio(view);

    assertNotNull(result);
    assertEquals("OFF1", result.getUfficio());
    assertEquals(1, result.getTipoDovutos().size());
    CtTipoDovuto tipoDovuto = result.getTipoDovutos().get(0);
    assertEquals("TYP1", tipoDovuto.getCodTipoDovuto());
    assertEquals(1, tipoDovuto.getCapitolos().size());
    CtCapitolo capitolo = tipoDovuto.getCapitolos().get(0);
    assertEquals("CAP1", capitolo.getCodCapitolo());
    assertEquals(1, capitolo.getAccertamentos().size());
    CtAccertamento accertamento = capitolo.getAccertamentos().get(0);
    assertEquals("SEC1", accertamento.getCodAccertamento());
    assertEquals(ConversionUtils.centsAmountToBigDecimalEuroAmount(12345L), accertamento.getImporto());

    TestUtils.checkNotNullFields(result);
    TestUtils.checkNotNullFields(tipoDovuto);
    TestUtils.checkNotNullFields(capitolo);
    TestUtils.checkNotNullFields(accertamento);
  }
}
