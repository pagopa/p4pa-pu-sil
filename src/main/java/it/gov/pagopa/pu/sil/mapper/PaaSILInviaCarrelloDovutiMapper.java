package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.Taxonomy;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.TaxonomyService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.pagamenti.ente.ElementoListaDovuti;
import it.veneto.regione.pagamenti.ente.ListaDovutiEntiSecondari;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiVersamentoDovutiEntiSecondari;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.DovutiEntiSecondari;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class PaaSILInviaCarrelloDovutiMapper extends AbstractImmediatePaymentsMapper {

  private final TaxonomyService taxonomyService;

  public PaaSILInviaCarrelloDovutiMapper(JAXBTransformService jaxbTransformService,
                                         DebtPositionService debtPositionService,
                                         PersonMapper personMapper,
                                         ValidationService validationService,
                                         TaxonomyService taxonomyService) {
    super(jaxbTransformService, debtPositionService, validationService, personMapper);
    this.taxonomyService = taxonomyService;
  }

  public List<DebtPositionDTO> mapRequestToDebtPositions(PaaSILInviaCarrelloDovuti request, Organization organization, String cartId, String accessToken) {
    //validate "dovuti"
    validationService.validatePrimaryDebtPositionOrganization(request, organization.getIpaCode());
    //validate "dovuti secondari"
    validationService.validateSecondaryDebtPositionCount(request, request.getListaDovuti().getElementoListaDovutis().size());

    //unmarshall "dovuti"
    List<Dovuti> dovutiList = new ArrayList<>();
    int idx = 1;
    for (ElementoListaDovuti elem : request.getListaDovuti().getElementoListaDovutis()) {
      try {
        dovutiList.add(jaxbTransformService.unmarshalling(elem.getDovuti(), Dovuti.class, "/soap/wsdl/payments/PagInf_Dovuti_Pagati_6_2_0.xsd"));
        idx++;
      } catch (ApplicationException unmarshallingException) {
        String errorMessage = "XML dovuti [" + idx + "] non conforme: \n" +
          jaxbTransformService.getDetailUnmarshalExceptionMessage(unmarshallingException, elem.getDovuti());
        log.error("error unmarshalling PaaSILInviaCarrelloDovuti [dovuti {}]: [{}]", idx, errorMessage, unmarshallingException);
        throw new SilFaultException(SilFaults.PAA_XML_NON_VALIDO, errorMessage);
      }
    }

    //check max number of debt positions in the cart
    if (dovutiList.size() > Constants.MAX_CART_SIZE) {
      throw new SilFaultException(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_CARRELLO, "Numero massimo dovuti nel carrello superato: " +
        dovutiList.size() + "/" + Constants.MAX_CART_SIZE);
    }

    List<DebtPositionDTO> debtPositionList = dovutiList.stream()
      .flatMap(d -> dovutiMapper(RegistryEventType.paaSILInviaCarrelloDovuti, cartId, d, organization, accessToken).stream())
      .toList();

    handleSecondaryTransfer(debtPositionList, dovutiList, request.getListaDovutiEntiSecondari(), accessToken);

    return debtPositionList;
  }

  private void handleSecondaryTransfer(List<DebtPositionDTO> debtPositionList, List<Dovuti> dovutiList,
                                       ListaDovutiEntiSecondari dovutiSecondariList, String accessToken) {
    //unmarshall "dovuti secondari" (if present)
    final CtDatiVersamentoDovutiEntiSecondari secondaryTransferData;
    if (dovutiSecondariList != null && !CollectionUtils.isEmpty(dovutiSecondariList.getElementoListaDovutiEntiSecondaris())) {
      byte[] xmlDovutiSecondari = dovutiSecondariList.getElementoListaDovutiEntiSecondaris().getFirst().getDovutiEntiSecondari();
      try {
        secondaryTransferData = jaxbTransformService.unmarshalling(xmlDovutiSecondari, DovutiEntiSecondari.class, "/soap/wsdl/payments/PagInf_Dovuti_Pagati_6_2_0.xsd")
          .getDatiVersamentoEntiSecondari();
      } catch (ApplicationException unmarshallingException) {
        String errorMessage = "XML dovuti enti secondari non conforme: \n" +
          jaxbTransformService.getDetailUnmarshalExceptionMessage(unmarshallingException, xmlDovutiSecondari);
        log.error("error unmarshalling PaaSILInviaCarrelloDovuti dovutEntiSecondari: [{}]", errorMessage, unmarshallingException);
        throw new SilFaultException(SilFaults.PAA_XML_NON_VALIDO, errorMessage);
      }
    } else {
      secondaryTransferData = null;
    }

    if (secondaryTransferData != null) {
      validationService.validateSecondaryDebtPositionData(secondaryTransferData, debtPositionList.size());
      fillSecondaryTransferData(debtPositionList.getFirst(), secondaryTransferData,
        dovutiList.getFirst().getDatiVersamento().getDatiSingoloVersamentos().getFirst().getIdentificativoTipoDovuto(), accessToken);
    }
  }

  private void fillSecondaryTransferData(DebtPositionDTO debtPosition, CtDatiVersamentoDovutiEntiSecondari secondaryTransferData,
                                         String debtPositionTypeOrgCode, String accessToken) {

    String category = retrieveAndValidateSecondaryTransferCategory(
      secondaryTransferData.getDatiSpecificiRiscossione(), debtPosition.getOrganizationId(),
      debtPositionTypeOrgCode, accessToken);

    long secondaryAmount = ConversionUtils.bigDecimalEuroAmountToCentsAmount(secondaryTransferData.getImportoSingoloVersamento());
    TransferDTO secondaryTransfer = TransferDTO.builder()
      .transferIndex(2)
      .orgFiscalCode(secondaryTransferData.getCodiceFiscaleBeneficiario())
      .orgName(secondaryTransferData.getDenominazioneBeneficiario())
      .amountCents(secondaryAmount)
      .remittanceInformation(secondaryTransferData.getCausaleVersamento())
      .iban(secondaryTransferData.getIbanAccreditoBeneficiario())
      .category(category)
      .build();

    PaymentOptionDTO paymentOption = debtPosition.getPaymentOptions().getFirst();
    InstallmentDTO installment = paymentOption.getInstallments().getFirst();
    if (installment.getTransfers() != null) {
      throw new SilFaultException(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_MULTIBENEFICIARI,
        "Non è possibile inserire pagamenti multibeneficiario con più di un trasferimento secondario");
    }
    installment.setTransfers(List.of(secondaryTransfer));
    installment.setAmountCents(installment.getAmountCents() + secondaryAmount);
    paymentOption.setTotalAmountCents(installment.getAmountCents());
  }

  private String retrieveAndValidateSecondaryTransferCategory(String legacyPaymentMetadata, Long organizationId, String debtPositionTypeOrgCode, String accessToken) {
    String category = ValidationUtils.getTransferCategoryFromLegacyPaymentMetadataSecondary(legacyPaymentMetadata);
    if (category == null) {
      // Retrieve the category from the DebtPositionTypeOrg using the debtPositionTypeOrgId
      DebtPositionTypeOrg debtPositionTypeOrg = debtPositionService.getDebtPositionTypeOrgByOrgIdAndType(
        organizationId, debtPositionTypeOrgCode, accessToken);
      DebtPositionType debtPositionType = debtPositionService.getDebtPositionTypeById(debtPositionTypeOrg.getDebtPositionTypeId(), accessToken);
      category = debtPositionType.getTaxonomyCode();
    }
    if (StringUtils.isBlank(category)) {
      throw new SilFaultException(SilFaults.PAA_ENTE_SECONDARIO_NON_VALIDO, "Codice tassonomico non presente o non valido per l'ente secondario: " + category);
    }
    Optional<Taxonomy> taxonomy = taxonomyService.getTaxonomyByTaxonomyCode(category, accessToken);
    if (taxonomy.isEmpty()) {
      throw new SilFaultException(SilFaults.PAA_ENTE_SECONDARIO_NON_VALIDO, "Codice tassonomico dei dati specifici riscossione dell'Ente Secondario non presente in archivio [" + category + "]");
    }

    return category;
  }


}
