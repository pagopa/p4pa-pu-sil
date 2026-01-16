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

    if (debtPositionTypeOrg == null) {
      throw new SilFaultException(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, "Identificativo tipo dovuto non valido");
    }

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
      .paymentOptionType(PaymentOptionType.SINGLE_INSTALLMENT)
      .totalAmountCents(amount)
      .installments(List.of(installment))
      .build();

    return DebtPositionDTO.builder()
      .iupdOrg(cartId)
      .status(DebtPositionStatus.UNPAID)
      .debtPositionOrigin(DebtPositionOrigin.SPONTANEOUS_SIL)
      .organizationId(Objects.requireNonNull(org.getOrganizationId()))
      .description("Posizione debitoria " + debtPositionTypeOrg.getDescription())
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
    for (CtDatiSingoloVersamentoDovuti singleTransfer : dovutiObj.getDatiVersamento().getDatiSingoloVersamentos()) {
      DebtPositionTypeOrg debtPositionTypeOrg = debtPositionTypeService.getDebtPositionTypeOrgByOrgIdAndType(
        org.getOrganizationId(), singleTransfer.getIdentificativoTipoDovuto(), accessToken);

      MixedTransferDTO mixedTransferDTO = MixedTransferDTO.builder()
        .iud(singleTransfer.getIdentificativoUnivocoDovuto())
        .debtPositionTypeOrgId(Objects.requireNonNull(debtPositionTypeOrg.getDebtPositionTypeOrgId()))
        .amountCents(ConversionUtils.bigDecimalEuroAmountToCentsAmount(singleTransfer.getImportoSingoloVersamento()))
        .balance(Optional.ofNullable(singleTransfer.getBilancio())
          .map(b -> jaxbTransformService.marshalling(b, Bilancio.class))
          .orElse(null))
        .legacyPaymentMetadata(singleTransfer.getDatiSpecificiRiscossione())
        .remittanceInformation(singleTransfer.getCausaleVersamento())
        .build();

      Optional.ofNullable(singleTransfer.getDatiMarcaBolloDigitale()).ifPresentOrElse(
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

    return MixedDebtPositionDTO.builder()
      .organizationId(Objects.requireNonNull(org.getOrganizationId()))
      .debtPositionOrigin(DebtPositionOrigin.SPONTANEOUS_SIL)
      .sourceFlowName(sourceFlowName)
      .description("MIXED")
      .dueDate(Utilities.getSpontaneousSilExpirationDate())
      .debtor(debtor)
      .transfers(mixedTransfers)
      .build();
  }
}
