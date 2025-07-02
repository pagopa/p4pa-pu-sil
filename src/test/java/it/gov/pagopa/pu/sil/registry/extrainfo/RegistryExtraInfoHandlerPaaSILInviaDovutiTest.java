package it.gov.pagopa.pu.sil.registry.extrainfo;

import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovutiRisposta;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.soap.SoapHeaderElement;
import uk.co.jemos.podam.api.PodamFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RegistryExtraInfoHandlerPaaSILInviaDovutiTest {

  @InjectMocks
  private RegistryExtraInfoHandlerPaaSILInviaDovuti registryExtraInfoHandlerPaaSILInviaDovuti;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();


  @ParameterizedTest
  @ValueSource(strings = {"exists", "empty", "null"})
  void extractRequestExtraInfo(String dovutiType) {
    PaaSILInviaDovuti request = podamFactory.manufacturePojo(PaaSILInviaDovuti.class);
    SoapHeaderElement header = podamFactory.manufacturePojo(SoapHeaderElement.class);
    if (dovutiType.equals("null")) {
      request.setDovuti(null);
    } else if (dovutiType.equals("empty")) {
      request.setDovuti(new byte[0]);
    }

    Map<String, Object> result = registryExtraInfoHandlerPaaSILInviaDovuti.extractRequestExtraInfo(request, header);

    assertNotNull(result);
    assertEquals(3, result.size());
    assertTrue(result.containsKey(RegistryLogger.SKIP_PAYLOAD_KEY));
    assertEquals(request.getEnteSILInviaRispostaPagamentoUrl(), result.get("enteSILInviaRispostaPagamentoUrl"));
    assertEquals(dovutiType.equals("exists") ? new String(request.getDovuti(), StandardCharsets.UTF_8) : null, result.get("dovuti"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"happyCase", "idSessionNull", "urlNull", "fault"})
  void extractResponseExtraInfo(String testCase) {
    PaaSILInviaDovutiRisposta response = podamFactory.manufacturePojo(PaaSILInviaDovutiRisposta.class);
    int expectedSize;
    switch (testCase) {
      case "idSessionNull" -> {
        response.setIdSession(null);
        expectedSize = 2;
      }
      case "urlNull" -> {
        response.setUrl(null);
        expectedSize = 2;
      }
      case "fault" -> {
        expectedSize = 6;
      }
      default -> {
        expectedSize = 3;
      }
    }
    if (testCase.equals("fault")) {
      response.setUrl(null);
      response.setIdSession(null);
    } else {
      response.setFault(null);
    }

    Map<String, Object> result = registryExtraInfoHandlerPaaSILInviaDovuti.extractResponseExtraInfo(response);

    assertNotNull(result);
    assertTrue(result.containsKey(RegistryLogger.SKIP_PAYLOAD_KEY));
    assertEquals(expectedSize, result.size());
    assertEquals(response.getUrl(), result.get("url"));
    assertEquals(response.getIdSession(), result.get("idSession"));
  }
}
