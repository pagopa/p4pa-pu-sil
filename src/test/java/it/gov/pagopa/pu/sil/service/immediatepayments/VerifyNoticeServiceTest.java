package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.dto.generated.PaymentResponse;
import it.gov.pagopa.pu.sil.dto.generated.PaymentResponse.OutcomeEnum;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.debtposition.InstallmentFacadeService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyNoticeServiceTest {
  private VerifyNoticeService service;

  @Mock
  private InstallmentFacadeService installmentFacadeServiceMock;
  @Mock
  private CheckoutService checkoutServiceMock;
  @Mock
  private CartRequestMapper cartRequestMapperMock;
  @Mock
  private OrganizationService organizationServiceMock;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  private UserInfo userInfo = null;
  private String orgIpaCode = null;
  private Pair<String, String> request = null;
  private static final String TOKEN = "ACCESS_TOKEN";
  private Organization org = null;
  private Long orgId = null;

  @BeforeEach
  void setUp() {
    Mockito.reset(installmentFacadeServiceMock, checkoutServiceMock, organizationServiceMock, cartRequestMapperMock);

    this. service = new VerifyNoticeService(
      cartRequestMapperMock,
      organizationServiceMock,
      checkoutServiceMock,
      installmentFacadeServiceMock,
      "https://example.com/pu-sil");

    userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", "OTHERIPACODE");
    orgIpaCode = userInfo.getOrganizations().getFirst().getOrganizationIpaCode();
    userInfo.getOrganizations().getFirst().setRoles(List.of("ROLE_X", "ROLE_ADMIN"));
    orgId = userInfo.getOrganizations().getFirst().getOrganizationId();
    org = podamFactory.manufacturePojo(Organization.class);
    org.setOrganizationId(orgId);
    org.setIpaCode(orgIpaCode);
    org.setStatus(OrganizationStatus.ACTIVE);

    String nav = "30123456789";
    String callbackUrl = "https://example.com/callback";
    request = Pair.of(nav, callbackUrl);
  }

  @Test
  void givenNotAuthorizedUserWhenVerifyNoticeServiceThenError() {
    userInfo.getOrganizations().getFirst().setOrganizationIpaCode("INVALID_IPA_CODE");
    Assertions.assertThrows(AuthorizationDeniedException.class, () -> service.processRequest(request, orgIpaCode, userInfo, TOKEN));
  }

  @ParameterizedTest
  @ValueSource(strings = {"not_active_ipa"})
  @NullSource
  void givenInvalidOrganizationWhenVerifyNoticeServiceThenError(String testCase) {
    if(testCase==null){
      org = null;
    } else {
      org.setStatus(OrganizationStatus.DRAFT);
    }
    when(organizationServiceMock.getOrganizationById(anyLong(), anyString())).thenReturn(Optional.ofNullable(org));
    SilFaultException response = Assertions.assertThrows(SilFaultException.class, () -> service.processRequest(request, orgIpaCode, userInfo, TOKEN));
    assertEquals(SilFaults.PAA_ENTE_NON_VALIDO, response.getFault());
  }

  @Test
  void givenInvalidUrlWhenVerifyNoticeServiceThenFault() {
    request = Pair.of(request.getLeft(), "http://");
    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    SilFaultException response = Assertions.assertThrows(SilFaultException.class, () -> service.processRequest(request, orgIpaCode, userInfo, TOKEN));
    //verify
    Assertions.assertEquals(SilFaults.PAA_URL_NON_VALIDA, response.getFault());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void givenNullIUVWhenVerifyNoticeServiceThenFault(String nav) {
    request = Pair.of(nav, request.getRight());
    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> service.processRequest(request, orgIpaCode, userInfo, TOKEN));
    //verify
    Assertions.assertEquals(SilFaults.PAA_IUV_NON_VALIDO, exception.getFault());
    Assertions.assertEquals("Identificativo univoco del versamento non indicato", exception.getDescription());
  }

  @Test
  void givenNotFoundIUVWhenVerifyNoticeServiceThenFault() {
    //given
    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(installmentFacadeServiceMock.getInstallmentsByOrganizationIdAndNav(orgId, request.getLeft(), TOKEN))
      .thenReturn(List.of());

    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> service.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_IUV_NON_VALIDO, exception.getFault());
    Assertions.assertEquals("Nessun avviso pagabile trovato per l'dentificativo univoco del versamento indicato", exception.getDescription());
  }

  @Test
  void givenNotPayableIUVWhenVerifyNoticeServiceThenOutcomeNotFound() {
    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    installmentDTO.setStatus(InstallmentStatus.EXPIRED);
    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(installmentFacadeServiceMock.getInstallmentsByOrganizationIdAndNav(orgId, request.getLeft(), TOKEN))
      .thenReturn(List.of(installmentDTO));

    PaymentResponse response = service.processRequest(request, orgIpaCode, userInfo, TOKEN);
    Assertions.assertEquals(OutcomeEnum.NOT_FOUND, response.getOutcome());
  }

  @Test
  void givenMapCartRequestFaultWhenVerifyNoticeServiceThenFault() {
    //given
    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    installmentDTO.setStatus(InstallmentStatus.UNPAID);
    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(installmentFacadeServiceMock.getInstallmentsByOrganizationIdAndNav(orgId, request.getLeft(), TOKEN)).thenReturn(List.of(installmentDTO));
    when(cartRequestMapperMock.mapInstallmentToCartRequest(same(installmentDTO), eq(org), any(), eq(request.getRight())))
      .thenThrow(new SilFaultException(SilFaults.PAA_URL_NON_VALIDA, "invalid url"));
    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> service.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_URL_NON_VALIDA, exception.getFault());
    Assertions.assertEquals("invalid url", exception.getDescription());
  }

  @Test
  void givenNullCheckoutUrlWhenVerifyNoticeServiceThenException() {
    //given
    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    installmentDTO.setStatus(InstallmentStatus.UNPAID);
    CartRequest cartRequest = podamFactory.manufacturePojo(CartRequest.class);
    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(installmentFacadeServiceMock.getInstallmentsByOrganizationIdAndNav(orgId, request.getLeft(), TOKEN)).thenReturn(List.of(installmentDTO));
    when(cartRequestMapperMock.mapInstallmentToCartRequest(same(installmentDTO), eq(org), any(), eq(request.getRight())))
      .thenReturn(cartRequest);
    when(checkoutServiceMock.checkoutCart(cartRequest)).thenReturn(null);
    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> service.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_SYSTEM_ERROR, exception.getFault());
  }

  @Test
  void givenValidRequestWhenVerifyNoticeServiceThenOk() {
    //given
    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    installmentDTO.setStatus(InstallmentStatus.UNPAID);
    CartRequest cartRequest = podamFactory.manufacturePojo(CartRequest.class);
    String sessionId = String.valueOf(installmentDTO.getInstallmentId());

    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(installmentFacadeServiceMock.getInstallmentsByOrganizationIdAndNav(orgId, request.getLeft(), TOKEN)).thenReturn(List.of(installmentDTO));
    when(cartRequestMapperMock.mapInstallmentToCartRequest(same(installmentDTO), eq(org), any(), eq(request.getRight())))
      .thenReturn(cartRequest);
    when(checkoutServiceMock.checkoutCart(cartRequest)).thenReturn("https://example.com/checkout");
    //when
    PaymentResponse response = service.processRequest(request, orgIpaCode, userInfo, TOKEN);

    //verify
    Assertions.assertNotNull(response);
    Assertions.assertEquals(OutcomeEnum.OK, response.getOutcome());
    Assertions.assertEquals("https://example.com/checkout", response.getTriggerPaymentUrl());
    Assertions.assertEquals(sessionId, response.getPaymentId());
  }
}
