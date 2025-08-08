package it.gov.pagopa.pu.sil.service.singleimport;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.ManageInstallmentDTO;
import it.gov.pagopa.pu.sil.mapper.DebtPositionMapper;
import it.gov.pagopa.pu.sil.mapper.PaaSILImportaDovutoMapper;
import it.gov.pagopa.pu.sil.service.debtposition.ManageDebtPositionService;
import it.gov.pagopa.pu.sil.service.notice.NoticeService;
import jakarta.activation.DataHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DebtPositionInstallmentsHandlerService extends BaseDebtPositionHandler<it.gov.pagopa.pu.sil.dto.generated.ManageDebtPositionDTO, DebtPositionDTO> {
  private final DebtPositionMapper debtPositionMapper;
  private final PaaSILImportaDovutoMapper paaSILImportaDovutoMapper;

  public DebtPositionInstallmentsHandlerService(OrganizationService organizationService,
                                                DebtPositionService debtPositionService,
                                                ManageDebtPositionService manageDebtPositionService,
                                                NoticeService noticeService,
                                                DebtPositionMapper debtPositionMapper,
                                                PaaSILImportaDovutoMapper paaSILImportaDovutoMapper) {
    super(organizationService, debtPositionService, manageDebtPositionService, noticeService);
    this.debtPositionMapper = debtPositionMapper;
    this.paaSILImportaDovutoMapper = paaSILImportaDovutoMapper;
  }

  @Override
  protected Pair<DebtPositionDTO, String> mapRequestToDebtPosition(it.gov.pagopa.pu.sil.dto.generated.ManageDebtPositionDTO request, Organization organization, String accessToken) {
    ManageInstallmentDTO manageInstallmentDTO = request.getInstallments().getFirst();
    InstallmentDTO installmentToSync = debtPositionMapper
      .mapSilInstallmentToInstallmentDTO(manageInstallmentDTO.getInstallment(), organization.getIpaCode());
    // build debtPositionDTO just to provide installment properly
    DebtPositionDTO debtPositionDTO = initDebtPosition(organization.getOrganizationId(), installmentToSync);
    return Pair.of(debtPositionDTO, manageInstallmentDTO.getAction().getValue());
  }

  @Override
  protected DebtPositionDTO mapToRespone(DebtPositionDTO debtPosition, String orgFiscalCode, DataHandler notice, String action) {
    return debtPosition;
  }

  @Override
  protected ManageDebtPositionDTO mapToManageDebtPositionDTO(DebtPositionDTO debtPositionOnDb, InstallmentDTO installmentToSync, String action) {
    return paaSILImportaDovutoMapper.mapToManageDebtPositionDTO(debtPositionOnDb, installmentToSync, action);
  }

  private DebtPositionDTO initDebtPosition(Long orgId, InstallmentDTO installmentDTO) {
    return DebtPositionDTO.builder()
      .status(DebtPositionStatus.UNPAID)
      .debtPositionOrigin(DebtPositionOrigin.SPONTANEOUS_SIL)
      .organizationId(orgId)
      .flagPuPagoPaPayment(true)
      .debtPositionTypeOrgId(0L)
      .paymentOptions(List.of(PaymentOptionDTO.builder()
        .status(PaymentOptionStatus.UNPAID)
        .paymentOptionIndex(1)
        .paymentOptionType(PaymentOptionTypeEnum.SINGLE_INSTALLMENT)
        .totalAmountCents(0L)
        .installments(List.of(installmentDTO))
        .build()))
      .build();
  }
}
