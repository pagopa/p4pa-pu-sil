package it.gov.pagopa.pu.sil.service.notice;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.pagopapayments.PagopaPaymentsService;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.PaymentNotFoundException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NoticeService {

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

  public Resource generateNoticeByIuv(String orgFiscalCode, String iuv, UserInfo userInfo, String accessToken) {
    //check user is authorized to access the resource
    Long organizationId = AuthorizationService.getOrganizationIdFromOrgFiscalCode(userInfo, orgFiscalCode);
    AuthorizationService.validateAdminRole(organizationId, userInfo);

    //retrieve debt position by organizationId and IUV
    DebtPositionDTO debtPosition = debtPositionService.getDebtPositionsByOrganizationIdAndIuv(organizationId, iuv, Constants.PRINT_NOTICE_ALLOWED_ORIGINS, accessToken)
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
