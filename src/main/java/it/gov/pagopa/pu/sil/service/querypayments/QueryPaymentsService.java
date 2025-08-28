package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.PaymentStatusResponseDTO;
import it.gov.pagopa.pu.sil.mapper.ReceiptMapper;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionInstallmentFacadeService;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class QueryPaymentsService extends AbstractQueryPaymentsService<PaymentStatusRequest, PaymentStatusResponseDTO> {
  private final ReceiptMapper receiptMapper;
  private final ReceiptService receiptService;

  public QueryPaymentsService(OrganizationService organizationService,
                              DebtPositionInstallmentFacadeService debtPositionInstallmentFacadeService,
                              ReceiptMapper receiptMapper,
                              ReceiptService receiptService) {
    super(organizationService, debtPositionInstallmentFacadeService);
    this.receiptMapper = receiptMapper;
    this.receiptService = receiptService;
  }

  @Override
  protected void validateInstallmentStatus(InstallmentDTO installment) {
    // no validation needed for installment status in this context,
    // it's handled in mapper method returing the status itself in the response
  }

  @Override
  protected String getOrgIpaCode(PaymentStatusRequest request) {
    return request.organizationIpaCode();
  }

  @Override
  protected PaymentStatusResponseDTO mapper(List<Pair<DebtPositionDTO, InstallmentDTO>> debtPositionWithInstallmentList,
                                            Organization organization, String accessToken, PaymentStatusRequest request) {
    InstallmentDTO installmentDTO = debtPositionWithInstallmentList.getFirst().getRight();
    PaymentStatusResponseDTO response = new PaymentStatusResponseDTO()
      .paymentId(installmentDTO.getInstallmentId().toString())
      .lastUpdateDateTime(installmentDTO.getUpdateDate());

    InstallmentStatus status = installmentDTO.getStatus() == InstallmentStatus.TO_SYNC
      ? installmentDTO.getSyncStatus().getSyncStatusTo()
      : installmentDTO.getStatus();

    if (status == InstallmentStatus.UNPAID || status == InstallmentStatus.EXPIRED) {
      return response.status(status);
    }
    if (status != InstallmentStatus.PAID && status != InstallmentStatus.REPORTED) {
      return response.status(InstallmentStatus.UNPAYABLE);
    }
    response.setStatus(status);
    response.setReceipt(receiptMapper.map2ReceiptWithAdditionalNodeDataDTO(installmentDTO, accessToken));
    if (request.withReceiptBytes()) {
      byte[] encodedReceipt = receiptService.getReceiptById(installmentDTO.getReceiptId(), organization.getOrganizationId(), accessToken);
      response.setReceiptBytes(encodedReceipt);
    }
    return response;
  }

  @Override
  protected PaymentStatusRequest validateAndTransformRequest(PaymentStatusRequest request, String orgIpaCode) {
    // no transformation needed, the request is already in the expected format in this scenario
    return request;
  }
}
