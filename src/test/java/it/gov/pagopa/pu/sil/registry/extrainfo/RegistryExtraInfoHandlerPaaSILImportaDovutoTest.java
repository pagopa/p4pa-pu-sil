package it.gov.pagopa.pu.sil.registry.extrainfo;

import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.ElementoListaDovutiEntiSecondari;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovuto;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovutoRisposta;
import jakarta.activation.DataHandler;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.soap.SoapHeaderElement;
import uk.co.jemos.podam.api.PodamFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RegistryExtraInfoHandlerPaaSILImportaDovutoTest {

  @InjectMocks
  private RegistryExtraInfoHandlerPaaSILImportaDovuto registryExtraInfoHandlerPaaSILImportaDovuto;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();


  @ParameterizedTest
  @ValueSource(strings = {"ListaDovutiEntiSecondari", "ElementoListaDovutiEntiSecondaris", "null"})
  void extractRequestExtraInfo(String nullListLevel) {
    PaaSILImportaDovuto request = podamFactory.manufacturePojo(PaaSILImportaDovuto.class);
    SoapHeaderElement header = podamFactory.manufacturePojo(SoapHeaderElement.class);
    if(nullListLevel.equals("ListaDovutiEntiSecondari")) {
      request.setListaDovutiEntiSecondari(null);
    } else if(nullListLevel.equals("ElementoListaDovutiEntiSecondaris")) {
      request.getListaDovutiEntiSecondari().getElementoListaDovutiEntiSecondaris().clear();
    }

    Map<String, Object> result = registryExtraInfoHandlerPaaSILImportaDovuto.extractRequestExtraInfo(request, header);

    assertNotNull(result);
    assertTrue(result.containsKey(RegistryLogger.SKIP_PAYLOAD_KEY));
    assertEquals(request.isFlagGeneraIuv(), result.get("flagGeneraIuv"));
    assertEquals(new String(request.getDovuto(), StandardCharsets.UTF_8), result.get("dovuto"));
    if(nullListLevel.equals("null")){
      assertEquals(4, result.size());
      assertIterableEquals(
        request.getListaDovutiEntiSecondari().getElementoListaDovutiEntiSecondaris().stream()
          .map(ElementoListaDovutiEntiSecondari::getDovutiEntiSecondari)
          .map(d -> new String(d, StandardCharsets.UTF_8))
          .toList(),
        (List<String>)result.get("dovutiEntiSecondariList")
      );
    } else {
      assertEquals(3, result.size());
      assertNull(result.get("dovutiEntiSecondariList"));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"hasNoticeZipFile", "hasNoticeUrl", "hasBoth", "hasNone"})
  void extractResponseExtraInfo(String testCase) {
    int expectedSize = 1;
    PaaSILImportaDovutoRisposta response = new PaaSILImportaDovutoRisposta();
    if(testCase.equals("hasNoticeZipFile") || testCase.equals("hasBoth")) {
      response.setBase64ZipAvviso(new DataHandler(new byte[]{1, 2, 3}, "application/zip"));
      expectedSize++;
    }
    if(testCase.equals("hasNoticeUrl") || testCase.equals("hasBoth")) {
      response.setUrlFileAvviso("http://example.com/notice");
      expectedSize++;
    }

    Map<String, Object> result = registryExtraInfoHandlerPaaSILImportaDovuto.extractResponseExtraInfo(response);

    assertNotNull(result);
    assertTrue(result.containsKey(RegistryLogger.SKIP_PAYLOAD_KEY));
    if(testCase.equals("hasNoticeZipFile") || testCase.equals("hasBoth")) {
      assertTrue(result.containsKey("hasNoticeZipFile"));
    }
    if(testCase.equals("hasNoticeUrl") || testCase.equals("hasBoth")) {
      assertTrue(result.containsKey("noticeUrl"));
    }
    assertEquals(expectedSize, result.size());
  }
}
