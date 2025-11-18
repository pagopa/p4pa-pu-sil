package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class ManageDebtPositionMapper {

  private final SecondaryTransferMapper secondaryTransferMapper;

  public ManageDebtPositionDTO mapToManageDebtPositionDTO(DebtPositionDTO debtPositionOnDb, DebtPositionDTO debtPositionToSync, InstallmentDTO installmentToSync, String action, boolean legacyMode) {
    InstallmentDTO installmentOnDb = debtPositionOnDb.getPaymentOptions()
      .stream().flatMap(po -> po.getInstallments().stream())
      .filter(i -> Objects.equals(i.getIud(), installmentToSync.getIud()))
      .findFirst()
      .orElseThrow(() -> {
        log.error("Installment not found on debtPosition[{}] for organizationId[{}] and iud[{}]", debtPositionOnDb.getDebtPositionId(),
          debtPositionOnDb.getOrganizationId(), installmentToSync.getIud());
        return new SilFaultException(SilFaults.PAA_IMPORT_DOVUTO_NON_PRESENTE, "Dovuto non trovato");
      });

    PaymentOptionDTO paymentOptionOnDb = debtPositionOnDb.getPaymentOptions().stream()
      .filter(po -> Objects.equals(po.getPaymentOptionId(), installmentOnDb.getPaymentOptionId()))
      .findFirst()
      .orElseThrow(); //should never happen, since it has been checked in the previous step; just used to satisfy SonarQube

    if(action.equals(Constants.LEGACY_IMPORT_ACTION_MODIFY)){
      //this is necessary only for action M (modify), for action A (cancel) the only relevant field is installmentId
      fillInstallmentOnDbWithFieldsToSync(installmentOnDb, installmentToSync, legacyMode);
    }

    ManageInstallmentDTO manageInstallmentDTO = ManageInstallmentDTO.builder()
      .action(Action.fromValue(action))
      .installment(installmentOnDb)
      .build();

    return ManageDebtPositionDTO.builder()
      .debtPositionDescription(debtPositionToSync.getDescription())
      .paymentOptionId(paymentOptionOnDb.getPaymentOptionId())
      .paymentOptionDescription(debtPositionToSync.getPaymentOptions().getFirst().getDescription())
      .validityDate(debtPositionOnDb.getValidityDate())
      .installments(List.of(manageInstallmentDTO))
      .build();
  }

  private void fillInstallmentOnDbWithFieldsToSync(InstallmentDTO installmentOnDb, InstallmentDTO installmentToSync, boolean legacyMode) {
    //IUD is used as the key to identify the installment to sync, so it is not modifiable
    installmentOnDb.setIuv(installmentToSync.getIuv());
    installmentOnDb.setNav(installmentToSync.getNav());
    installmentOnDb.setAmountCents(installmentToSync.getAmountCents());
    installmentOnDb.setDueDate(installmentToSync.getDueDate());
    installmentOnDb.setRemittanceInformation(installmentToSync.getRemittanceInformation());
    installmentOnDb.setLegacyPaymentMetadata(installmentToSync.getLegacyPaymentMetadata());
    installmentOnDb.setBalance(installmentToSync.getBalance());
    installmentOnDb.setDebtor(installmentToSync.getDebtor());
    installmentOnDb.setSourceFlowName(installmentToSync.getSourceFlowName());

    //handle transfers
    secondaryTransferMapper.checkAndFillSupportedTransfersConfigurationForModify(installmentOnDb, installmentToSync, legacyMode);
  }
}
