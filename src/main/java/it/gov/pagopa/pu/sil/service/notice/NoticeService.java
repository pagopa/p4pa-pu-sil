package it.gov.pagopa.pu.sil.service.notice;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionOrigin;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.pagopapayments.PagopaPaymentsService;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NoticeService {

  public static final List<DebtPositionOrigin> ALLOWED_ORIGINS = List.of(
    DebtPositionOrigin.ORDINARY,
    DebtPositionOrigin.ORDINARY_SIL
  );

  private final DebtPositionService debtPositionService;
  private final PagopaPaymentsService pagopaPaymentsService;

  public byte[] generateNotice(String iuv, DebtPositionDTO debtPositionDTO, String accessToken) {
    return Optional.ofNullable(pagopaPaymentsService.generateNotice(iuv, debtPositionDTO, accessToken))
      .map(resource -> {
        try {
          return resource.getContentAsByteArray();
        } catch (IOException ioe) {
          throw new ApplicationException(ioe);
        }
      })
      .orElseThrow(() -> new ApplicationException("notice not found for org["+debtPositionDTO.getOrganizationId()+"] iuv["+iuv+"]"));
  }

  public Resource generateNoticeByIuv(Long organizationId, String iuv, String accessToken) {
    //retrieve debt position by organizationId and IUV
    DebtPositionDTO debtPosition = debtPositionService.getDebtPositionsByOrganizationIdAndIuv(organizationId, iuv, ALLOWED_ORIGINS, accessToken)
      .stream()
      .filter(dp -> dp.getPaymentOptions().stream()
        .flatMap(po -> po.getInstallments().stream())
        .anyMatch(installment -> iuv.equals(installment.getIuv()) && InstallmentStatus.UNPAID.equals(installment.getStatus())))
      .findFirst()
      .orElseThrow(() -> new PaymentNotFoundException("No installment found for IUV: " + iuv));

    //generate the payment notice PDF
    Resource pdfResource = pagopaPaymentsService.generateNotice(iuv, debtPosition, accessToken);
    if(pdfResource == null) {
      throw new ApplicationException("Error generating notice for IUV: " + iuv);
    }
    return pdfResource;
  }

}
