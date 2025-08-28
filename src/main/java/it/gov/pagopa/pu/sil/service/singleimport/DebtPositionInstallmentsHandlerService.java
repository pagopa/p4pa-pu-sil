package it.gov.pagopa.pu.sil.service.singleimport;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.ManageDebtPositionWithIudDTO;
import it.gov.pagopa.pu.sil.dto.generated.ManageInstallmentDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
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
public class DebtPositionInstallmentsHandlerService extends BaseDebtPositionHandler<ManageDebtPositionWithIudDTO, DebtPositionDTO> {
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
  protected Pair<DebtPositionDTO, String> mapRequestToDebtPosition(ManageDebtPositionWithIudDTO request, Organization organization, String accessToken) {

    //search installment to sync based on IUD
    List<ManageInstallmentDTO> manageInstallmentDTOList = request.getInstallments().stream().filter(i -> request.getIud().equals(i.getInstallment().getIud())).toList();
    if(manageInstallmentDTOList.size()>1) {
      throw new SilFaultException(SilFaults.PAA_IUD_DUPLICATO, "Dovuto con IUD " + request.getIud() + " non univoco");
    } else if(manageInstallmentDTOList.isEmpty()) {
      throw new SilFaultException(SilFaults.PAA_IUD_NON_VALIDO, "Nessun dovuto trovato con IUD " + request.getIud());
    }

    ManageInstallmentDTO manageInstallmentDTO = manageInstallmentDTOList.getFirst();

    //validate action
    if(!manageInstallmentDTO.getAction().equals(Action.M) && !manageInstallmentDTO.getAction().equals(Action.A)){
      throw new SilFaultException(SilFaults.PAA_AZIONE_NON_VALIDA, "Azione non supportata: " + manageInstallmentDTO.getAction());
    }

    InstallmentDTO installmentToSync = debtPositionMapper
      .mapSilInstallmentToInstallmentDTO(manageInstallmentDTO.getInstallment(), organization.getIpaCode());
    // build debtPositionDTO just to provide installment (values of DebtPosition and PaymentOption are not used in this flow)
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
    //since we need only the installment to sync, we can initialize the debt position with non-nullable values only
    return DebtPositionDTO.builder()
      .debtPositionOrigin(DebtPositionOrigin.SPONTANEOUS_SIL)
      .organizationId(orgId)
      .debtPositionTypeOrgId(0L)
      .flagPuPagoPaPayment(true)
      .paymentOptions(List.of(PaymentOptionDTO.builder()
        .paymentOptionType(PaymentOptionTypeEnum.SINGLE_INSTALLMENT)
        .totalAmountCents(0L)
        .installments(List.of(installmentDTO))
        .build()))
      .build();
  }
}
