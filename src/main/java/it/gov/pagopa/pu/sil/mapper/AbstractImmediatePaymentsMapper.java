package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionInstallmentService;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.*;
import it.veneto.regione.schemas._2012.pagamenti.ente.Bilancio;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@RequiredArgsConstructor
abstract class AbstractImmediatePaymentsMapper {

  protected final JAXBTransformService jaxbTransformService;
  protected final DebtPositionTypeService debtPositionTypeService;
  protected final ValidationService validationService;
  protected final PersonMapper personMapper;
  protected final DebtPositionInstallmentService debtPositionInstallmentService;

  protected DebtPositionDTO dovutiMapper(RegistryEventType operationType, String cartId, Dovuti dovutiObj, Organization org, String accessToken) {
    if (!RegistryEventType.PTDP_paaSILInviaDovuti.equals(operationType) &&
      !RegistryEventType.PTDP_paaSILInviaCarrelloDovuti.equals(operationType)) {
      throw new ApplicationException("invalid operation type: " + operationType);
    }
    if (StringUtils.isNotBlank(dovutiObj.getDatiVersamento().getIdentificativoUnivocoVersamento())) {
      throw new SilFaultException(SilFaults.PAA_IUV_NON_VALIDO, "L'inserimento dello IUV è deprecato");
    }

    PersonDTO debtor = personMapper.getAndValidateDebtor(dovutiObj.getSoggettoPagatore());

    CtDatiSingoloVersamentoDovuti versamento = dovutiObj.getDatiVersamento().getDatiSingoloVersamentos().getFirst();

    // get debt position type org and validate
    DebtPositionTypeOrg debtPositionTypeOrg = debtPositionTypeService.getDebtPositionTypeOrgByOrgIdAndType(
      org.getOrganizationId(), versamento.getIdentificativoTipoDovuto(), accessToken);
    validationService.validateStamp(versamento);
    validationService.validateIud(org.getOrganizationId(), versamento.getIdentificativoUnivocoDovuto(), accessToken);

    String balance = Optional.ofNullable(versamento.getBilancio())
      .map(b -> jaxbTransformService.marshalling(b, Bilancio.class))
      .orElse(null);

    Long amount = ConversionUtils.bigDecimalEuroAmountToCentsAmount(versamento.getImportoSingoloVersamento());

    String sourceFlowName = Constants.SOURCE_FLOW_NAME_PREFIX_INVIADOVUTI + cartId;
    if (RegistryEventType.PTDP_paaSILInviaCarrelloDovuti.equals(operationType)) {
      sourceFlowName = Constants.SOURCE_FLOW_NAME_PREFIX_INVIACARRELLODOVUTI + cartId;
    }

    TransferDTO transferDTO = TransferDTO.builder()
      .transferIndex(1)
      .orgFiscalCode(org.getOrgFiscalCode())
      .orgName(org.getOrgName())
      .amountCents(amount)
      .remittanceInformation(versamento.getCausaleVersamento())
      .category(debtPositionInstallmentService.getCategory(versamento.getDatiSpecificiRiscossione(), versamento.getIdentificativoTipoDovuto(), org.getOrganizationId(), accessToken))
      .build();

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

    InstallmentDTO installment = InstallmentDTO.builder()
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
      .transfers(new ArrayList<>(List.of(transferDTO)))
      .build();

    PaymentOptionDTO paymentOption = PaymentOptionDTO.builder()
      .status(PaymentOptionStatus.UNPAID)
      .paymentOptionIndex(1)
      .paymentOptionType(PaymentOptionTypeEnum.SINGLE_INSTALLMENT)
      .totalAmountCents(amount)
      .installments(List.of(installment))
      .build();

    return DebtPositionDTO.builder()
      .iupdOrg(cartId)
      .status(DebtPositionStatus.UNPAID)
      .debtPositionOrigin(DebtPositionOrigin.SPONTANEOUS_SIL)
      .organizationId(org.getOrganizationId())
      .description("Posizione debitoria " + debtPositionTypeOrg.getDescription())
      .flagIuvVolatile(true)
      .flagPuPagoPaPayment(true)
      .multiDebtor(false)
      .debtPositionTypeOrgId(Objects.requireNonNull(debtPositionTypeOrg.getDebtPositionTypeOrgId()))
      .paymentOptions(List.of(paymentOption))
      .build();
  }

  protected MixedDebtPositionDTO mixedDebtPositionDTOMapper(RegistryEventType operationType, String cartId, Dovuti dovutiObj, Organization org, String accessToken) {
    validationService.validateCartSize(dovutiObj.getDatiVersamento().getDatiSingoloVersamentos().size());
    PersonDTO debtor = personMapper.getAndValidateDebtor(dovutiObj.getSoggettoPagatore());

    String sourceFlowName = Constants.SOURCE_FLOW_NAME_PREFIX_INVIADOVUTI + cartId;
    if (RegistryEventType.PTDP_paaSILInviaCarrelloDovuti.equals(operationType)) {
      sourceFlowName = Constants.SOURCE_FLOW_NAME_PREFIX_INVIACARRELLODOVUTI + cartId;
    }

    List<MixedTransferDTO> mixedTransfers = new ArrayList<>();
    for (CtDatiSingoloVersamentoDovuti singleTranfer : dovutiObj.getDatiVersamento().getDatiSingoloVersamentos()) {
      DebtPositionTypeOrg debtPositionTypeOrg = debtPositionTypeService.getDebtPositionTypeOrgByOrgIdAndType(
        org.getOrganizationId(), singleTranfer.getIdentificativoTipoDovuto(), accessToken);

      MixedTransferDTO mixedTransferDTO = MixedTransferDTO.builder()
        .iud(singleTranfer.getIdentificativoUnivocoDovuto())
        .debtPositionTypeOrgId(debtPositionTypeOrg.getDebtPositionTypeOrgId())
        .amountCents(ConversionUtils.bigDecimalEuroAmountToCentsAmount(singleTranfer.getImportoSingoloVersamento()))
        .balance(Optional.ofNullable(singleTranfer.getBilancio())
          .map(b -> jaxbTransformService.marshalling(b, Bilancio.class))
          .orElse(null))
        .legacyPaymentMetadata(singleTranfer.getDatiSpecificiRiscossione())
        .build();

      Optional.ofNullable(singleTranfer.getDatiMarcaBolloDigitale()).ifPresentOrElse(
        stamp -> mixedTransferDTO
          .stampHashDocument(stamp.getHashDocumento())
          .stampProvincialResidence(stamp.getProvinciaResidenza())
          .stampType(stamp.getTipoBollo()),
        () -> {
          if (StringUtils.isAllBlank(debtPositionTypeOrg.getIban(), debtPositionTypeOrg.getPostalIban())) {
            mixedTransferDTO.iban(org.getIban())
              .postalIban(org.getPostalIban());
          } else {
            mixedTransferDTO.iban(debtPositionTypeOrg.getIban())
              .postalIban(debtPositionTypeOrg.getPostalIban());
          }
        });
      mixedTransfers.add(mixedTransferDTO);
    }
    //TODO: P4ADEV-3958 move remittanceInformation
    return MixedDebtPositionDTO.builder()
      .organizationId(org.getOrganizationId())
      .debtPositionOrigin(DebtPositionOrigin.SPONTANEOUS_SIL)
      .sourceFlowName(sourceFlowName)
      .flagIuvVolatile(true)
      .description("MIXED")
      .remittanceInformation(dovutiObj.getDatiVersamento().getDatiSingoloVersamentos().getFirst().getCausaleVersamento())
      .dueDate(Utilities.getSpontaneousSilExpirationDate())
      .debtor(debtor)
      .transfers(mixedTransfers)
      .build();
  }
}
