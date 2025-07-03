package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFileStatus;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.dto.PaymentsProcessingStatusDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.legacy.ExportFileLegacyStatus;
import it.gov.pagopa.pu.sil.enums.legacy.IngestionFlowFileLegacyStatus;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILImportaDovuto;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILInviaCarrelloDovuti;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILInviaDovuti;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.exportfile.ExportFileProcessingStatusService;
import it.gov.pagopa.pu.sil.service.exportfile.PaaSILPrenotaExportFlussoIncrementaleConRicevutaService;
import it.gov.pagopa.pu.sil.service.exportfile.PaaSILPrenotaExportFlussoService;
import it.gov.pagopa.pu.sil.service.immediatepayments.PaaSILInviaCarrelloDovutiService;
import it.gov.pagopa.pu.sil.service.immediatepayments.PaaSILInviaDovutiService;
import it.gov.pagopa.pu.sil.service.immediatepayments.PaaSILVerificaAvvisoService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileAuthorizationService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileProcessingStatusService;
import it.gov.pagopa.pu.sil.service.paasillimportadovuto.PaaSILImportaDovutoService;
import it.gov.pagopa.pu.sil.service.querypayments.PaaSILChiediPagatiConRicevutaService;
import it.gov.pagopa.pu.sil.service.querypayments.PaaSILChiediPagatiService;
import it.gov.pagopa.pu.sil.util.soap.FaultUtils;
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
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Endpoint
@RequiredArgsConstructor
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

  private final PaaSILVerificaAvvisoService paaSILVerificaAvvisoService;

  private final PaaSILChiediPagatiService paaSILChiediPagatiService;
  private final PaaSILChiediPagatiConRicevutaService paaSILChiediPagatiConRicevutaService;

  private final PaaSILPrenotaExportFlussoService paaSILPrenotaExportFlussoService;
  private final PaaSILPrenotaExportFlussoIncrementaleConRicevutaService paaSILPrenotaExportFlussoIncrementaleConRicevutaService;

  private final ExportFileProcessingStatusService exportFileProcessingStatusService;

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
      Pair<ExportFileStatus, String> processingStatus = exportFileProcessingStatusService.getProcessingStatus(
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
      return FaultUtils.unauthorizedOrSystemExceptionHandler(
        new PaaSILChiediStatoExportFlussoRisposta(),
        PaaSILChiediStatoExportFlussoRisposta::setFault,
        FaultBean::new,
        SilFaults.PAA_ENTE_NON_VALIDO,
        SilFaults.PAA_SYSTEM_ERROR
      ).apply(e);
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
      return response;
    } catch (Exception e) {
      return FaultUtils.unauthorizedOrSystemExceptionHandler(
        new PaaSILChiediStatoImportFlussoRisposta(),
        PaaSILChiediStatoImportFlussoRisposta::setFault,
        FaultBean::new,
        SilFaults.PAA_ENTE_NON_VALIDO,
        SilFaults.PAA_SYSTEM_ERROR
      ).apply(e);
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
      .eventType(RegistryEventType.PTDP_paaSILImportaDovuto)
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
      FaultUtils.unauthorizedOrSystemExceptionHandler(
        new PaaSILVerificaAvvisoRisposta(),
        PaaSILVerificaAvvisoRisposta::setFault,
        FaultBean::new,
        SilFaults.PAA_ENTE_NON_VALIDO,
        SilFaults.PAA_SYSTEM_ERROR
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
      response = FaultUtils.unauthorizedOrSystemExceptionHandler(
        new PaaSILChiediPagatiRisposta(),
        PaaSILChiediPagatiRisposta::setFault,
        FaultBean::new,
        SilFaults.PAA_ENTE_NON_VALIDO,
        SilFaults.PAA_SYSTEM_ERROR
      ).apply(e);
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
      response = FaultUtils.unauthorizedOrSystemExceptionHandler(
        new PaaSILChiediPagatiConRicevutaRisposta(),
        PaaSILChiediPagatiConRicevutaRisposta::setFault,
        FaultBean::new,
        SilFaults.PAA_ENTE_NON_VALIDO,
        SilFaults.PAA_SYSTEM_ERROR
      ).apply(e);
    }

    return response;
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

    Optional<PaaSILPrenotaExportFlusso> optRequest = Optional.ofNullable(request);

    String fileVersion = optRequest.map(PaaSILPrenotaExportFlusso::getVersioneTracciato).orElse(null);

    OffsetDateTime from = optRequest.map(PaaSILPrenotaExportFlusso::getDateFrom)
      .map(XMLGregorianCalendar::toGregorianCalendar)
      .map(gc -> gc.toZonedDateTime().toOffsetDateTime())
      .map(odt -> odt.truncatedTo(ChronoUnit.DAYS))
      .orElse(null);

    OffsetDateTime to = optRequest.map(PaaSILPrenotaExportFlusso::getDateTo)
      .map(XMLGregorianCalendar::toGregorianCalendar)
      .map(gc -> gc.toZonedDateTime().toOffsetDateTime())
      .map(odt -> odt.truncatedTo(ChronoUnit.DAYS))
      .orElse(null);

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
      PaaSILPrenotaExportFlussoRisposta response = new PaaSILPrenotaExportFlussoRisposta();
      response.setRequestToken(String.valueOf(result));
      return response;
    } catch (Exception e) {
      return handleExportFileRequestValidationException(PaaSILPrenotaExportFlussoRisposta::new)
        .apply(e);
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
      return handleExportFileRequestValidationException(PaaSILPrenotaExportFlussoIncrementaleConRicevutaRisposta::new)
        .apply(e);
    }
  }

  private <T extends Risposta> Function<Exception, T> handleExportFileRequestValidationException(Supplier<T> response) {
    return (Exception e) -> {
      if (e instanceof ExportFileClientException ce) {
        SilFaults fault = switch (ce.getCode()) {
          case
            ProcessExecutionsErrorDTO.CodeEnum.PROCESS_EXECUTIONS_INVALID_FILE_VERSION ->
            SilFaults.PAA_VERSIONE_TRACCIATO_NON_VALIDA;
          case
            ProcessExecutionsErrorDTO.CodeEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE ->
            SilFaults.PAA_INTERVALLO_DATE_NON_VALIDO;
          default -> SilFaults.PAA_SYSTEM_ERROR;
        };
        return FaultUtils.setFaultOnResponse(
          response.get(),
          fault,
          fault.description()
        );
      }
      if (e instanceof ExportFileServiceException se) {
        return FaultUtils.setFaultOnResponse(
          response.get(),
          se.getFault(),
          se.getFault().description() + ": " + se.getMessage()
        );
      }
      return FaultUtils.unauthorizedOrSystemExceptionHandler(
        response.get(),
        T::setFault,
        FaultBean::new,
        SilFaults.PAA_ENTE_NON_VALIDO,
        SilFaults.PAA_SYSTEM_ERROR
      ).apply(e);
    };
  }
}
