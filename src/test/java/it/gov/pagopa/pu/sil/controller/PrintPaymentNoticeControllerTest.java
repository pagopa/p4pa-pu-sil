package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.notice.NoticeService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrintPaymentNoticeControllerTest {
  @Mock
  private NoticeService noticeServiceMock;

  @InjectMocks
  private PrintPaymentNoticeController controller;

  private final Long orgId = 123L;
  private final String accessToken = "fakeAccessToken";

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    UserInfo userInfo = podamFactory.manufacturePojo(UserInfo.class);
    userInfo.getOrganizations().getFirst().setOrganizationId(orgId);
    userInfo.getOrganizations().getFirst().setRoles(List.of(AuthorizationService.ROLE_ADMIN));

    Authentication authentication = new UsernamePasswordAuthenticationToken(userInfo, accessToken);
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  void whenNotifyPaymentThenOk() {
    // Given
    String iuv = "IUV";

    Resource expectedResource = new ByteArrayResource("fakePDFContent".getBytes());

    when(noticeServiceMock.generateNoticeByIuv(orgId, iuv, accessToken))
      .thenReturn(expectedResource);


    // When Then
    ResponseEntity<Resource> response = controller.generateNotice(orgId, iuv);

    //Verify
    Assertions.assertEquals(expectedResource, response.getBody());
  }
}
