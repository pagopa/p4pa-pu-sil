package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.gov.pagopa.pu.sil.util.PersonValidationUtils;
import it.gov.pagopa.pu.sil.util.Utilities;
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
  protected final DebtPositionService debtPositionService;
  protected final ValidationService validationService;
  protected final PersonMapper personMapper;

  protected List<DebtPositionDTO> dovutiMapper(RegistryEventType operationType, String cartId, Dovuti dovutiObj, Organization organization, String accessToken){
    if(!RegistryEventType.PTDP_paaSILInviaDovuti.equals(operationType) &&
      !RegistryEventType.PTDP_paaSILInviaCarrelloDovuti.equals(operationType)){
      throw new ApplicationException("invalid operation type: " + operationType);
    }
    if (StringUtils.isNotBlank(dovutiObj.getDatiVersamento().getIdentificativoUnivocoVersamento())) {
      throw new SilFaultException(SilFaults.PAA_IUV_NON_VALIDO, "L'inserimento dello IUV è deprecato");
    }

    if (dovutiObj.getDatiVersamento().getDatiSingoloVersamentos().size() > Constants.MAX_CART_SIZE) {
      throw new SilFaultException(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_CARRELLO, "Numero massimo dovuti nel carrello superato: " +
        dovutiObj.getDatiVersamento().getDatiSingoloVersamentos().size() + "/" + Constants.MAX_CART_SIZE);
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

    DebtPositionTypeOrg debtPositionTypeOrg = debtPositionService.getDebtPositionTypeOrgByOrgIdAndType(
      org.getOrganizationId(), versamento.getIdentificativoTipoDovuto(), accessToken);

    validationService.validateDebtPositionTypeOrg(debtPositionTypeOrg, versamento.getIdentificativoTipoDovuto());
    PersonValidationUtils.validateAnonymousDebtor(debtPositionTypeOrg, debtor);
    validationService.validateStamp(versamento.getDatiMarcaBolloDigitale(), versamento.getIdentificativoTipoDovuto());
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
    debtPosition.setDescription(versamento.getCausaleVersamento());
    PaymentOptionDTO paymentOption = debtPosition.getPaymentOptions().getFirst();
    paymentOption.setDescription(versamento.getCausaleVersamento());
    paymentOption.setTotalAmountCents(amount);
    //TODO: since first transfer of the installment is not passed, as per P4ADEV-2407, there is currently no way
    // to set stamp-related data. This needs to be fixed in the future, when the stamp handling will be defined.
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
        .build()
    ));
  }
}
