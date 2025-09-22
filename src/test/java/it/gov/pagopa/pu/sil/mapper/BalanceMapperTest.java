package it.gov.pagopa.pu.sil.mapper;

import static org.junit.jupiter.api.Assertions.*;

import it.gov.pagopa.pu.sil.exception.BalanceParseException;
import org.junit.jupiter.api.Test;

class BalanceMapperTest {

  private final BalanceMapper balanceMapper = new BalanceMapper();


  @Test
  void givenBalanceWhenMapBalanceFromPuSilThenReturnStringXml() {
    String jsonBalance = "[{" +
      "\"capitolo\": \"Dionysus\"," +
      "\"ufficio\": \"Prometheus\"," +
      "\"accertamento\": \"Meleager\"," +
      "\"importo\": 9.53" +
      "}]";

    String expectedXml = "<bilancio><capitolo><codCapitolo>Dionysus</codCapitolo>" +
      "<codUfficio>Prometheus</codUfficio>" +
      "<accertamento><codAccertamento>Meleager</codAccertamento>" +
      "<importo>9.53</importo></accertamento></capitolo></bilancio>";

    String result = balanceMapper.mapBalanceFromSil(jsonBalance);
    assertEquals(expectedXml, result);
  }



  @Test
  void givenBalanceWhenMapBalanceFromPuSilThenReturnNull() {
    String jsonBalance = "[{" +
      "\"ufficio\": \"Prometheus\"," +
      "\"accertamento\": \"Meleager\"," +
      "\"importo\": 9.53" +
      "}]";

    String result = balanceMapper.mapBalanceFromSil(jsonBalance);
    assertNull(result);
  }


  @Test
  void givenBalanceWhenMapBalanceFromPuSilThenThrowException() {
    String invalidJson = "not a json";

    BalanceParseException exception = assertThrows(BalanceParseException.class, () -> {
      balanceMapper.mapBalanceFromSil(invalidJson);
    });

    assertTrue(exception.getMessage().contains("Error while map balance from PuSil"));
  }


  @Test
  void givenEmptyListWhenMapBalanceFromPuSilThenReturnNull() {
    String emptyListJson = "[]";

    String result = balanceMapper.mapBalanceFromSil(emptyListJson);
    assertNull(result);
  }

}
