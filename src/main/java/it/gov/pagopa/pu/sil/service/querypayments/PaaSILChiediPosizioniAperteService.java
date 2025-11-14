package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.client.CheckoutClient;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPosizioniAperte;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPosizioniAperteRisposta;
import it.veneto.regione.pagamenti.ente.PaaSILPosizioniAperte;
import it.veneto.regione.schemas._2012.pagamenti.ente.*;
import jakarta.activation.DataHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static it.gov.pagopa.pu.sil.util.Constants.EXCLUDED_DEBT_POSITION_TYPE_CODES;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaaSILChiediPosizioniAperteService {

  private static final String OBJECT_VERSION = "6.2.0";

  private final DebtPositionService debtPositionService;
  private final OrganizationService organizationService;
  private final JAXBTransformService jaxbTransformService;
  private final CheckoutClient checkoutClient;
  private final CartRequestMapper cartRequestMapper;
  private final DebtPositionTypeService debtPositionTypeService;

  public PaaSILChiediPosizioniAperteRisposta processRequest(
    PaaSILChiediPosizioniAperte request,
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode
  ) {
    PaaSILChiediPosizioniAperteRisposta response = new PaaSILChiediPosizioniAperteRisposta();
    AuthorizationService.validateAdminRole(request.getCodIpaEnte(), userInfo);

    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, request.getCodIpaEnte());
    Organization organization = organizationService.getOrganizationById(organizationId, accessToken)
      .orElse(null);
    if (organization == null || !OrganizationStatus.ACTIVE.equals(organization.getStatus())) {
      throw new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "L'ente non è valido o non è abilitato");
    }


    OffsetDateTimeIntervalFilter dateFilter = new OffsetDateTimeIntervalFilter(null, null);

    List<DebtPositionDTO> debtPositions = debtPositionService.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
      request.getIdentificativoUnivocoPersonaFG().getCodiceIdentificativoUnivoco(),
      PersonEntityType.fromValue(request.getIdentificativoUnivocoPersonaFG().getTipoIdentificativoUnivoco().value()),
      List.of(organizationId),
      EXCLUDED_DEBT_POSITION_TYPE_CODES,
      InstallmentStatus.UNPAID,
      dateFilter,
      accessToken
    );

    response.getPaaSILPosizioniApertes().addAll(processOpenPositions(request, organization, debtPositions, accessToken));
    return response;
  }

  private List<PaaSILPosizioniAperte> processOpenPositions(PaaSILChiediPosizioniAperte request, Organization organization, List<DebtPositionDTO> debtPositions, String accessToken) {
    return debtPositions.stream()
      .flatMap(debtPosition -> debtPosition.getPaymentOptions().stream()
        .flatMap(paymentOption -> paymentOption.getInstallments().stream()
          .map(installment -> {
            PaaSILPosizioniAperte openPosition = new PaaSILPosizioniAperte();
            openPosition.setCodIpaEnte(request.getCodIpaEnte());
            openPosition.setDeNomeEnte(organization.getOrgName());

            try {
              CartRequest cartRequest = cartRequestMapper.mapInstallmentToCartRequest(installment, organization, null, null);
              openPosition.setUrlPagamento(checkoutClient.checkoutCart(cartRequest)); // @TODO: da implementare con la P4ADEV-4043
            } catch (Exception e) {
              log.error("Error generating payment URL for installment[{}]", installment.getInstallmentId(), e);
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
          })
        )
      )
      .toList();
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
