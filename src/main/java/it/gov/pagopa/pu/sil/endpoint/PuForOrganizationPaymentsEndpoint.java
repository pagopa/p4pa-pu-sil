package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.dto.PaymentsProcessingStatusDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.legacy.IngestionFlowFileLegacyStatus;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILImportaDovuto;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILInviaCarrelloDovuti;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILInviaDovuti;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.immediatepayments.PaaSILInviaCarrelloDovutiService;
import it.gov.pagopa.pu.sil.service.immediatepayments.PaaSILInviaDovutiService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileAuthorizationService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileProcessingStatusService;
import it.gov.pagopa.pu.sil.service.paasillimportadovuto.PaaSILImportaDovutoService;
import it.gov.pagopa.pu.sil.util.soap.FaultUtils;
import it.gov.pagopa.pu.sil.util.soap.SoapUtils;
import it.veneto.regione.pagamenti.ente.*;
import it.veneto.regione.pagamenti.ente.ppthead.IntestazionePPT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
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

  private final RegistryLogger registryLogger;

  private final PaaSILImportaDovutoService paaSILImportaDovutoService;
  private final IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationService;
  private final IngestionFlowFileProcessingStatusService ingestionFlowFileProcessingStatusService;
  private final RegistryExtraInfoHandlerPaaSILImportaDovuto registryExtraInfoHandlerPaaSILImportaDovuto;

  private final PaaSILInviaDovutiService paaSILInviaDovutiService;
  private final RegistryExtraInfoHandlerPaaSILInviaDovuti registryExtraInfoHandlerPaaSILInviaDovuti;

  private final PaaSILInviaCarrelloDovutiService paaSILInviaCarrelloDovutiService;
  private final RegistryExtraInfoHandlerPaaSILInviaCarrelloDovuti registryExtraInfoHandlerPaaSILInviaCarrelloDovuti;

  @SuppressWarnings("java:S107")
  public PuForOrganizationPaymentsEndpoint(RegistryLogger registryLogger,
                                           PaaSILImportaDovutoService paaSILImportaDovutoService,
                                           IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationService,
                                           IngestionFlowFileProcessingStatusService ingestionFlowFileProcessingStatusService,
                                           RegistryExtraInfoHandlerPaaSILImportaDovuto registryExtraInfoHandlerPaaSILImportaDovuto,
                                           PaaSILInviaDovutiService paaSILInviaDovutiService,
                                           RegistryExtraInfoHandlerPaaSILInviaDovuti registryExtraInfoHandlerPaaSILInviaDovuti,
                                           PaaSILInviaCarrelloDovutiService paaSILInviaCarrelloDovutiService,
                                           RegistryExtraInfoHandlerPaaSILInviaCarrelloDovuti registryExtraInfoHandlerPaaSILInviaCarrelloDovuti) {
    this.registryLogger = registryLogger;
    this.paaSILImportaDovutoService = paaSILImportaDovutoService;
    this.ingestionFlowFileAuthorizationService = ingestionFlowFileAuthorizationService;
    this.ingestionFlowFileProcessingStatusService = ingestionFlowFileProcessingStatusService;
    this.registryExtraInfoHandlerPaaSILImportaDovuto = registryExtraInfoHandlerPaaSILImportaDovuto;
    this.paaSILInviaDovutiService = paaSILInviaDovutiService;
    this.registryExtraInfoHandlerPaaSILInviaDovuti = registryExtraInfoHandlerPaaSILInviaDovuti;
    this.paaSILInviaCarrelloDovutiService = paaSILInviaCarrelloDovutiService;
    this.registryExtraInfoHandlerPaaSILInviaCarrelloDovuti = registryExtraInfoHandlerPaaSILInviaCarrelloDovuti;
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediStatoImportFlusso")
  @ResponsePayload
  public PaaSILChiediStatoImportFlussoRisposta paaSILChiediStatoImportFlusso(
    @RequestPayload PaaSILChiediStatoImportFlusso request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "paaSILChiediStatoImportFlusso");

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .eventType(RegistryEventType.paaSILChiediStatoImportFlusso)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> {
        PaymentsProcessingStatusDTO processingStatusDTO = ingestionFlowFileProcessingStatusService.getProcessingStatus(
        request,
        userInfo,
        accessToken,
        orgIpaCode,
        Long.valueOf(request.getRequestToken()),
        IngestionFlowFileTypeEnum.DP_INSTALLMENTS);
        PaaSILChiediStatoImportFlussoRisposta response = new PaaSILChiediStatoImportFlussoRisposta();
        response.setStato(IngestionFlowFileLegacyStatus.fromValue2LegacyValue(processingStatusDTO.getStatus()));
        response.setUrlFileScarti(processingStatusDTO.getUrlErrors());
        response.setUrlFileIUV(processingStatusDTO.getUrlImported());
        response.setUrlFileAvvisi(processingStatusDTO.getUrlNotice());
        return Triple.of(response, null, RegistryOutcome.OK);
      },
      FaultUtils.unauthorizedOrSystemExceptionHandler(
        new PaaSILChiediStatoImportFlussoRisposta(),
        PaaSILChiediStatoImportFlussoRisposta::setFault,
        FaultBean::new,
        SilFaults.PAA_ENTE_NON_VALIDO,
        SilFaults.PAA_SYSTEM_ERROR
      )
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILAutorizzaImportFlusso")
  @ResponsePayload
  public PaaSILAutorizzaImportFlussoRisposta paaSILAutorizzaImportFlusso(
    @RequestPayload PaaSILAutorizzaImportFlusso request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "paaSILAutorizzaImportFlusso");

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .eventType(RegistryEventType.paaSILAutorizzaImportFlusso)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> {
        Pair<Long, String> result = ingestionFlowFileAuthorizationService.authorizeIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          IngestionFlowFileTypeEnum.DP_INSTALLMENTS
        );
        PaaSILAutorizzaImportFlussoRisposta response = new PaaSILAutorizzaImportFlussoRisposta();
        response.setRequestToken(String.valueOf(result.getLeft()));
        response.setUploadUrl(result.getRight());
        return Triple.of(response, null, RegistryOutcome.OK);
      },
      FaultUtils.unauthorizedOrSystemExceptionHandler(
        new PaaSILAutorizzaImportFlussoRisposta(),
        PaaSILAutorizzaImportFlussoRisposta::setFault,
        FaultBean::new,
        SilFaults.PAA_ENTE_NON_VALIDO,
        SilFaults.PAA_SYSTEM_ERROR
      )
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILImportaDovuto")
  @ResponsePayload
  public PaaSILImportaDovutoRisposta paaSILImportaDovuto(
    @RequestPayload PaaSILImportaDovuto request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    PaaSILImportaDovutoRisposta response = new PaaSILImportaDovutoRisposta();
    response.setEsito(RegistryOutcome.KO.getValue());
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "paaSILImportaDovuto");

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .eventType(RegistryEventType.paaSILImportaDovuto)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> paaSILImportaDovutoService.paaSILImportaDovuto(userInfo, orgIpaCode, request),
      FaultUtils.unauthorizedOrSystemExceptionHandler(
        response,
        PaaSILImportaDovutoRisposta::setFault,
        FaultBean::new,
        SilFaults.PAA_ENTE_NON_VALIDO,
        SilFaults.PAA_SYSTEM_ERROR
      ),
      () -> registryExtraInfoHandlerPaaSILImportaDovuto.extractRequestExtraInfo(request, header),
      registryExtraInfoHandlerPaaSILImportaDovuto::extractResponseExtraInfo
    );


  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILInviaDovuti")
  @ResponsePayload
  public PaaSILInviaDovutiRisposta paaSILInviaDovuti(
    @RequestPayload PaaSILInviaDovuti request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "paaSILInviaDovuti");
    UserInfo userInfo = SecurityUtils.getLoggedUser();

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .eventType(RegistryEventType.paaSILInviaDovuti)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> paaSILInviaDovutiService.paaSILInviaDovuti(userInfo, orgIpaCode, request),
      FaultUtils.unauthorizedOrSystemExceptionHandler(
        new PaaSILInviaDovutiRisposta(),
        PaaSILInviaDovutiRisposta::setFault,
        FaultBean::new,
        SilFaults.PAA_ENTE_NON_VALIDO,
        SilFaults.PAA_SYSTEM_ERROR
      ),
      () -> registryExtraInfoHandlerPaaSILInviaDovuti.extractRequestExtraInfo(request, header),
      registryExtraInfoHandlerPaaSILInviaDovuti::extractResponseExtraInfo
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILInviaCarrelloDovuti")
  @ResponsePayload
  public PaaSILInviaCarrelloDovutiRisposta paaSILInviaCarrelloDovuti(
    @RequestPayload PaaSILInviaCarrelloDovuti request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "paaSILInviaCarrelloDovuti");
    UserInfo userInfo = SecurityUtils.getLoggedUser();

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .eventType(RegistryEventType.paaSILInviaCarrelloDovuti)
      .loggedUser(userInfo)
      .build();

    //write the request/response to the registry, and execute the service
    return registryLogger.execute(
      contextData,
      request,
      () -> paaSILInviaCarrelloDovutiService.paaSILInviaCarrelloDovuti(userInfo, orgIpaCode, request),
      FaultUtils.unauthorizedOrSystemExceptionHandler(
        new PaaSILInviaCarrelloDovutiRisposta(),
        PaaSILInviaCarrelloDovutiRisposta::setFault,
        FaultBean::new,
        SilFaults.PAA_ENTE_NON_VALIDO,
        SilFaults.PAA_SYSTEM_ERROR
      ),
      () -> registryExtraInfoHandlerPaaSILInviaCarrelloDovuti.extractRequestExtraInfo(request, header),
      registryExtraInfoHandlerPaaSILInviaCarrelloDovuti::extractResponseExtraInfo
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
}
