package it.gov.pagopa.pu.sil.service.inbound.payments.notice;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.pagopapayments.PagopaPaymentsService;
import it.gov.pagopa.pu.sil.exception.common.IllegalStateBusinessException;
import it.gov.pagopa.pu.sil.exception.PaymentNotFoundException;
import it.gov.pagopa.pu.sil.exception.common.NotFoundException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
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

  public byte[] generateNotice(String nav, DebtPositionDTO debtPositionDTO, String accessToken) {
    return Optional.ofNullable(pagopaPaymentsService.generateNotice(nav, debtPositionDTO, accessToken))
      .map(resource -> {
        try {
          return resource.getContentAsByteArray();
        } catch (IOException ioe) {
          throw new IllegalStateBusinessException(ErrorCodeConstants.ERROR_CODE_GENERATE_NOTICE_ERROR, "Something went wrong while generating notice for nav " + nav + " of organization " + debtPositionDTO.getOrganizationId() ,ioe);
        }
      })
      .orElseThrow(() -> new NotFoundException(ErrorCodeConstants.ERROR_CODE_NOTICE_NOT_FOUND, "Notice not found for org["+debtPositionDTO.getOrganizationId()+"] nav["+nav+"]"));
  }

  public Resource generateNoticeByIuv(String orgFiscalCode, String iuv, UserInfo userInfo, String accessToken) {
    //check user is authorized to access the resource
    Long organizationId = AuthorizationService.getOrganizationIdFromOrgFiscalCode(userInfo, orgFiscalCode);
    AuthorizationService.validateAdminRole(organizationId, userInfo);

    //retrieve debt position by organizationId and IUV
    Pair<DebtPositionDTO, InstallmentDTO> debtPositionWithInstallment = debtPositionService
      .getDebtPositionsByOrganizationIdAndIuv(organizationId, iuv, Constants.PRINT_NOTICE_ALLOWED_ORIGINS, accessToken)
      .stream()
      .flatMap(dp -> dp.getPaymentOptions().stream()
        .flatMap(po -> po.getInstallments().stream())
        .filter(installment -> iuv.equals(installment.getIuv()) && InstallmentStatus.UNPAID.equals(installment.getStatus()))
        .map(installment -> Pair.of(dp, installment)))
      .findFirst()
      .orElseThrow(() -> new PaymentNotFoundException("No installment found for IUV: " + iuv));

    String nav = debtPositionWithInstallment.getRight().getNav();
    //generate the payment notice PDF
    Resource pdfResource = pagopaPaymentsService.generateNotice(nav, debtPositionWithInstallment.getLeft(), accessToken);
    if(pdfResource == null) {
      throw new NotFoundException(ErrorCodeConstants.ERROR_CODE_NOTICE_NOT_FOUND, "Notice not found for NAV: " + nav);
    }
    return pdfResource;
  }

}
