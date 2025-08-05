package it.gov.pagopa.pu.sil.service.debtposition;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.service.querypayments.PaymentStatusRequest;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DebtPositionInstallmentFacadeService {
  private final DebtPositionInstallmentService debtPositionInstallmentService;

  public DebtPositionInstallmentFacadeService(DebtPositionInstallmentService debtPositionInstallmentService) {
    this.debtPositionInstallmentService = debtPositionInstallmentService;
  }

  public List<Pair<DebtPositionDTO, InstallmentDTO>> fetch(PaymentStatusRequest request,
                                                           Organization organization,
                                                           String accessToken) {
    return switch (request.idType()) {
      case INSTALLMENT_ID -> debtPositionInstallmentService.getDebtPositionsAndInstallmentsByInstallmentId(
          request, accessToken);
      case IUD -> debtPositionInstallmentService.getDebtPositionsAndInstallmentsByIud(
          request, organization, accessToken);
      case NOTICE_NUMBER -> debtPositionInstallmentService.getDebtPositionsAndInstallmentsByIuv(
          request, organization, accessToken);
    };
  }
}
