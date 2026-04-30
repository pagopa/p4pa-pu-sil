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
public class RegistryExtraInfoHandlerPaaSILImportaDovuto extends AbstractFaultAwareExtraInfoHandler<PaaSILImportaDovutoRisposta> {

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
    body.put(RegistryLogger.SKIP_PAYLOAD_KEY, "true");
    return body;
  }

  @Override
  public Map<String, Object> extractResponseExtraInfoOutcomeOk(PaaSILImportaDovutoRisposta response) {
    Map<String, Object> body = new HashMap<>();
    if(response.getBase64ZipAvviso() != null) {
      body.put("hasNoticeZipFile", "true");
    }
    if(response.getUrlFileAvviso()!=null) {
      body.put("noticeUrl", response.getUrlFileAvviso());
    }
    body.put(RegistryLogger.SKIP_PAYLOAD_KEY, "true");
    return body;
  }
}
