package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.controller.generated.PrintPaymentNoticeApi;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.notice.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PrintPaymentNoticeController implements PrintPaymentNoticeApi {

  private final NoticeService noticeService;

  @Override
  public ResponseEntity<Resource> generateNotice(String orgFiscalCode, String iuv) {

    //check user is authorized to access the resource
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    Long organizationId = AuthorizationService.getOrganizationIdFromOrgFiscalCode(userInfo, orgFiscalCode);
    AuthorizationService.validateAdminRole(organizationId, userInfo);
    String accessToken = SecurityUtils.getAccessToken();

    //generate the payment notice PDF
    Resource pdfResource = noticeService.generateNoticeByIuv(organizationId, iuv, accessToken);

    //return it as a response
    return ResponseEntity.ok(pdfResource);
  }


}
