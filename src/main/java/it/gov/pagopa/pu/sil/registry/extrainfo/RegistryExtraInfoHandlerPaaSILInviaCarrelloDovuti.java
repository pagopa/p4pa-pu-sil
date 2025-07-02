package it.gov.pagopa.pu.sil.registry.extrainfo;

import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.veneto.regione.pagamenti.ente.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.ws.soap.SoapHeaderElement;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class RegistryExtraInfoHandlerPaaSILInviaCarrelloDovuti extends AbstractFaultAwareExtraInfoHandler<PaaSILInviaCarrelloDovutiRisposta> {

  public Map<String, Object> extractRequestExtraInfo(PaaSILInviaCarrelloDovuti request, SoapHeaderElement header) {
    Map<String, Object> body = new HashMap<>();
    body.put("enteSILInviaRispostaPagamentoUrl", request.getEnteSILInviaRispostaPagamentoUrl());

    Optional.ofNullable(request.getListaDovuti())
      .map(ListaDovuti::getElementoListaDovutis)
      .filter(l -> !CollectionUtils.isEmpty(l))
      .ifPresent(l -> body.put("listaDovuti", l.stream()
        .map(ElementoListaDovuti::getDovuti)
        .map(d -> new String(d, StandardCharsets.UTF_8))
        .toList()));

    Optional.ofNullable(request.getListaDovutiEntiSecondari())
      .map(ListaDovutiEntiSecondari::getElementoListaDovutiEntiSecondaris)
      .filter(l -> !CollectionUtils.isEmpty(l))
      .ifPresent(l -> body.put("listaDovutiEntiSecondari", l.stream()
        .map(ElementoListaDovutiEntiSecondari::getDovutiEntiSecondari)
        .map(d -> new String(d, StandardCharsets.UTF_8))
        .toList()));

    body.put(RegistryLogger.SKIP_PAYLOAD_KEY, "true");
    return body;
  }

  @Override
  public Map<String, Object> extractResponseExtraInfoOutcomeOk(PaaSILInviaCarrelloDovutiRisposta response) {
    Map<String, Object> body = new HashMap<>();
    if(response.getIdSessionCarrello()!=null){
      body.put("idSession", response.getIdSessionCarrello());
    }
    if(response.getUrl()!=null){
      body.put("url", response.getUrl());
    }
    body.put(RegistryLogger.SKIP_PAYLOAD_KEY, "true");
    return body;
  }
}
