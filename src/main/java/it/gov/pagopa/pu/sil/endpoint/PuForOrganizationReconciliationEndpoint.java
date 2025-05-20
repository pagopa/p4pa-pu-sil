package it.gov.pagopa.pu.sil.endpoint;

    import it.gov.pagopa.pu.sil.enums.SilFaults;
    import it.gov.pagopa.pu.sil.util.soap.SoapUtils;
    import it.veneto.regione.pagamenti.pivot.ente.*;
    import it.veneto.regione.pagamenti.pivot.ente.ppthead.IntestazionePPT;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.ws.server.endpoint.annotation.Endpoint;
    import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
    import org.springframework.ws.server.endpoint.annotation.RequestPayload;
    import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
    import org.springframework.ws.soap.SoapHeaderElement;
    import org.springframework.ws.soap.server.endpoint.annotation.SoapHeader;

    @Endpoint
    @Slf4j
    public class PuForOrganizationReconciliationEndpoint {
      public static final String NAMESPACE_URI = "http://www.regione.veneto.it/pagamenti/pivot/ente/";
      public static final String NAME = "PagamentiTelematiciPagatiRiconciliati";

      @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILAutorizzaImportFlusso")
      @ResponsePayload
      public PivotSILAutorizzaImportFlussoRisposta pivotSILAutorizzaImportFlusso(
        @RequestPayload PivotSILAutorizzaImportFlusso request,
        @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header){

        IntestazionePPT intestazionePPT = SoapUtils.unmarshallHeader(header, IntestazionePPT.class);
        log.info("processing PivotSILAutorizzaImportFlusso codIpaEnte[{}]", intestazionePPT.getCodIpaEnte());
        //TODO: implement the logic to handle the SOAP Action P4ADEV-2893
        return handleFault(SilFaults.PIVOT_SYSTEM_ERROR, intestazionePPT.getCodIpaEnte(), new PivotSILAutorizzaImportFlussoRisposta());
      }

      @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILChiediPagatiRiconciliati")
      @ResponsePayload
      public PivotSILChiediPagatiRiconciliatiRisposta pivotSILChiediPagatiRiconciliati(
          @RequestPayload PivotSILChiediPagatiRiconciliati request,
          @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
        return handleUnsupportedOperation("paaSILRegistraPagamento is an UNSUPPORTED Operation",
          SoapUtils.unmarshallHeader(header, IntestazionePPT.class).getCodIpaEnte(),
          new PivotSILChiediPagatiRiconciliatiRisposta());
      }

      @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILAutorizzaImportFlussoRendicontazione")
      @ResponsePayload
      public PivotSILAutorizzaImportFlussoRendicontazioneRisposta pivotSILAutorizzaImportFlussoRendicontazione(
          @RequestPayload PivotSILAutorizzaImportFlussoRendicontazione request,
          @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
        return handleUnsupportedOperation("pivotSILAutorizzaImportFlussoRendicontazione is an UNSUPPORTED Operation",
            SoapUtils.unmarshallHeader(header, IntestazionePPT.class).getCodIpaEnte(),
            new PivotSILAutorizzaImportFlussoRendicontazioneRisposta());
      }

      @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILAutorizzaImportFlussoRT")
      @ResponsePayload
      public PivotSILAutorizzaImportFlussoRTRisposta pivotSILAutorizzaImportFlussoRT(
          @RequestPayload PivotSILAutorizzaImportFlussoRT request,
          @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
        return handleUnsupportedOperation("pivotSILAutorizzaImportFlussoRT is an UNSUPPORTED Operation",
            SoapUtils.unmarshallHeader(header, IntestazionePPT.class).getCodIpaEnte(),
            new PivotSILAutorizzaImportFlussoRTRisposta());
      }

      private <T extends Risposta> T handleUnsupportedOperation(String faultString, String idFaultEmitter, T responseObj) {
        log.error(faultString);
        return handleFault(SilFaults.PIVOT_SYSTEM_ERROR, faultString, idFaultEmitter, responseObj);
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
