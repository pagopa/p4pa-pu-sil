package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.organization.service.TaxonomyService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.*;
import it.veneto.regione.schemas._2012.pagamenti.ente.Bilancio;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
abstract class AbstractImmediatePaymentsMapper {

  protected final JAXBTransformService jaxbTransformService;
  protected final DebtPositionTypeService debtPositionTypeService;
  protected final ValidationService validationService;
  protected final PersonMapper personMapper;
  protected final TaxonomyService taxonomyService;

  protected List<DebtPositionDTO> dovutiMapper(RegistryEventType operationType, String cartId, Dovuti dovutiObj, Organization organization, String accessToken) {
    if (!RegistryEventType.PTDP_paaSILInviaDovuti.equals(operationType) &&
      !RegistryEventType.PTDP_paaSILInviaCarrelloDovuti.equals(operationType)) {
      throw new ApplicationException("invalid operation type: " + operationType);
    }
    if (StringUtils.isNotBlank(dovutiObj.getDatiVersamento().getIdentificativoUnivocoVersamento())) {
      throw new SilFaultException(SilFaults.PAA_IUV_NON_VALIDO, "L'inserimento dello IUV è deprecato");
    }

    if (dovutiObj.getDatiVersamento().getDatiSingoloVersamentos().size() > Constants.MAX_CART_SIZE) {
      throw new SilFaultException(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_CARRELLO, "Numero massimo dovuti nel carrello superato: " +
        dovutiObj.getDatiVersamento().getDatiSingoloVersamentos().size() + "/" + Constants.MAX_CART_SIZE);
    } else if (dovutiObj.getDatiVersamento().getDatiSingoloVersamentos().isEmpty()) {
      throw new SilFaultException(SilFaults.PAA_XML_NON_VALIDO, "Nessun dovuto presente");
    }

    PersonDTO debtor = personMapper.getAndValidateDebtor(dovutiObj.getSoggettoPagatore());

    List<DebtPositionDTO> debtPositions = new ArrayList<>();
    int idx = 1;
    for (CtDatiSingoloVersamentoDovuti versamento : dovutiObj.getDatiVersamento().getDatiSingoloVersamentos()) {
      DebtPositionDTO debtPosition = initDebtPosition(organization.getOrganizationId());
      fillAndValidateVersamentoFieldsOfDebtPosition(operationType, idx++, debtPosition,
        debtor, organization, versamento, cartId, accessToken);
      debtPositions.add(debtPosition);
    }

    return debtPositions;
  }


  private DebtPositionDTO initDebtPosition(Long orgId) {
    return DebtPositionDTO.builder()
      .status(DebtPositionStatus.UNPAID)
      .debtPositionOrigin(DebtPositionOrigin.SPONTANEOUS_SIL)
      .organizationId(orgId)
      .description("Posizione debitoria ") //debtPositionTypeOrg description will be added later, with method fillAndValidateVersamentoFieldsOfDebtPosition()
      .flagIuvVolatile(true)
      .flagPuPagoPaPayment(true)
      .multiDebtor(false)
      .debtPositionTypeOrgId(0L) // will be filled later, with method fillAndValidateVersamentoFieldsOfDebtPosition()
      .paymentOptions(List.of(PaymentOptionDTO.builder()
        .status(PaymentOptionStatus.UNPAID)
        .paymentOptionIndex(1)
        .paymentOptionType(PaymentOptionTypeEnum.SINGLE_INSTALLMENT)
        .totalAmountCents(0L) // will be filled later, with method fillAndValidateVersamentoFieldsOfDebtPosition()
        .installments(List.of()) // will be filled later, with method fillAndValidateVersamentoFieldsOfDebtPosition()
        .build()))
      .build();
  }

  private void fillAndValidateVersamentoFieldsOfDebtPosition(RegistryEventType operationType, int idx,
                                                             DebtPositionDTO debtPosition, PersonDTO debtor,
                                                             Organization org, CtDatiSingoloVersamentoDovuti versamento,
                                                             String cartId, String accessToken) {

    DebtPositionTypeOrg debtPositionTypeOrg = debtPositionTypeService.getDebtPositionTypeOrgByOrgIdAndType(
      org.getOrganizationId(), versamento.getIdentificativoTipoDovuto(), accessToken);

    validationService.validateDebtPositionTypeOrg(debtPositionTypeOrg, versamento.getIdentificativoTipoDovuto());
    PersonValidationUtils.validateAnonymousDebtor(debtPositionTypeOrg, debtor);
    validationService.validateStamp(versamento);
    validationService.validatePaymentData(versamento);
    validationService.validateIud(org.getOrganizationId(), versamento.getIdentificativoUnivocoDovuto(), accessToken);

    String balance = Optional.ofNullable(versamento.getBilancio())
      .map(b -> jaxbTransformService.marshalling(b, Bilancio.class))
      .orElse(debtPositionTypeOrg.getBalance());

    Long amount = ConversionUtils.bigDecimalEuroAmountToCentsAmount(versamento.getImportoSingoloVersamento());

    String sourceFlowName = operationType.equals(RegistryEventType.PTDP_paaSILInviaCarrelloDovuti) ?
      Constants.SOURCE_FLOW_NAME_PREFIX_INVIACARRELLODOVUTI + cartId :
      Constants.SOURCE_FLOW_NAME_PREFIX_INVIADOVUTI + cartId;

    debtPosition.setIupdOrg(cartId + "-" + idx);
    debtPosition.setDebtPositionTypeOrgId(Objects.requireNonNull(debtPositionTypeOrg.getDebtPositionTypeOrgId()));
    debtPosition.setDescription(debtPosition.getDescription() + debtPositionTypeOrg.getDescription());
    PaymentOptionDTO paymentOption = debtPosition.getPaymentOptions().getFirst();
    paymentOption.setDescription(debtPosition.getDescription());
    paymentOption.setTotalAmountCents(amount);

    paymentOption.setInstallments(List.of(
      InstallmentDTO.builder()
        .status(InstallmentStatus.UNPAID)
        .iud(versamento.getIdentificativoUnivocoDovuto())
        .amountCents(amount)
        .balance(balance)
        .dueDate(Utilities.getSpontaneousSilExpirationDate())
        .debtor(debtor)
        .legacyPaymentMetadata(versamento.getDatiSpecificiRiscossione())
        .remittanceInformation(versamento.getCausaleVersamento())
        .sourceFlowName(sourceFlowName)
        .generateNotice(false)
        .build()
    ));

    //fill first transfer if necessary (i.e. if the payment has a custom taxonomy code on legacyPaymentMetadata field or a stamp)
    validateAndFillFirstTransferIfNecessary(debtPosition, versamento, org, debtPositionTypeOrg, accessToken);
  }

  private void validateAndFillFirstTransferIfNecessary(DebtPositionDTO debtPosition, CtDatiSingoloVersamentoDovuti versamento,
                                                       Organization org, DebtPositionTypeOrg debtPositionTypeOrg, String accessToken) {
    InstallmentDTO installmentDTO = debtPosition.getPaymentOptions().getFirst().getInstallments().getFirst();

    String category = ValidationUtils.getTransferCategoryFromLegacyPaymentMetadataSecondary(versamento.getDatiSpecificiRiscossione());
    if (category == null && versamento.getDatiMarcaBolloDigitale() == null) {
      // no need to pass the first transfer; it will be created automatically by debt-position API
      return;
    }

    //transfer category
    if (category == null || taxonomyService.getTaxonomyByTaxonomyCode(category, accessToken).isEmpty()) {
      DebtPositionType debtPositionType = debtPositionTypeService.getDebtPositionTypeById(debtPositionTypeOrg.getDebtPositionTypeId(), accessToken);
      category = debtPositionType.getTaxonomyCode().replace("9/", "").replace("/", "");
    }

    // common fields for the first transfer
    TransferDTO transferDTO = TransferDTO.builder()
      .transferIndex(1)
      .orgFiscalCode(org.getOrgFiscalCode())
      .orgName(org.getOrgName())
      .amountCents(installmentDTO.getAmountCents())
      .remittanceInformation(versamento.getCausaleVersamento())
      .category(category)
      .build();

    // stamp/iban fields
    Optional.ofNullable(versamento.getDatiMarcaBolloDigitale()).ifPresentOrElse(
      stamp -> transferDTO
        .stampHashDocument(stamp.getHashDocumento())
        .stampProvincialResidence(stamp.getProvinciaResidenza())
        .stampType(stamp.getTipoBollo()),
      () -> {
        if (StringUtils.isAllBlank(debtPositionTypeOrg.getIban(), debtPositionTypeOrg.getPostalIban())) {
          transferDTO.iban(org.getIban())
            .postalIban(org.getPostalIban());
        } else {
          transferDTO.iban(debtPositionTypeOrg.getIban())
            .postalIban(debtPositionTypeOrg.getPostalIban());
        }
      });

    installmentDTO.setTransfers(List.of(transferDTO));
  }

}
