package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.veneto.regione.pagamenti.pivot.ente.CtAccertamento;
import it.veneto.regione.pagamenti.pivot.ente.CtBilancio;
import it.veneto.regione.pagamenti.pivot.ente.CtCapitolo;
import it.veneto.regione.pagamenti.pivot.ente.CtTipoDovuto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LegacyAssessmentsBalanceMapper {

  public CtBilancio map2CtBilancio(AssessmentsBalanceView balance) {
    Map<String, Map<String, Map<String, Map<String, BigDecimal>>>> grouped = new HashMap<>();
    grouped
      .computeIfAbsent(balance.getOfficeCode(), k -> new HashMap<>())
      .computeIfAbsent(balance.getDebtPositionTypeOrgCode(), k -> new HashMap<>())
      .computeIfAbsent(balance.getAssessmentCode(), k -> new HashMap<>())
      .merge(balance.getSectionCode(),
        ConversionUtils.centsAmountToBigDecimalEuroAmount(balance.getAmountCents()),
        BigDecimal::add
      );
    return toCtBilancio(grouped.entrySet().iterator().next());
  }

  private CtBilancio toCtBilancio(Map.Entry<String, Map<String, Map<String, Map<String, BigDecimal>>>> ufficioEntry) {
    CtBilancio ctBilancio = new CtBilancio();
    ctBilancio.setUfficio(ufficioEntry.getKey());
    List<CtTipoDovuto> tipoDovutoList = ufficioEntry.getValue().entrySet().stream()
      .map(this::toCtTipoDovuto)
      .toList();
    ctBilancio.getTipoDovutos().addAll(tipoDovutoList);
    return ctBilancio;
  }

  private CtTipoDovuto toCtTipoDovuto(Map.Entry<String, Map<String, Map<String, BigDecimal>>> tipoDovutoEntry) {
    CtTipoDovuto ctTipoDovuto = new CtTipoDovuto();
    ctTipoDovuto.setCodTipoDovuto(tipoDovutoEntry.getKey());
    List<CtCapitolo> capitoloList = tipoDovutoEntry.getValue().entrySet().stream()
      .map(this::toCtCapitolo)
      .toList();
    ctTipoDovuto.getCapitolos().addAll(capitoloList);
    return ctTipoDovuto;
  }

  private CtCapitolo toCtCapitolo(Map.Entry<String, Map<String, BigDecimal>> capitoloEntry) {
    CtCapitolo ctCapitolo = new CtCapitolo();
    ctCapitolo.setCodCapitolo(capitoloEntry.getKey());
    List<CtAccertamento> accertamentoList = capitoloEntry.getValue().entrySet().stream()
      .map(this::toCtAccertamento)
      .toList();
    ctCapitolo.getAccertamentos().addAll(accertamentoList);
    return ctCapitolo;
  }

  private CtAccertamento toCtAccertamento(Map.Entry<String, BigDecimal> accertamentoEntry) {
    CtAccertamento ctAccertamento = new CtAccertamento();
    ctAccertamento.setCodAccertamento(accertamentoEntry.getKey());
    ctAccertamento.setImporto(accertamentoEntry.getValue());
    return ctAccertamento;
  }
}
