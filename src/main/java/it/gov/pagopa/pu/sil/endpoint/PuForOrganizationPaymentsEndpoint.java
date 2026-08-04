package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.dto.generated.DownloadUrl;
import it.gov.pagopa.pu.sil.dto.generated.ExportStatusResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.ImportFileResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.ImportStatusResponseDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.legacy.ExportFileLegacyStatus;
import it.gov.pagopa.pu.sil.enums.legacy.IngestionFlowFileLegacyStatus;
import it.gov.pagopa.pu.sil.exception.soap.PuForOrganizationPaymentsExceptionHandler;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILImportaDovuto;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILInviaCarrelloDovuti;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILInviaDovuti;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.inbound.payments.exportfile.ExportFileProcessingStatusService;
import it.gov.pagopa.pu.sil.service.inbound.payments.exportfile.soap.PaaSILPrenotaExportFlussoIncrementaleConRicevutaService;
import it.gov.pagopa.pu.sil.service.inbound.payments.exportfile.soap.PaaSILPrenotaExportFlussoService;
import it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments.soap.PaaSILInviaCarrelloDovutiService;
import it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments.soap.PaaSILInviaDovutiService;
import it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments.soap.PaaSILVerificaAvvisoService;
import it.gov.pagopa.pu.sil.service.inbound.payments.ingestionflowfile.IngestionFlowFileAuthorizationService;
import it.gov.pagopa.pu.sil.service.inbound.payments.ingestionflowfile.IngestionFlowFileProcessingStatusService;
import it.gov.pagopa.pu.sil.service.inbound.payments.querypayments.soap.*;
import it.gov.pagopa.pu.sil.service.inbound.payments.singleimport.soap.PaaSILImportaDovutoService;
import it.gov.pagopa.pu.sil.util.DateUtils;
import it.gov.pagopa.pu.sil.util.soap.SoapUtils;
import it.veneto.regione.pagamenti.ente.*;
import it.veneto.regione.pagamenti.ente.ppthead.IntestazionePPT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.server.endpoint.annotation.SoapHeader;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

@Endpoint
@RequiredArgsConstructor
@Slf4j
public class PuForOrganizationPaymentsEndpoint {
  public static final String NAMESPACE_URI = "http://www.regione.veneto.it/pagamenti/ente/";
  public static final String NAME = "PagamentiTelematiciDovutiPagati";

  private final RegistryLogger registryLogger;
  private final PuForOrganizationPaymentsExceptionHandler exceptionHandler;

  private final PaaSILImportaDovutoService paaSILImportaDovutoService;
  private final IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationService;
  private final IngestionFlowFileProcessingStatusService ingestionFlowFileProcessingStatusService;
  private final RegistryExtraInfoHandlerPaaSILImportaDovuto registryExtraInfoHandlerPaaSILImportaDovuto;

  private final PaaSILInviaDovutiService paaSILInviaDovutiService;
  private final RegistryExtraInfoHandlerPaaSILInviaDovuti registryExtraInfoHandlerPaaSILInviaDovuti;

  private final PaaSILInviaCarrelloDovutiService paaSILInviaCarrelloDovutiService;
  private final RegistryExtraInfoHandlerPaaSILInviaCarrelloDovuti registryExtraInfoHandlerPaaSILInviaCarrelloDovuti;

  private final PaaSILVerificaAvvisoService paaSILVerificaAvvisoService;

  private final PaaSILChiediPagatiService paaSILChiediPagatiService;
  private final PaaSILChiediPagatiConRicevutaService paaSILChiediPagatiConRicevutaService;
  private final PaaSILChiediEsitoCarrelloDovutiService paaSILChiediEsitoCarrelloDovutiService;

  private final PaaSILPrenotaExportFlussoService paaSILPrenotaExportFlussoService;
  private final PaaSILPrenotaExportFlussoIncrementaleConRicevutaService paaSILPrenotaExportFlussoIncrementaleConRicevutaService;

  private final ExportFileProcessingStatusService exportFileProcessingStatusService;
  private final PaaSILChiediPosizioniAperteService paaSILChiediPosizioniAperteService;
  private final PaaSILChiediStoricoPagamentiService paaSILChiediStoricoPagamentiService;

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediStatoExportFlusso")
  @ResponsePayload
  public PaaSILChiediStatoExportFlussoRisposta paaSILChiediStatoExportFlusso(
    @RequestPayload PaaSILChiediStatoExportFlusso request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "paaSILChiediStatoExportFlusso");
    try {
      Pair<ExportStatusResponseDTO.StatusEnum, String> processingStatus = exportFileProcessingStatusService.getProcessingStatus(
        userInfo,
        accessToken,
        orgIpaCode,
        Long.valueOf(request.getRequestToken()),
        ExportFile.ExportFileTypeEnum.PAID);
      PaaSILChiediStatoExportFlussoRisposta response = new PaaSILChiediStatoExportFlussoRisposta();
      response.setStato(ExportFileLegacyStatus.fromValue2LegacyValue(processingStatus.getLeft()));
      response.setDownloadUrl(processingStatus.getRight());
      return response;
    } catch (Exception e) {
      return exceptionHandler.setExceptionTranscoded(
        new PaaSILChiediStatoExportFlussoRisposta(),
        PaaSILChiediStatoExportFlussoRisposta::setFault,
        e);
    }
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

    try {
      ImportStatusResponseDTO processingStatusDTO = ingestionFlowFileProcessingStatusService.getProcessingStatus(
        userInfo,
        accessToken,
        orgIpaCode,
        Long.valueOf(request.getRequestToken()),
        IngestionFlowFileTypeEnum.DP_INSTALLMENTS);
      PaaSILChiediStatoImportFlussoRisposta response = new PaaSILChiediStatoImportFlussoRisposta();
      response.setStato(IngestionFlowFileLegacyStatus.fromValue2LegacyValue(processingStatusDTO.getStatus()));
      if (processingStatusDTO.getDownloadUrls() != null) {
        setDownloadUrls(response, processingStatusDTO.getDownloadUrls(), request);
      }
      return response;
    } catch (Exception e) {
      return exceptionHandler.setExceptionTranscoded(
        new PaaSILChiediStatoImportFlussoRisposta(),
        PaaSILChiediStatoImportFlussoRisposta::setFault,
        e);
    }
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
      .eventType(RegistryEventType.PTDP_paaSILAutorizzaImportFlusso)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> {
        ImportFileResponseDTO result = ingestionFlowFileAuthorizationService.authorizeIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          IngestionFlowFileTypeEnum.DP_INSTALLMENTS
        );
        PaaSILAutorizzaImportFlussoRisposta response = new PaaSILAutorizzaImportFlussoRisposta();
        response.setRequestToken(result.getImportId());
        response.setUploadUrl(result.getUploadUrl());
        response.setAuthorizationToken(result.getAuthorizationToken());
        response.setImportPath(result.getImportPath());
        return Triple.of(response, null, RegistryOutcome.OK);
      },
      exceptionHandler.buildExceptionHandlerFunction(
        new PaaSILAutorizzaImportFlussoRisposta(),
        PaaSILAutorizzaImportFlussoRisposta::setFault
      )
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILImportaDovuto")
  @ResponsePayload
  public PaaSILImportaDovutoRisposta paaSILImportaDovuto(
    @RequestPayload PaaSILImportaDovuto request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "paaSILImportaDovuto");

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .eventType(RegistryEventType.PTDP_paaSILImportaDovuto)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> paaSILImportaDovutoService.handleAction(request, orgIpaCode, userInfo, accessToken),
      exceptionHandler.buildExceptionHandlerFunction(
        new PaaSILImportaDovutoRisposta(),
        PaaSILImportaDovutoRisposta::setFault
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
    String accessToken = SecurityUtils.getAccessToken();

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .eventType(RegistryEventType.PTDP_paaSILInviaDovuti)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> paaSILInviaDovutiService.processRequest(request, orgIpaCode, userInfo, accessToken),
      exceptionHandler.buildExceptionHandlerFunction(
        new PaaSILInviaDovutiRisposta(),
        PaaSILInviaDovutiRisposta::setFault
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
    String accessToken = SecurityUtils.getAccessToken();

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .eventType(RegistryEventType.PTDP_paaSILInviaCarrelloDovuti)
      .loggedUser(userInfo)
      .build();

    //write the request/response to the registry, and execute the service
    return registryLogger.execute(
      contextData,
      request,
      () -> paaSILInviaCarrelloDovutiService.processRequest(request, orgIpaCode, userInfo, accessToken),
      exceptionHandler.buildExceptionHandlerFunction(
        new PaaSILInviaCarrelloDovutiRisposta(),
        PaaSILInviaCarrelloDovutiRisposta::setFault
      ),
      () -> registryExtraInfoHandlerPaaSILInviaCarrelloDovuti.extractRequestExtraInfo(request, header),
      registryExtraInfoHandlerPaaSILInviaCarrelloDovuti::extractResponseExtraInfo
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILVerificaAvviso")
  @ResponsePayload
  public PaaSILVerificaAvvisoRisposta paaSILVerificaAvviso(
    @RequestPayload PaaSILVerificaAvviso request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "paaSILVerificaAvviso");
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .iuv(request.getIdentificativoUnivocoVersamento())
      .eventType(RegistryEventType.PTDP_paaSILVerificaAvviso)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> Triple.of(
        paaSILVerificaAvvisoService.processRequest(request, orgIpaCode, userInfo, accessToken),
        null,
        RegistryOutcome.OK),
      exceptionHandler.buildExceptionHandlerFunction(
        new PaaSILVerificaAvvisoRisposta(),
        PaaSILVerificaAvvisoRisposta::setFault
      ));
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediPagati")
  @ResponsePayload
  public PaaSILChiediPagatiRisposta paaSILChiediPagati(
    @RequestPayload PaaSILChiediPagati request) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();

    PaaSILChiediPagatiRisposta response;
    try {
      response = paaSILChiediPagatiService.processRequest(request, userInfo, accessToken);
    }catch(Exception e){
      response = exceptionHandler.setExceptionTranscoded(
        new PaaSILChiediPagatiRisposta(),
        PaaSILChiediPagatiRisposta::setFault,
        e);
    }

    return response;
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediPagatiConRicevuta")
  @ResponsePayload
  public PaaSILChiediPagatiConRicevutaRisposta paaSILChiediPagatiConRicevuta(
    @RequestPayload PaaSILChiediPagatiConRicevuta request) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();

    PaaSILChiediPagatiConRicevutaRisposta response;
    try {
      response = paaSILChiediPagatiConRicevutaService.processRequest(request, userInfo, accessToken);
    }catch(Exception e){
      response = exceptionHandler.setExceptionTranscoded(
        new PaaSILChiediPagatiConRicevutaRisposta(),
        PaaSILChiediPagatiConRicevutaRisposta::setFault,
        e);
    }

    return response;
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediEsitoCarrelloDovuti")
  @ResponsePayload
  public PaaSILChiediEsitoCarrelloDovutiRisposta paaSILChiediEsitoCarrelloDovuti(
    @RequestPayload PaaSILChiediEsitoCarrelloDovuti request) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();

    PaaSILChiediEsitoCarrelloDovutiRisposta response;
    try {
      response = paaSILChiediEsitoCarrelloDovutiService.processRequest(request, userInfo, accessToken);
    }catch(Exception e){
      response = exceptionHandler.setExceptionTranscoded(
        new PaaSILChiediEsitoCarrelloDovutiRisposta(),
        PaaSILChiediEsitoCarrelloDovutiRisposta::setFault,
        e);
    }

    return response;
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediAvvisiPendenti")
  @ResponsePayload
  public PaaSILChiediAvvisiPendentiRisposta paaSILChiediAvvisiPendenti(
    @RequestPayload PaaSILChiediAvvisiPendenti request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return exceptionHandler.setFault(
      new PaaSILChiediAvvisiPendentiRisposta(),
      PaaSILChiediAvvisiPendentiRisposta::setFault,
      SilFaults.PAA_SYSTEM_ERROR,
      "paaSILChiediAvvisiPendenti non è una operazione supportata"
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediPosizioniAperte")
  @ResponsePayload
  public PaaSILChiediPosizioniAperteRisposta paaSILChiediPosizioniAperte(
    @RequestPayload PaaSILChiediPosizioniAperte request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();

    PaaSILChiediPosizioniAperteRisposta response;
    try {
      response = paaSILChiediPosizioniAperteService.processRequest(request, userInfo, accessToken);
    } catch(Exception e) {
      response = exceptionHandler.setExceptionTranscoded(
        new PaaSILChiediPosizioniAperteRisposta(),
        PaaSILChiediPosizioniAperteRisposta::setFault,
        e);
    }

    return response;
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediStoricoPagamenti")
  @ResponsePayload
  public PaaSILChiediStoricoPagamentiRisposta paaSILChiediStoricoPagamenti(
    @RequestPayload PaaSILChiediStoricoPagamenti request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();

    PaaSILChiediStoricoPagamentiRisposta response;
    try {
      response = paaSILChiediStoricoPagamentiService.processRequest(request, userInfo, accessToken);
    } catch(Exception e) {
      response = exceptionHandler.setExceptionTranscoded(
        new PaaSILChiediStoricoPagamentiRisposta(),
        PaaSILChiediStoricoPagamentiRisposta::setFault,
        e);
    }

    return response;
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILRegistraPagamento")
  @ResponsePayload
  public PaaSILRegistraPagamentoRisposta paaSILRegistraPagamento(
    @RequestPayload PaaSILRegistraPagamento request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return exceptionHandler.setFault(
      new PaaSILRegistraPagamentoRisposta(),
      PaaSILRegistraPagamentoRisposta::setFault,
      SilFaults.PAA_SYSTEM_ERROR,
      "paaSILRegistraPagamento non è una operazione supportata"
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILPrenotaExportFlusso")
  @ResponsePayload
  public PaaSILPrenotaExportFlussoRisposta paaSILPrenotaExportFlusso(
    @RequestPayload PaaSILPrenotaExportFlusso request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "paaSILPrenotaExportFlusso");

    PaaSILPrenotaExportFlussoRisposta response = new PaaSILPrenotaExportFlussoRisposta();

    Optional<PaaSILPrenotaExportFlusso> optRequest = Optional.ofNullable(request);

    String fileVersion = optRequest.map(PaaSILPrenotaExportFlusso::getVersioneTracciato).orElse(null);

    OffsetDateTime from = optRequest.map(PaaSILPrenotaExportFlusso::getDateFrom)
      .map(XMLGregorianCalendar::toGregorianCalendar)
      .map(GregorianCalendar::toZonedDateTime)
      .map(DateUtils::toOffsetDateTimeStartOfTheDay)
      .orElse(null);

    if (from == null) {
      return exceptionHandler.setFault(
        response,
        PaaSILPrenotaExportFlussoRisposta::setFault,
        SilFaults.PAA_DATE_FROM_NON_VALIDO,
        SilFaults.PAA_DATE_FROM_NON_VALIDO.description()
      );
    }

    OffsetDateTime to = optRequest.map(PaaSILPrenotaExportFlusso::getDateTo)
      .map(XMLGregorianCalendar::toGregorianCalendar)
      .map(GregorianCalendar::toZonedDateTime)
      .map(DateUtils::toOffsetDateTimeEndOfTheDay)
      .orElse(null);

    if (to == null) {
      return exceptionHandler.setFault(
        response,
        PaaSILPrenotaExportFlussoRisposta::setFault,
        SilFaults.PAA_DATE_TO_NON_VALIDO,
        SilFaults.PAA_DATE_TO_NON_VALIDO.description()
      );
    }


    String debtPositionTypeOrgCode = optRequest.map(PaaSILPrenotaExportFlusso::getIdentificativoTipoDovuto)
      .orElse(null);

    try {
      Long result = paaSILPrenotaExportFlussoService.paaSILPrenotaExportFlusso(
        userInfo,
        accessToken,
        orgIpaCode,
        fileVersion,
        from,
        to,
        debtPositionTypeOrgCode
      );
      response.setRequestToken(String.valueOf(result));
      return response;
    } catch (Exception e) {
      return exceptionHandler.setExceptionTranscoded(
        new PaaSILPrenotaExportFlussoRisposta(),
        PaaSILPrenotaExportFlussoRisposta::setFault,
        e
      );
    }
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILPrenotaExportFlussoIncrementaleConRicevuta")
  @ResponsePayload
  public PaaSILPrenotaExportFlussoIncrementaleConRicevutaRisposta paaSILPrenotaExportFlussoIncrementaleConRicevuta(
    @RequestPayload PaaSILPrenotaExportFlussoIncrementaleConRicevuta request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "paaSILPrenotaExportFlussoIncrementaleConRicevuta");

    Optional<PaaSILPrenotaExportFlussoIncrementaleConRicevuta> optRequest = Optional.ofNullable(request);

    boolean incremental = optRequest.map(PaaSILPrenotaExportFlussoIncrementaleConRicevuta::isIncrementale).orElse(false);

    String fileVersion = optRequest.map(PaaSILPrenotaExportFlussoIncrementaleConRicevuta::getVersioneTracciato).orElse(null);

    OffsetDateTime from = optRequest.map(PaaSILPrenotaExportFlussoIncrementaleConRicevuta::getDateFrom)
        .map(XMLGregorianCalendar::toGregorianCalendar)
        .map(gc -> gc.toZonedDateTime().toOffsetDateTime())
        .map(odt -> incremental ? odt : odt.truncatedTo(ChronoUnit.DAYS))
        .orElse(null);

    Optional<XMLGregorianCalendar> xmlGregorianCalendar = optRequest.map(PaaSILPrenotaExportFlussoIncrementaleConRicevuta::getDateTo);
    OffsetDateTime to = xmlGregorianCalendar
        .map(XMLGregorianCalendar::toGregorianCalendar)
        .map(gc -> gc.toZonedDateTime().toOffsetDateTime())
        .map(odt -> incremental ? odt : odt.truncatedTo(ChronoUnit.DAYS))
        .orElse(null);

    String debtPositionTypeOrgCode = optRequest.map(PaaSILPrenotaExportFlussoIncrementaleConRicevuta::getIdentificativoTipoDovuto)
      .orElse(null);

    try {
      Long result = paaSILPrenotaExportFlussoIncrementaleConRicevutaService.doReservation(
        userInfo,
        accessToken,
        orgIpaCode,
        fileVersion,
        from,
        to,
        debtPositionTypeOrgCode,
        incremental
      );
      PaaSILPrenotaExportFlussoIncrementaleConRicevutaRisposta response = new PaaSILPrenotaExportFlussoIncrementaleConRicevutaRisposta();
      response.setRequestToken(String.valueOf(result));
      if (incremental) {
        response.setDateTo(xmlGregorianCalendar.orElse(null));
      }
      return response;
    } catch (Exception e) {
      return exceptionHandler.setExceptionTranscoded(
        new PaaSILPrenotaExportFlussoIncrementaleConRicevutaRisposta(),
        PaaSILPrenotaExportFlussoIncrementaleConRicevutaRisposta::setFault,
        e
      );
    }
  }

  private void setDownloadUrls(PaaSILChiediStatoImportFlussoRisposta response, List<DownloadUrl> urls, PaaSILChiediStatoImportFlusso request) {
    if (urls == null) {
      return;
    }
    urls.forEach(url -> {
      switch (url.getCode()) {
        case DISCARDED_FILE -> response.setUrlFileScarti(Boolean.TRUE.equals(request.isFileScarti()) ? url.getUrl() : null);
        case PAYMENT_NOTICE_FILE -> response.setUrlFileAvvisi(Boolean.TRUE.equals(request.isFileAvvisi()) ? url.getUrl() : null);
        case OUTPUT_FILE -> response.setUrlFileIUV(Boolean.TRUE.equals(request.isFileIUV()) ? url.getUrl() : null);
        case INPUT_FILE -> log.debug("Ignoring INPUT_FILE download URL in response, as it is not relevant for this operation.");
      }
    });
  }
}
