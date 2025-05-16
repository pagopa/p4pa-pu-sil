package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.util.soap.SoapUtils;
import it.veneto.regione.pagamenti.ente.FaultBean;
import it.veneto.regione.pagamenti.ente.PaaSILAutorizzaImportFlusso;
import it.veneto.regione.pagamenti.ente.PaaSILAutorizzaImportFlussoRisposta;
import it.veneto.regione.pagamenti.ente.Risposta;
import it.veneto.regione.pagamenti.ente.ppthead.IntestazionePPT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.server.endpoint.annotation.SoapHeader;

@Endpoint
@Slf4j
public class PuForOrganizationPayEndpoint {
  public static final String NAMESPACE_URI = "http://www.regione.veneto.it/pagamenti/ente/";
  public static final String NAME = "PagamentiTelematiciDovutiPagati";

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILAutorizzaImportFlusso")
  @ResponsePayload
  public PaaSILAutorizzaImportFlussoRisposta paaSILAutorizzaImportFlusso(
    @RequestPayload PaaSILAutorizzaImportFlusso request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header){
    IntestazionePPT intestazionePPT = SoapUtils.unmarshallHeader(header, IntestazionePPT.class);
    log.info("processing paaSILAutorizzaImportFlusso codIpaEnte[{}]", intestazionePPT.getCodIpaEnte());

    return handleFault(SilFaults.PAA_SYSTEM_ERROR, new PaaSILAutorizzaImportFlussoRisposta());
  }

  private <T extends Risposta> T handleFault(SilFaults fault, T responseObj){
    responseObj.setFault(new FaultBean());
    responseObj.getFault().setFaultCode(fault.code());
    responseObj.getFault().setDescription(fault.description());
    responseObj.getFault().setFaultString(fault.description());
    responseObj.getFault().setId("idFaultEmitter");
    responseObj.getFault().setSerial(0);
    return responseObj;
  }

}
