package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.dto.generated.InstantPaymentRequest;
import it.gov.pagopa.pu.sil.dto.generated.PaymentResponse;
import it.gov.pagopa.pu.sil.dto.generated.PaymentStatusResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.QueryPaymentStatusType;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.RegistryLoggerTest;
import it.gov.pagopa.pu.sil.security.SecurityUtilsTest;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.immediatepayments.InstantPaymentService;
import it.gov.pagopa.pu.sil.service.immediatepayments.VerifyNoticeService;
import it.gov.pagopa.pu.sil.service.querypayments.PaymentStatusRequest;
import it.gov.pagopa.pu.sil.service.querypayments.QueryPaymentsService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.co.jemos.podam.api.PodamFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {
  @Mock
  private QueryPaymentsService queryPaymentsServiceMock;
  @Mock
  private VerifyNoticeService verifyNoticeServiceMock;
  @Mock
  private InstantPaymentService instantPaymentServiceMock;
  @Mock
  private RegistryLogger registryLoggerMock;

  @InjectMocks
  private PaymentController controller;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();
  private final String accessToken = "fakeAccessToken";
  private final String orgFiscalCode = "ORG123456789";
  private final String orgIpaCode = "userIpaCode";
  private UserInfo userInfo;

  @BeforeEach
  void setUp() {
    userInfo = AuthorizationServiceTest.buildAdminUser(1L, orgFiscalCode, orgIpaCode);
    userInfo.setMappedExternalUserId("fakeExternalUser");
    SecurityUtilsTest.configureSecurityContext(accessToken, userInfo);
  }

  @AfterEach
  void clear() { SecurityUtilsTest.clearSecurityContext(); }

  @Test
  void whenRequestInstantPaymentThenOk() {
    // Given
    InstantPaymentRequest instantPaymentRequest = podamFactory.manufacturePojo(InstantPaymentRequest.class);
    Triple<PaymentResponse, String, RegistryOutcome> expectedResult = Triple.of(
      PaymentResponse.builder()
        .outcome(PaymentResponse.OutcomeEnum.OK)
        .triggerPaymentUrl("https://example.com/checkout")
        .paymentId("sessionId123")
        .build(),
      "30123456789",
      RegistryOutcome.OK
    );
    RegistryContextData expectedContextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.PTDP_paaSILInviaDovuti)
      .loggedUser(userInfo)
      .build();
    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, expectedContextData, instantPaymentRequest, false, false);

    when(instantPaymentServiceMock.processRequest(
      instantPaymentRequest, orgIpaCode, userInfo, accessToken))
      .thenReturn(expectedResult);

    // When
    ResponseEntity<PaymentResponse> response = controller.requestInstantPayment(
      orgFiscalCode, instantPaymentRequest);
    // Then
    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertSame(expectedResult.getLeft(), response.getBody());
  }

  @Test
  void whenRequestNoticePaymentThenOk() {
    // Given
    String nav = "30123456789";
    String callbackUrl = "https://example.com/callback";
    RegistryContextData expectedContextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.PTDP_paaSILVerificaAvviso)
      .loggedUser(userInfo)
      .build();
    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, expectedContextData, nav, false, false);

    PaymentResponse expectedResult = PaymentResponse.builder()
        .outcome(PaymentResponse.OutcomeEnum.OK)
        .triggerPaymentUrl("https://example.com/checkout")
        .paymentId("sessionId123")
        .build();

    Pair<String, String> request = Pair.of(nav, callbackUrl);
    when(verifyNoticeServiceMock.processRequest(request, orgIpaCode, userInfo, accessToken))
      .thenReturn(expectedResult);

    // When
    ResponseEntity<PaymentResponse> response = controller.requestNoticePayment(orgFiscalCode, nav, callbackUrl);

    // Then
    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertSame(expectedResult, response.getBody());
  }

  @Test
  void whenPaymentStatusThenOk() {
    // Given
    QueryPaymentStatusType idType = QueryPaymentStatusType.INSTALLMENT_ID;
    String id = "ID_SESSION_123";
    boolean withReceiptBytes = true;

    PaymentStatusResponseDTO expectedResult = mock(PaymentStatusResponseDTO.class);

    PaymentStatusRequest request = new PaymentStatusRequest(orgIpaCode, idType, id, withReceiptBytes);
    when(queryPaymentsServiceMock.processRequest(request, userInfo, accessToken))
      .thenReturn(expectedResult);

    // When
    ResponseEntity<PaymentStatusResponseDTO> response = controller.paymentStatus(orgFiscalCode, idType, id, withReceiptBytes);

    // Then
    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertSame(expectedResult, response.getBody());
  }
}
