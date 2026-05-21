package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.util.Utilities;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class DebtPositionMapper {
  public static final String IMPORT_DOVUTO = "_IMPORT-DOVUTO_";

  private final DebtPositionTypeService debtPositionTypeService;
  private final TransferMapper transferMapper;
  private final String auxDigit;

  public DebtPositionMapper(DebtPositionTypeService debtPositionTypeService,
                            TransferMapper transferMapper,
                            @Value("${nav.aux-digit}") String auxDigit) {
    this.debtPositionTypeService = debtPositionTypeService;
    this.transferMapper = transferMapper;
    this.auxDigit = auxDigit;
  }

  public DebtPositionDTO mapRequestToDebtPosition(it.gov.pagopa.pu.sil.dto.generated.DebtPositionDTO source, Organization organization, String accessToken) {//TODO P4ADEV-4720 handle stationId mapping
    DebtPositionTypeOrg debtPositionTypeOrg = debtPositionTypeService.getDebtPositionTypeOrgByOrgIdAndType(
      organization.getOrganizationId(), source.getDebtPositionTypeOrgCode(), accessToken);

    return DebtPositionDTO.builder()
      .iupdOrg(source.getIupdOrg())
      .validityDate(source.getValidityDate())
      .status(DebtPositionStatus.UNPAID)
      .organizationId(organization.getOrganizationId())
      .debtPositionOrigin(DebtPositionOrigin.ORDINARY_SIL)
      .debtPositionTypeOrgId(debtPositionTypeOrg.getDebtPositionTypeOrgId())
      .description(source.getDescription())
      .flagPuPagoPaPayment(source.getFlagPagoPaPayment())
      .multiDebtor(source.getMultiDebtor())
      .paymentOptions(source.getPaymentOptions().stream()
        .map(po -> fillPaymentOptionFields(po, organization.getIpaCode()))
        .toList())
      .build();
  }

  private PaymentOptionDTO fillPaymentOptionFields(it.gov.pagopa.pu.sil.dto.generated.PaymentOptionDTO source, String orgIpaCode) {
    return PaymentOptionDTO.builder()
      .paymentOptionType(PaymentOptionType.SINGLE_INSTALLMENT)
      .paymentOptionIndex(source.getPaymentOptionIndex())
      .description(source.getDescription())
      .totalAmountCents(source.getTotalAmountCents())
      .status(PaymentOptionStatus.UNPAID)
      .installments(source.getInstallments().stream()
        .map(i -> mapSilInstallmentToInstallmentDTO(i, orgIpaCode))
        .toList())
      .build();
  }

  public InstallmentDTO mapSilInstallmentToInstallmentDTO(it.gov.pagopa.pu.sil.dto.generated.InstallmentDTO source, String orgIpaCode) {
    String now = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    return InstallmentDTO.builder()
      .status(InstallmentStatus.UNPAID)
      .iud(source.getIud())
      .iuv(source.getIuv())
      .nav(Utilities.iuv2Nav(source.getIuv(), auxDigit))
      .amountCents(source.getAmountCents())
      .dueDate(source.getDueDate())
      .remittanceInformation(source.getRemittanceInformation())
      .legacyPaymentMetadata(source.getLegacyPaymentMetadata())
      .balance(source.getBalance())
      .debtor(source.getDebtor())
      .generateNotice(false)
      .sourceFlowName(orgIpaCode + IMPORT_DOVUTO + now)
      .transfers(source.getTransfers().stream().map(transferMapper::mapToDebtPositionTransferDTO).toList())
      .build();
  }
}
