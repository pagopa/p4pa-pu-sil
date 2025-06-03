package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.enums.RegistrySilEventType;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.SilOutcome;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILImportaDovuto;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileAuthorizationService;
import it.gov.pagopa.pu.sil.service.paasillimportadovuto.PaaSILImportaDovutoService;
import it.gov.pagopa.pu.sil.util.soap.FaultUtils;
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

import java.util.Optional;

@Endpoint
@Slf4j
public class PuForOrganizationPaymentsEndpoint {
  public static final String NAMESPACE_URI = "http://www.regione.veneto.it/pagamenti/ente/";
  public static final String NAME = "PagamentiTelematiciDovutiPagati";

  private final RegistryLogger registryLogger;

  private final PaaSILImportaDovutoService paaSILImportaDovutoService;
  private final IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationService;
  private final RegistryExtraInfoHandlerPaaSILImportaDovuto registryExtraInfoHandlerPaaSILImportaDovutoService;

  public PuForOrganizationPaymentsEndpoint(RegistryLogger registryLogger,
                                           PaaSILImportaDovutoService paaSILImportaDovutoService,
                                           IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationService,
                                           RegistryExtraInfoHandlerPaaSILImportaDovuto registryExtraInfoHandlerPaaSILImportaDovutoService) {
    this.registryLogger = registryLogger;
    this.paaSILImportaDovutoService = paaSILImportaDovutoService;
    this.ingestionFlowFileAuthorizationService = ingestionFlowFileAuthorizationService;
    this.registryExtraInfoHandlerPaaSILImportaDovutoService = registryExtraInfoHandlerPaaSILImportaDovutoService;
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILAutorizzaImportFlusso")
  @ResponsePayload
  public PaaSILAutorizzaImportFlussoRisposta paaSILAutorizzaImportFlusso(
    @RequestPayload PaaSILAutorizzaImportFlusso request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    PaaSILAutorizzaImportFlussoRisposta response = new PaaSILAutorizzaImportFlussoRisposta();
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = getOrganizationIpaCodeFromHeader(header, "paaSILAutorizzaImportFlusso");

    //write the request/response to the registry, and execute the service
    return registryLogger.execute(
      AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode),
      RegistrySilEventType.paaSILAutorizzaImportFlusso,
      null,
      request,
      userInfo,
      null,
      () -> ingestionFlowFileAuthorizationService.authorizeIngestionFlowFile(userInfo, accessToken, orgIpaCode, IngestionFlowFileTypeEnum.DP_INSTALLMENTS),
      (Exception e) -> systemErrorFaultResponse(response, e),
      null,
      null
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILImportaDovuto")
  @ResponsePayload
  public PaaSILImportaDovutoRisposta paaSILImportaDovuto(
    @RequestPayload PaaSILImportaDovuto request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    PaaSILImportaDovutoRisposta faultResponse = new PaaSILImportaDovutoRisposta();
    faultResponse.setEsito(SilOutcome.KO.name());
    String orgIpaCode = getOrganizationIpaCodeFromHeader(header, "paaSILImportaDovuto");
    UserInfo userInfo = SecurityUtils.getLoggedUser();

    //write the request/response to the registry, and execute the service
    return registryLogger.execute(
      AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode),
      RegistrySilEventType.paaSILImportaDovuto,
      null,
      request,
      userInfo,
      null,
      () -> paaSILImportaDovutoService.paaSILImportaDovuto(userInfo, orgIpaCode, request),
      (Exception e) -> systemErrorFaultResponse(faultResponse, e),
      () -> registryExtraInfoHandlerPaaSILImportaDovutoService.extractRequestExtraInfo(request, header),
      registryExtraInfoHandlerPaaSILImportaDovutoService::extractResponseExtraInfo
    );


  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediAvvisiPendenti")
  @ResponsePayload
  public PaaSILChiediAvvisiPendentiRisposta paaSILChiediAvvisiPendenti(
    @RequestPayload PaaSILChiediAvvisiPendenti request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return FaultUtils.setFaultOnResponse(
      new PaaSILChiediAvvisiPendentiRisposta(),
      SilFaults.PAA_SYSTEM_ERROR,
      "paaSILChiediAvvisiPendenti non è una operazione supportata",
      FaultBean::new,
      PaaSILChiediAvvisiPendentiRisposta::setFault
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediPosizioniAperte")
  @ResponsePayload
  public PaaSILChiediPosizioniAperteRisposta paaSILChiediPosizioniAperte(
    @RequestPayload PaaSILChiediPosizioniAperte request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return FaultUtils.setFaultOnResponse(
      new PaaSILChiediPosizioniAperteRisposta(),
      SilFaults.PAA_SYSTEM_ERROR,
      "paaSILChiediPosizioniAperte non è una operazione supportata",
      FaultBean::new,
      PaaSILChiediPosizioniAperteRisposta::setFault
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediStoricoPagamenti")
  @ResponsePayload
  public PaaSILChiediStoricoPagamentiRisposta paaSILChiediStoricoPagamenti(
    @RequestPayload PaaSILChiediStoricoPagamenti request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return FaultUtils.setFaultOnResponse(
      new PaaSILChiediStoricoPagamentiRisposta(),
      SilFaults.PAA_SYSTEM_ERROR,
      "paaSILChiediStoricoPagamenti non è una operazione supportata",
      FaultBean::new,
      PaaSILChiediStoricoPagamentiRisposta::setFault
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILRegistraPagamento")
  @ResponsePayload
  public PaaSILRegistraPagamentoRisposta paaSILRegistraPagamento(
    @RequestPayload PaaSILRegistraPagamento request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return FaultUtils.setFaultOnResponse(
      new PaaSILRegistraPagamentoRisposta(),
      SilFaults.PAA_SYSTEM_ERROR,
      "paaSILRegistraPagamento non è una operazione supportata",
      FaultBean::new,
      PaaSILRegistraPagamentoRisposta::setFault
    );
  }

  private <T extends Risposta> T systemErrorFaultResponse(T response, Exception e) {
    log.error("System error occurred", e);
    FaultUtils.setFaultOnResponse(response,
      SilFaults.PAA_SYSTEM_ERROR,
      "Errore di sistema",
      FaultBean::new,
      T::setFault
    );
    return response;
  }

  private String getOrganizationIpaCodeFromHeader(SoapHeaderElement header, String operationName) {
    IntestazionePPT intestazionePPT = SoapUtils.unmarshallHeader(header, IntestazionePPT.class);
    String orgIpaCode = Optional.ofNullable(intestazionePPT)
      .map(IntestazionePPT::getCodIpaEnte)
      .orElse(null);
    log.info("processing {} orgIpaCode[{}]", operationName, orgIpaCode);
    return orgIpaCode;
  }

}
