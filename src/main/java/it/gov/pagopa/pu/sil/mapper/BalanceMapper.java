package it.gov.pagopa.pu.sil.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.pu.sil.exception.BalanceParseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class BalanceMapper {

  private static final String CAPITOLO = "capitolo";
  private static final String IMPORTO = "importo";

  public String mapBalanceFromSil(String balance) {
    try{
      ObjectMapper mapper = new ObjectMapper();
      List<Map<String, Object>> balanceList = mapper.readValue(balance, new TypeReference<>() {});
      if (balanceList != null && !balanceList.isEmpty()) {
        if (StringUtils.isNotBlank((String) balanceList.getFirst().get(CAPITOLO)) &&
          balanceList.getFirst().containsKey(IMPORTO)) {

          StringBuilder sb = new StringBuilder("<bilancio>");
          BigDecimal importFromBalance = BigDecimal.ZERO;

          for (Map<String, Object> bb : balanceList) {
            if (StringUtils.isNotBlank((String) bb.get(CAPITOLO))) {
              sb.append("<capitolo>");
              sb.append("<codCapitolo>").append(bb.get(CAPITOLO)).append("</codCapitolo>");

              if (StringUtils.isNotBlank((String) bb.get("ufficio"))) {
                sb.append("<codUfficio>").append(bb.get("ufficio")).append("</codUfficio>");
              }

              sb.append("<accertamento>");
              if (StringUtils.isNotBlank((String) bb.get("accertamento"))) {
                sb.append("<codAccertamento>").append(bb.get("accertamento")).append("</codAccertamento>");
              }

              sb.append("<importo>").append(bb.get(IMPORTO)).append("</importo>");
              importFromBalance = importFromBalance.add(new BigDecimal(bb.get(IMPORTO).toString()));
              sb.append("</accertamento></capitolo>");
            }
          }
          sb.append("</bilancio>");
          return sb.toString();
        } else {
          log.error("Field chapter or import not present.");
        }
      }
    } catch (JsonProcessingException e) {
      throw new BalanceParseException("Error while parse balance from PuSil.", e);
    }
    return null;
  }
}
