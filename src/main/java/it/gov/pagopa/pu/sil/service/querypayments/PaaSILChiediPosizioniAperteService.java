package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionCheckoutService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPosizioniAperte;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPosizioniAperteRisposta;
import it.veneto.regione.pagamenti.ente.PaaSILPosizioniAperte;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtIdentificativoUnivocoPersonaFG;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtSoggettoPagatore;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.ObjectFactory;
import it.veneto.regione.schemas._2012.pagamenti.ente.StTipoIdentificativoUnivocoPersFG;
import jakarta.activation.DataHandler;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaaSILChiediPosizioniAperteService extends AbstractDebtorQueryPaymentService<PaaSILChiediPosizioniAperte, PaaSILChiediPosizioniAperteRisposta> {

  private static final String OBJECT_VERSION = "6.2.0";

  private final JAXBTransformService jaxbTransformService;
  private final DebtPositionTypeService debtPositionTypeService;

  protected PaaSILChiediPosizioniAperteService(@Value("${public-base-url.bff}") String bffBaseUrl,
                                               DebtPositionService debtPositionService,
                                               OrganizationService organizationService,
                                               AuthorizationService authorizationService,
                                               JAXBTransformService jaxbTransformService,
                                               DebtPositionCheckoutService debtPositionCheckoutService,
                                               DebtPositionTypeService debtPositionTypeService) {
    super(bffBaseUrl, debtPositionService, organizationService, authorizationService, debtPositionCheckoutService);
    this.jaxbTransformService = jaxbTransformService;
    this.debtPositionTypeService = debtPositionTypeService;
  }

  @Override
  protected DebtorQueryPaymentRequest transformRequest(PaaSILChiediPosizioniAperte request) {
    return new DebtorQueryPaymentRequest(
      request.getCodIpaEnte(),
      PersonEntityType.fromValue(request.getIdentificativoUnivocoPersonaFG().getTipoIdentificativoUnivoco().value()),
      request.getIdentificativoUnivocoPersonaFG().getCodiceIdentificativoUnivoco(),
      InstallmentStatus.UNPAID,
      null,
      null
    );
  }

  @Override
  protected PaaSILChiediPosizioniAperteRisposta gatherToResponse(DebtorQueryPaymentRequest request,
                                                                  List<Organization> organizations,
                                                                  List<DebtPositionDTO> debtPositions,
                                                                  String accessToken) {
    PaaSILChiediPosizioniAperteRisposta response = new PaaSILChiediPosizioniAperteRisposta();

    debtPositions.stream()
      .flatMap(debtPosition -> {
        Organization organization = organizations.stream()
          .filter(org -> org.getOrganizationId().equals(debtPosition.getOrganizationId()))
          .findFirst()
          .orElseThrow();

        return debtPosition.getPaymentOptions().stream()
          .flatMap(paymentOption -> paymentOption.getInstallments().stream()
            .map(installment -> {
              PaaSILPosizioniAperte openPosition = new PaaSILPosizioniAperte();
              openPosition.setCodIpaEnte(organization.getIpaCode());
              openPosition.setDeNomeEnte(organization.getOrgName());

              try {
                openPosition.setUrlPagamento(
                  getCheckoutUrl(organization.getOrganizationId(),
                    installment.getIuv(), null, organization.getOrgFiscalCode(),
                    accessToken));
              } catch (Exception e) {
                log.error("Error generating payment URL for installment[{}]",
                  installment.getInstallmentId(), e);
              }

              DebtPositionTypeOrg debtPositionTypeOrg = null;

              try {
                debtPositionTypeOrg = debtPositionTypeService.getDebtPositionTypeOrgByInstallmentId(installment.getInstallmentId(), accessToken);
              } catch (Exception e) {
                log.error("Error fetching DebtPositionTypeOrg for installment[{}]", installment.getInstallmentId(), e);
              }

              Dovuti dovuti = buildDovuti(installment, debtPositionTypeOrg);
              byte[] dovutiBytes = jaxbTransformService.marshallingAsBytes(dovuti, Dovuti.class);

              openPosition.setDovuti(
                new DataHandler(
                  new ByteArrayDataSource("application/octet-stream", dovutiBytes)
                )
              );

              return openPosition;
            }));
      })
      .forEach(response.getPaaSILPosizioniApertes()::add);

    return response;
  }

  private Dovuti buildDovuti(InstallmentDTO installment, DebtPositionTypeOrg debtPositionTypeOrg) {
    ObjectFactory of = new ObjectFactory();

    Dovuti dovuti = of.createDovuti();
    dovuti.setVersioneOggetto(OBJECT_VERSION);
    CtSoggettoPagatore soggettoPagatore = of.createCtSoggettoPagatore();
    CtIdentificativoUnivocoPersonaFG identificativoUnivocoPagatore = new CtIdentificativoUnivocoPersonaFG();
    identificativoUnivocoPagatore.setCodiceIdentificativoUnivoco(installment.getDebtor().getFiscalCode());
    identificativoUnivocoPagatore.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.fromValue(installment.getDebtor().getEntityType().getValue()));
    soggettoPagatore.setIdentificativoUnivocoPagatore(identificativoUnivocoPagatore);
    soggettoPagatore.setAnagraficaPagatore(installment.getDebtor().getFullName());
    soggettoPagatore.setIndirizzoPagatore(installment.getDebtor().getAddress());
    soggettoPagatore.setCivicoPagatore(installment.getDebtor().getCivic());
    soggettoPagatore.setCapPagatore(installment.getDebtor().getPostalCode());
    soggettoPagatore.setLocalitaPagatore(installment.getDebtor().getLocation());
    soggettoPagatore.setProvinciaPagatore(installment.getDebtor().getProvince());
    soggettoPagatore.setNazionePagatore(installment.getDebtor().getNation());
    soggettoPagatore.setEMailPagatore(installment.getDebtor().getEmail());

    CtDatiVersamentoDovuti datiVersamentoDovuti = of.createCtDatiVersamentoDovuti();
    datiVersamentoDovuti.setTipoVersamento("ALL");
    datiVersamentoDovuti.setIdentificativoUnivocoVersamento(installment.getIuv());
    CtDatiSingoloVersamentoDovuti datiSingoloVersamento = of.createCtDatiSingoloVersamentoDovuti();
    datiSingoloVersamento.setIdentificativoUnivocoDovuto(installment.getIud());
    datiSingoloVersamento.setImportoSingoloVersamento(ConversionUtils.centsAmountToBigDecimalEuroAmount(installment.getAmountCents()));

    if (debtPositionTypeOrg != null) {
      datiSingoloVersamento.setIdentificativoTipoDovuto(debtPositionTypeOrg.getCode());
    }

    datiSingoloVersamento.setCausaleVersamento(installment.getRemittanceInformation());
    datiSingoloVersamento.setDatiSpecificiRiscossione(installment.getLegacyPaymentMetadata());

    datiVersamentoDovuti.getDatiSingoloVersamentos().add(datiSingoloVersamento);

    dovuti.setSoggettoPagatore(soggettoPagatore);
    dovuti.setDatiVersamento(datiVersamentoDovuti);

    return dovuti;
  }
}
