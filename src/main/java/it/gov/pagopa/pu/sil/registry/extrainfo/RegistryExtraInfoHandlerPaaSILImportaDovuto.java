package it.gov.pagopa.pu.sil.registry.extrainfo;

import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.veneto.regione.pagamenti.ente.ElementoListaDovutiEntiSecondari;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovuto;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovutoRisposta;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.ws.soap.SoapHeaderElement;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class RegistryExtraInfoHandlerPaaSILImportaDovuto {
  public Map<String, Object> extractRequestExtraInfo(PaaSILImportaDovuto request, SoapHeaderElement header) {
    Map<String, Object> body = new HashMap<>();
    body.put("flagGeneraIuv", request.isFlagGeneraIuv());
    if (request.getDovuto() != null && request.getDovuto().length > 0) {
      body.put("dovuto", new String(request.getDovuto(), StandardCharsets.UTF_8));
    } else {
      body.put("dovuto", null);
    }
    if (request.getListaDovutiEntiSecondari() != null &&
      !CollectionUtils.isEmpty(request.getListaDovutiEntiSecondari().getElementoListaDovutiEntiSecondaris())) {
      body.put("dovutiEntiSecondariList", request.getListaDovutiEntiSecondari().getElementoListaDovutiEntiSecondaris().stream()
        .map(ElementoListaDovutiEntiSecondari::getDovutiEntiSecondari)
        .map(d -> new String(d, StandardCharsets.UTF_8))
        .toList());
    }
    body.put(RegistryLogger.SKIP_XML_BODY_KEY, "true");
    return body;
  }

  public Map<String, Object> extractResponseExtraInfo(PaaSILImportaDovutoRisposta response, String iuv) {
    return Map.of(RegistryLogger.SKIP_XML_BODY_KEY, "true");
  }
}
