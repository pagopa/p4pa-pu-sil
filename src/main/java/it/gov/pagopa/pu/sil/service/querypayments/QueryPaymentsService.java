package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.PaymentStatusResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.PaymentStatusResponseDTO.StatusEnum;
import it.gov.pagopa.pu.sil.dto.generated.ReceiptWithAdditionalNodeDataDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.mapper.ReceiptMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.debtposition.InstallmentFacadeService;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class QueryPaymentsService extends AbstractQueryPaymentsService<PaymentStatusRequest, PaymentStatusResponseDTO> {
  private SilFaults debtPositionNotFoundFault;

  private final DebtPositionService debtPositionService;
  private final ReceiptMapper receiptMapper;
  private final ReceiptService receiptService;
  private final SessionIdMapper sessionIdMapper;

  public QueryPaymentsService(OrganizationService organizationService,
                              DebtPositionService debtPositionService,
                              ReceiptMapper receiptMapper,
                              ReceiptService receiptService,
                              SessionIdMapper sessionIdMapper) {
    super(organizationService);
    this.debtPositionService = debtPositionService;
    this.receiptMapper = receiptMapper;
    this.receiptService = receiptService;
    this.sessionIdMapper = sessionIdMapper;
  }

  @Override
  protected void validateRequest(PaymentStatusRequest request) {
    // no validation needed, is mutually exclusive due to enum request.getIdType()
  }

  @Override
  protected void validateInstallmentStatus(InstallmentDTO installment) {
    // no validation needed for installment status in this context,
    // it's handled in mapper method returing the status itself in the response
  }

  @Override
  protected SilFaults getFaultForDebtPositionNotFound() {
    return debtPositionNotFoundFault;
  }

  @Override
  protected List<Pair<DebtPositionDTO, InstallmentDTO>> getDebtPositionsAndInstallments(PaymentStatusRequest request, Organization organization, String accessToken) {
    String idParam = request.id();
    switch (request.idType()) {
      case PAYMENT_ID -> {
        debtPositionNotFoundFault = SilFaults.PAA_ID_SESSION_NON_VALIDO;
        return sessionIdMapper.mapSessionIdToInstallmentIds(idParam).stream()
          //search for the debt position by installmentId
          .map(installmentId -> Pair.of(installmentId, debtPositionService.getDebtPositionDTOByInstallmentId(installmentId, accessToken)))
          //find the installment in the debt position
          .map(debtPositionPair -> Pair.of(debtPositionPair.getRight(), findInstallmentOfDebtPosition(debtPositionPair.getRight(),
            installment -> Objects.equals(installment.getInstallmentId(), debtPositionPair.getLeft()))))
          //return the pair of debt position and matching installment
          .toList();
      }
      case IUD -> {
        debtPositionNotFoundFault = SilFaults.PAA_IUD_NON_VALIDO;
        return debtPositionService.getDebtPositionsByOrganizationIdAndIud(
            organization.getOrganizationId(), idParam, InstallmentFacadeService.ALLOWED_ORIGINS, accessToken)
          .stream().filter(dp -> !Objects.equals(dp.getStatus(), DebtPositionStatus.CANCELLED))
          .findFirst()
          .map(debtPosition -> Pair.of(debtPosition, findInstallmentOfDebtPosition(debtPosition,
            installment -> idParam.equals(installment.getIud()))))
          .map(List::of)
          .orElse(List.of());
      }
      case NOTICE_NUMBER ->  {
        debtPositionNotFoundFault = SilFaults.PAA_IUV_NON_VALIDO;
        return debtPositionService.getDebtPositionsByOrganizationIdAndIuv(
            organization.getOrganizationId(), idParam, InstallmentFacadeService.ALLOWED_ORIGINS, accessToken)
          .stream().filter(dp -> !Objects.equals(dp.getStatus(), DebtPositionStatus.CANCELLED))
          .findFirst()
          .map(debtPosition -> Pair.of(debtPosition, findInstallmentOfDebtPosition(debtPosition,
            installment -> idParam.equals(installment.getIuv()))))
          .map(List::of)
          .orElse(List.of());
      }
      default -> {
        debtPositionNotFoundFault = SilFaults.PAA_SYSTEM_ERROR;
        return List.of();
      }
    }
  }

  @Override
  protected String getOrgIpaCode(PaymentStatusRequest request) {
    return request.organizationIpaCode();
  }

  @Override
  protected PaymentStatusResponseDTO mapper(List<Pair<DebtPositionDTO, InstallmentDTO>> debtPositionWithInstallmentList,
                                            Organization organization, String accessToken, PaymentStatusRequest request) {
    //TODO currently support only one debt position and installment, but could be extended to support multiple
    InstallmentDTO installmentDTO = debtPositionWithInstallmentList.getFirst().getRight();
    PaymentStatusResponseDTO response = new PaymentStatusResponseDTO()
      .paymentId(installmentDTO.getInstallmentId().toString())
      .lastUpdateDateTime(installmentDTO.getUpdateDate());

    InstallmentStatus status = installmentDTO.getStatus() == InstallmentStatus.TO_SYNC
      ? installmentDTO.getSyncStatus().getSyncStatusTo()
      : installmentDTO.getStatus();

    if (status == InstallmentStatus.UNPAID) {
      response.setStatus(StatusEnum.UNPAID);
    } else if (status == InstallmentStatus.EXPIRED) {
      response.setStatus(StatusEnum.EXPIRED);
    } else if (status != InstallmentStatus.PAID && status != InstallmentStatus.REPORTED) {
      response.setStatus(StatusEnum.UNPAYABLE);
    } else if (status == InstallmentStatus.PAID) {
      response.setStatus(StatusEnum.PAID);
      ReceiptWithAdditionalNodeDataDTO receiptDTO = receiptMapper.map2ReceiptWithAdditionalNodeDataDTO(installmentDTO, accessToken);
      response.setReceipt(receiptDTO);

      if (request.withReceiptBytes()) {
        byte[] encodedReceipt = receiptService.getReceiptById(installmentDTO.getReceiptId(), organization.getOrganizationId(), accessToken);
        response.setReceiptBytes(new ByteArrayResource(encodedReceipt));
      }
    }
    return response;
  }
}
