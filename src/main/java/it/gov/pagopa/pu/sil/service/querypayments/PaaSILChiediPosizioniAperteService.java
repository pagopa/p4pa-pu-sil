package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PaymentOptionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPosizioniAperte;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPosizioniAperteRisposta;
import it.veneto.regione.pagamenti.ente.PaaSILPosizioniAperte;
import it.veneto.regione.schemas._2012.pagamenti.ente.*;
import jakarta.activation.DataHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaaSILChiediPosizioniAperteService {

  private final DebtPositionService debtPositionService;
  private final OrganizationService organizationService;
  private final JAXBTransformService jaxbTransformService;

  public PaaSILChiediPosizioniAperteRisposta processRequest(
    PaaSILChiediPosizioniAperte request,
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode
  ) {
    PaaSILChiediPosizioniAperteRisposta response = new PaaSILChiediPosizioniAperteRisposta();
    AuthorizationService.validateAdminRole(request.getCodIpaEnte(), userInfo);

    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);
    Organization organization = organizationService.getOrganizationById(organizationId, accessToken)
      .orElse(null);
    if (organization == null || !OrganizationStatus.ACTIVE.equals(organization.getStatus())) {
      throw new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "L'ente non è valido o non è abilitato");
    }

    List<DebtPositionDTO> debtPositions = debtPositionService.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(request.getIdentificativoUnivocoPersonaFG().getCodiceIdentificativoUnivoco(), PersonEntityType.fromValue(request.getIdentificativoUnivocoPersonaFG().getTipoIdentificativoUnivoco().value()), organizationId, accessToken);

    response.getPaaSILPosizioniApertes().addAll(processOpenPositions(request, organization, debtPositions));
    return response;
  }

  private List<PaaSILPosizioniAperte> processOpenPositions(PaaSILChiediPosizioniAperte request, Organization organization, List<DebtPositionDTO> debtPositions) {
    return debtPositions.stream()
      .flatMap(debtPosition -> debtPosition.getPaymentOptions().stream()
        .flatMap(paymentOption -> paymentOption.getInstallments().stream()
          .map(installment -> {
            PaaSILPosizioniAperte openPosition = new PaaSILPosizioniAperte();
            openPosition.setCodIpaEnte(request.getCodIpaEnte());
            openPosition.setDeNomeEnte(organization.getOrgName());
            openPosition.setUrlPagamento(null); // TODO: to be defined

            Dovuti dovuti = buildDovuti(paymentOption, installment);
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

  private Dovuti buildDovuti(PaymentOptionDTO paymentOption, InstallmentDTO installment) {
    ObjectFactory of = new ObjectFactory();

    Dovuti dovuti = of.createDovuti();
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
    datiVersamentoDovuti.setTipoVersamento(paymentOption.getPaymentOptionType().getValue());
    datiVersamentoDovuti.setIdentificativoUnivocoVersamento(installment.getIuv());

    dovuti.setSoggettoPagatore(soggettoPagatore);
    dovuti.setDatiVersamento(datiVersamentoDovuti);

    return dovuti;
  }

}
