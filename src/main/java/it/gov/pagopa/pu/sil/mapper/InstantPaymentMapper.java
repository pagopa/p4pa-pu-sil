package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.dto.generated.InstantPaymentRequest;
import it.gov.pagopa.pu.sil.dto.generated.PaymentDTO;
import it.gov.pagopa.pu.sil.dto.generated.TransferDTO;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class InstantPaymentMapper {
  private final DebtPositionTypeService debtPositionTypeService;
  private final TransferMapper transferMapper;

  public InstantPaymentMapper(DebtPositionTypeService debtPositionTypeService,
                              TransferMapper transferMapper) {
    this.debtPositionTypeService = debtPositionTypeService;
    this.transferMapper = transferMapper;
  }

  public List<DebtPositionDTO> mapRequestToDebtPositions(InstantPaymentRequest request, Organization organization, String cartId, String accessToken) {
    return request.getPayments().stream()
      .flatMap(payment -> payment.getTransfers().stream()
        .map(transfer -> createDebtPosition(payment, organization, transfer, cartId, accessToken))
      ).toList();
  }

  private DebtPositionDTO createDebtPosition(PaymentDTO payment, Organization org, TransferDTO transfer, String cartId, String accessToken) {
    DebtPositionTypeOrg debtPositionTypeOrg = debtPositionTypeService.getDebtPositionTypeOrgByOrgIdAndType(
      org.getOrganizationId(), payment.getDebtPositionTypeOrgCode(), accessToken);

    PaymentOptionDTO paymentOption = PaymentOptionDTO.builder()
      .status(PaymentOptionStatus.UNPAID)
      .paymentOptionIndex(1)
      .paymentOptionType(PaymentOptionTypeEnum.SINGLE_INSTALLMENT)
      .totalAmountCents(payment.getTotalAmountCents())
      .description(transfer.getRemittanceInformation())
      .installments(List.of(
        InstallmentDTO.builder()
          .status(InstallmentStatus.UNPAID)
          .iud(payment.getIud())
          .amountCents(payment.getTotalAmountCents())
          .balance(Optional.ofNullable(payment.getBalance()).orElse(debtPositionTypeOrg.getBalance()))
          .dueDate(Utilities.getSpontaneousSilExpirationDate())
          .debtor(payment.getDebtor())
          .remittanceInformation(transfer.getRemittanceInformation())
          .sourceFlowName(Constants.SOURCE_FLOW_NAME_PREFIX_INVIADOVUTI + cartId)
          .generateNotice(false)
          .transfers(payment.getTransfers().stream().map(transferMapper::mapToDebtPositionTransferDTO).toList())
          .build()
      ))
      .build();

    return DebtPositionDTO.builder()
      .status(DebtPositionStatus.UNPAID)
      .debtPositionOrigin(DebtPositionOrigin.SPONTANEOUS_SIL)
      .organizationId(org.getOrganizationId())
      .flagIuvVolatile(true)
      .flagPuPagoPaPayment(true)
      .multiDebtor(false)
      .debtPositionTypeOrgId(Objects.requireNonNull(debtPositionTypeOrg.getDebtPositionTypeOrgId()))
      .iupdOrg(cartId + "-" + transfer.getTransferIndex())
      .description(payment.getDescription())
      .paymentOptions(List.of(paymentOption))
      .build();
  }
}
