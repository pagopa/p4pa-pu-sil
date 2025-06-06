package it.gov.pagopa.pu.sil.registry.extrainfo;

import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovutiRisposta;
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
class RegistryExtraInfoHandlerPaaSILInviaCarrelloDovutiTest {

  @InjectMocks
  private RegistryExtraInfoHandlerPaaSILInviaCarrelloDovuti registryExtraInfoHandlerPaaSILInviaCarrelloDovuti;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();


  @ParameterizedTest
  @ValueSource(strings = {"PrimaryAndSecExists", "SecEmpty", "SecNull", "PrimaryEmpty", "PrimaryNull"})
  void extractRequestExtraInfo(String dovutiType) {
    PaaSILInviaCarrelloDovuti request = podamFactory.manufacturePojo(PaaSILInviaCarrelloDovuti.class);
    SoapHeaderElement header = podamFactory.manufacturePojo(SoapHeaderElement.class);
    switch (dovutiType) {
      case "PrimaryNull" -> request.setListaDovuti(null);
      case "PrimaryEmpty" ->
        request.getListaDovuti().getElementoListaDovutis().clear();
      case "SecNull" -> request.setListaDovutiEntiSecondari(null);
      case "SecEmpty" ->
        request.getListaDovutiEntiSecondari().getElementoListaDovutiEntiSecondaris().clear();
    }

    Map<String, Object> result = registryExtraInfoHandlerPaaSILInviaCarrelloDovuti.extractRequestExtraInfo(request, header);

    assertNotNull(result);
    assertEquals(dovutiType.equals("PrimaryAndSecExists") ? 4 : 3, result.size());
    assertTrue(result.containsKey(RegistryLogger.SKIP_XML_BODY_KEY));
    assertEquals(request.getEnteSILInviaRispostaPagamentoUrl(), result.get("enteSILInviaRispostaPagamentoUrl"));
    if(dovutiType.equals("PrimaryNull") || dovutiType.equals("PrimaryEmpty")) {
      assertNull(result.get("listaDovuti"));
    } else {
      assertNotNull(result.get("listaDovuti"));
      assertIterableEquals((Iterable<?>)result.get("listaDovuti"), request.getListaDovuti().getElementoListaDovutis().stream()
        .map(e -> new String(e.getDovuti(), StandardCharsets.UTF_8))
        .toList()
      );
    }
    if(dovutiType.equals("SecNull") || dovutiType.equals("SecEmpty")) {
      assertNull(result.get("listaDovutiEntiSecondari"));
    } else {
      assertNotNull(result.get("listaDovutiEntiSecondari"));
      assertIterableEquals((Iterable<?>)result.get("listaDovutiEntiSecondari"),
        request.getListaDovutiEntiSecondari().getElementoListaDovutiEntiSecondaris().stream()
          .map(e -> new String(e.getDovutiEntiSecondari(), StandardCharsets.UTF_8))
          .toList()
      );
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"happyCase", "iuvNull", "iuvMultiple", "idSessionNull", "urlNull"})
  void extractResponseExtraInfo(String testType) {
    PaaSILInviaCarrelloDovutiRisposta response = podamFactory.manufacturePojo(PaaSILInviaCarrelloDovutiRisposta.class);
    String iuv = "IUV";

    switch (testType) {
      case "iuvNull" -> iuv = null;
      case "iuvMultiple" -> iuv = "IUV1" + RegistryLogger.IUV_SEPARATOR + "IUV2";
      case "idSessionNull" -> response.setIdSessionCarrello(null);
      case "urlNull" -> response.setUrl(null);
    }

    Map<String, Object> result = registryExtraInfoHandlerPaaSILInviaCarrelloDovuti.extractResponseExtraInfo(response, iuv);

    assertNotNull(result);
    assertTrue(result.containsKey(RegistryLogger.SKIP_XML_BODY_KEY));
    assertEquals(testType.equals("happyCase") || testType.equals("iuvMultiple") ? 4 : 3, result.size());
    assertEquals(response.getUrl(), testType.equals("urlNull")?null:result.get("url"));
    assertEquals(response.getIdSessionCarrello(), testType.equals("idSessionNull")?null:result.get("idSession"));
    if(!testType.equals("iuvNull")) {
      assertArrayEquals(iuv.split(RegistryLogger.IUV_SEPARATOR), (String[]) result.get("iuv"));
    } else {
      assertNull(result.get("iuv"));
    }
  }
}
