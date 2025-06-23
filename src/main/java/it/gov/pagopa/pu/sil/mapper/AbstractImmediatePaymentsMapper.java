package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.veneto.regione.schemas._2012.pagamenti.ente.Bilancio;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

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

  protected Triple<List<DebtPositionDTO>, SilFaults, String> dovutiMapper(RegistryEventType operationType, String cartId, Dovuti dovutiObj, Organization organization, String accessToken){
    if(!RegistryEventType.paaSILInviaDovuti.equals(operationType) &&
      !RegistryEventType.paaSILInviaCarrelloDovuti.equals(operationType)){
      throw new ApplicationException("invalid operation type: " + operationType);
    }
    if (StringUtils.isNotBlank(dovutiObj.getDatiVersamento().getIdentificativoUnivocoVersamento())) {
      return Triple.of(null, SilFaults.PAA_IUV_NON_VALIDO, "L'inserimento dello IUV è deprecato");
    }

    if (dovutiObj.getDatiVersamento().getDatiSingoloVersamentos().size() > Constants.MAX_CART_SIZE) {
      return Triple.of(null, SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_CARRELLO, "Numero massimo dovuti nel carrello superato: " +
        dovutiObj.getDatiVersamento().getDatiSingoloVersamentos().size() + "/" + Constants.MAX_CART_SIZE);
    }

    Triple<PersonDTO, SilFaults, String> debtorResult = personMapper.getAndValidateDebtor(dovutiObj.getSoggettoPagatore());
    if (debtorResult.getMiddle() != null) {
      return Triple.of(null, debtorResult.getMiddle(), debtorResult.getRight());
    }

    List<DebtPositionDTO> debtPositions = new ArrayList<>();
    int idx = 1;
    for (CtDatiSingoloVersamentoDovuti versamento : dovutiObj.getDatiVersamento().getDatiSingoloVersamentos()) {
      DebtPositionDTO debtPosition = initDebtPosition(organization.getOrganizationId());
      Pair<SilFaults, String> fault = fillAndValidateVersamentoFieldsOfDebtPosition(operationType, idx++, debtPosition,
        debtorResult.getLeft(), organization, versamento, cartId, accessToken);
      if (fault != null) {
        return Triple.of(null, fault.getLeft(), fault.getRight());
      }
      debtPositions.add(debtPosition);
    }

    return Triple.of(debtPositions, null, null);
  }


  protected DebtPositionDTO initDebtPosition(Long orgId) {
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

  protected Pair<SilFaults, String> fillAndValidateVersamentoFieldsOfDebtPosition(RegistryEventType operationType, int idx,
                                                                                  DebtPositionDTO debtPosition, PersonDTO debtor,
                                                                                  Organization org, CtDatiSingoloVersamentoDovuti versamento,
                                                                                  String cartId, String accessToken) {

    DebtPositionTypeOrg debtPositionTypeOrg = debtPositionService.getDebtPositionTypeOrgByOrgIdAndType(
      org.getOrganizationId(), versamento.getIdentificativoTipoDovuto(), accessToken);

    Pair<SilFaults, String> fault = Optional.ofNullable(validationService.validateDebtPositionTypeOrg(debtPositionTypeOrg, versamento.getIdentificativoTipoDovuto()))
      .or(() -> Optional.ofNullable(personMapper.validateAnonymousDebtor(debtPositionTypeOrg, debtor)))
      .or(() -> Optional.ofNullable(validationService.validateStamp(versamento.getDatiMarcaBolloDigitale(), versamento.getIdentificativoTipoDovuto())))
      .or(() -> Optional.ofNullable(validationService.validatePaymentData(versamento)))
      .or(() -> Optional.ofNullable(validationService.validateIud(org.getOrganizationId(), versamento.getIdentificativoUnivocoDovuto(), accessToken)))
      .orElse(null);
    if (fault != null) {
      return fault;
    }

    String balance = Optional.ofNullable(versamento.getBilancio())
      .map(b -> jaxbTransformService.marshalling(b, Bilancio.class))
      .orElse(debtPositionTypeOrg.getBalance());

    Long amount = ConversionUtils.bigDecimalEuroAmountToCentsAmount(versamento.getImportoSingoloVersamento());

    String sourceFlowName = operationType.equals(RegistryEventType.paaSILInviaCarrelloDovuti) ?
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

    return null;
  }
}
