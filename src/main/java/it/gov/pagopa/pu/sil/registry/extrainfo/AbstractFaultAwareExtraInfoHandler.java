package it.gov.pagopa.pu.sil.registry.extrainfo;

import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.veneto.regione.pagamenti.ente.Risposta;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFaultAwareExtraInfoHandler<T extends Risposta> {

  public final Map<String, Object> extractResponseExtraInfo(T response) {
    if (response.getFault() != null) {
      Map<String, Object> extraInfo = new HashMap<>();
      extraInfo.put("faultCode", response.getFault().getFaultCode());
      extraInfo.put("description", response.getFault().getDescription());
      extraInfo.put("faultString", response.getFault().getFaultString());
      extraInfo.put("id", response.getFault().getId());
      extraInfo.put("serial", response.getFault().getSerial());
      extraInfo.put(RegistryLogger.SKIP_PAYLOAD_KEY, "true");
      return extraInfo;
    } else {
      return extractResponseExtraInfoOutcomeOk(response);
    }
  }

  protected abstract Map<String, Object> extractResponseExtraInfoOutcomeOk(T response);
}
