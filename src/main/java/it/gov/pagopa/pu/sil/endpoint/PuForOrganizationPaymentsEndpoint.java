package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.util.soap.SoapUtils;
import it.veneto.regione.pagamenti.ente.*;
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
public class PuForOrganizationPaymentsEndpoint {
  public static final String NAMESPACE_URI = "http://www.regione.veneto.it/pagamenti/ente/";
  public static final String NAME = "PagamentiTelematiciDovutiPagati";

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILAutorizzaImportFlusso")
  @ResponsePayload
  public PaaSILAutorizzaImportFlussoRisposta paaSILAutorizzaImportFlusso(
    @RequestPayload PaaSILAutorizzaImportFlusso request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header){
    IntestazionePPT intestazionePPT = SoapUtils.unmarshallHeader(header, IntestazionePPT.class);
    log.info("processing paaSILAutorizzaImportFlusso codIpaEnte[{}]", intestazionePPT.getCodIpaEnte());
    //TODO: implement the logic to handle the SOAP Action P4ADEV-2892
    return handleFault(SilFaults.PAA_SYSTEM_ERROR, intestazionePPT.getCodIpaEnte(), new PaaSILAutorizzaImportFlussoRisposta());
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediAvvisiPendenti")
  @ResponsePayload
  public PaaSILChiediAvvisiPendentiRisposta paaSILChiediAvvisiPendenti(
    @RequestPayload PaaSILChiediAvvisiPendenti request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    String faultString = "paaSILChiediAvvisiPendenti is an UNSUPPORTED Operation";
    log.error(faultString);
    return handleFault(SilFaults.PAA_SYSTEM_ERROR, faultString, request.getCodIpaEnte(), new PaaSILChiediAvvisiPendentiRisposta());
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediPosizioniAperte")
  @ResponsePayload
  public PaaSILChiediPosizioniAperteRisposta paaSILChiediPosizioniAperte(
    @RequestPayload PaaSILChiediPosizioniAperte request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    String faultString = "paaSILChiediPosizioniAperte is an UNSUPPORTED Operation";
    log.error(faultString);
    return handleFault(SilFaults.PAA_SYSTEM_ERROR, faultString, request.getCodIpaEnte(), new PaaSILChiediPosizioniAperteRisposta());
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediStoricoPagamenti")
  @ResponsePayload
  public PaaSILChiediStoricoPagamentiRisposta paaSILChiediStoricoPagamenti(
    @RequestPayload PaaSILChiediStoricoPagamenti request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    String faultString = "paaSILChiediStoricoPagamenti is an UNSUPPORTED Operation";
    log.error(faultString);
    return handleFault(SilFaults.PAA_SYSTEM_ERROR, faultString, request.getCodIpaEnte(), new PaaSILChiediStoricoPagamentiRisposta());
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILRegistraPagamento")
  @ResponsePayload
  public PaaSILRegistraPagamentoRisposta paaSILRegistraPagamento(
    @RequestPayload PaaSILRegistraPagamento request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    String faultString = "paaSILRegistraPagamento is an UNSUPPORTED Operation";
    log.error(faultString);
    return handleFault(SilFaults.PAA_SYSTEM_ERROR, faultString, request.getCodIpaEnte(), new PaaSILRegistraPagamentoRisposta());
  }

  private <T extends Risposta> T handleFault(SilFaults fault, String faultString, String idFaultEmitter, T responseObj) {
    responseObj.setFault(new FaultBean());
    responseObj.getFault().setFaultCode(fault.code());
    responseObj.getFault().setDescription(fault.description());
    responseObj.getFault().setFaultString(faultString);
    responseObj.getFault().setId(idFaultEmitter);
    responseObj.getFault().setSerial(0);
    return responseObj;
  }

  private <T extends Risposta> T handleFault(SilFaults fault, String idFaultEmitter, T responseObj) {
    return handleFault(fault, fault.description(), idFaultEmitter, responseObj);
  }
}
