package it.gov.pagopa.pu.sil.registry.extrainfo;

import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovutiRisposta;
import org.springframework.stereotype.Service;
import org.springframework.ws.soap.SoapHeaderElement;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class RegistryExtraInfoHandlerPaaSILInviaDovuti {
  public Map<String, Object> extractRequestExtraInfo(PaaSILInviaDovuti request, SoapHeaderElement header) {
    Map<String, Object> body = new HashMap<>();
    body.put("enteSILInviaRispostaPagamentoUrl", request.getEnteSILInviaRispostaPagamentoUrl());
    if (request.getDovuti() != null && request.getDovuti().length > 0) {
      body.put("dovuti", new String(request.getDovuti(), StandardCharsets.UTF_8));
    } else {
      body.put("dovuti", null);
    }
    body.put(RegistryLogger.SKIP_XML_BODY_KEY, "true");
    return body;
  }

  public Map<String, Object> extractResponseExtraInfo(PaaSILInviaDovutiRisposta response, String iuv) {
    Map<String, Object> body = new HashMap<>();
    if(response.getIdSession()!=null){
      body.put("idSession", response.getIdSession());
    }
    if(response.getUrl()!=null){
      body.put("url", response.getUrl());
    }
    body.put(RegistryLogger.SKIP_XML_BODY_KEY, "true");
    return body;
  }
}
