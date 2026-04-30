package it.gov.pagopa.pu.sil.service.debtposition;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.LimitedTokenRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserInfoLimitedScope;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.TransferDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static it.gov.pagopa.pu.sil.service.debtposition.DebtPositionCheckoutService.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtPositionCheckoutServiceTest {

  private final String applicationBaseUrl = "baseUrl";
  @Mock
  private OrganizationService organizationServiceMock;
  @Mock
  private DebtPositionService debtPositionServiceMock;
  @Mock
  private CheckoutService checkoutServiceMock;
  @Mock
  private CartRequestMapper cartRequestMapperMock;
  @Mock
  private AuthorizationService authorizationServiceMock;

  @InjectMocks
  private DebtPositionCheckoutService debtPositionCheckoutService;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();
  private final String accessToken = "header.payload.signature";
  private final String orgFiscalCode = "ORG123456789";
  private final String orgIpaCode = "userIpaCode";
  private final String iuvs = "IUV1,IUV2,IUV3";
  private final String callbackUrl = "https://www.google.com";
  private final UserInfoLimitedScope loggedUser = AuthorizationServiceTest.buildUserLimitedScope(
      1L, orgFiscalCode, orgIpaCode, CHECKOUT_RESOURCE, iuvs);

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(debtPositionCheckoutService, "applicationBaseUrl", applicationBaseUrl);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
        organizationServiceMock,
        debtPositionServiceMock,
        checkoutServiceMock,
        cartRequestMapperMock);
  }

  @Test
  void givenValidAuthorizationWhenRedirectToCheckoutThenOk() {
    Organization organization = podamFactory.manufacturePojo(Organization.class);
    organization.setOrganizationId(1L);
    List<DebtPositionDTO> debtPositionDTOList = IntStream.range(1, 4).mapToObj(i -> {
      DebtPositionDTO dto = podamFactory.manufacturePojo(DebtPositionDTO.class);
      dto.setPaymentOptions(List.of(dto.getPaymentOptions().getFirst()));
      dto.getPaymentOptions().getFirst()
          .setInstallments(List.of(dto.getPaymentOptions().getFirst().getInstallments().getFirst()));
      dto.getPaymentOptions().getFirst().getInstallments().getFirst().setIuv("IUV" + i);
      dto.getPaymentOptions().getFirst().getInstallments().getFirst().setTransfers(List.of(new TransferDTO()));
      return dto;
    }).toList();
    CartRequest cartRequest = podamFactory.manufacturePojo(CartRequest.class);

    when(organizationServiceMock.getOrganizationById(anyLong(), eq(accessToken))).thenReturn(
        Optional.of(organization));
    when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIuv(eq(organization.getOrganizationId()),
        anyString(),
        eq(Constants.ORDINARY_DEBT_POSITION_ORIGINS), eq(accessToken)))
        .thenReturn(List.of(debtPositionDTOList.getFirst()))
        .thenReturn(List.of(debtPositionDTOList.get(1)))
        .thenReturn(List.of(debtPositionDTOList.get(2)));
    when(cartRequestMapperMock.mapDebtPositionsToCartRequest(any(), any(), anyString(), any()))
        .thenReturn(cartRequest);
    when(checkoutServiceMock.checkoutCart(cartRequest)).thenReturn("http://www.test.com");

    assertDoesNotThrow(() -> debtPositionCheckoutService.redirectToCheckout(loggedUser, accessToken));

    verify(organizationServiceMock).getOrganizationById(anyLong(), eq(accessToken));
    verify(debtPositionServiceMock, times(3)).getDebtPositionsByOrganizationIdAndIuv(
        eq(organization.getOrganizationId()), anyString(), eq(Constants.ORDINARY_DEBT_POSITION_ORIGINS),
        eq(accessToken));
    verify(cartRequestMapperMock).mapDebtPositionsToCartRequest(eq(debtPositionDTOList), eq(organization), anyString(),
        eq(callbackUrl));
    verify(checkoutServiceMock).checkoutCart(cartRequest);
  }

  @Test
  void givenNullSessionDataWhenRedirectToCheckoutThenOk() {
    Organization organization = podamFactory.manufacturePojo(Organization.class);
    organization.setOrganizationId(1L);
    List<DebtPositionDTO> debtPositionDTOList = IntStream.range(1, 4).mapToObj(i -> {
      DebtPositionDTO dto = podamFactory.manufacturePojo(DebtPositionDTO.class);
      dto.setPaymentOptions(List.of(dto.getPaymentOptions().getFirst()));
      dto.getPaymentOptions().getFirst()
          .setInstallments(List.of(dto.getPaymentOptions().getFirst().getInstallments().getFirst()));
      dto.getPaymentOptions().getFirst().getInstallments().getFirst().setIuv("IUV" + i);
      dto.getPaymentOptions().getFirst().getInstallments().getFirst().setTransfers(List.of(new TransferDTO()));
      return dto;
    }).toList();
    CartRequest cartRequest = podamFactory.manufacturePojo(CartRequest.class);
    UserInfoLimitedScope loggedUserNoSessionData = AuthorizationServiceTest.buildUserLimitedScope(
        1L, orgFiscalCode, orgIpaCode, CHECKOUT_RESOURCE, iuvs);
    loggedUserNoSessionData.getResource().setSessionData(null);

    when(organizationServiceMock.getOrganizationById(anyLong(), eq(accessToken))).thenReturn(
        Optional.of(organization));
    when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIuv(eq(organization.getOrganizationId()),
        anyString(),
        eq(Constants.ORDINARY_DEBT_POSITION_ORIGINS), eq(accessToken)))
        .thenReturn(List.of(debtPositionDTOList.getFirst()))
        .thenReturn(List.of(debtPositionDTOList.get(1)))
        .thenReturn(List.of(debtPositionDTOList.get(2)));
    when(cartRequestMapperMock.mapDebtPositionsToCartRequest(any(), any(), anyString(), any()))
        .thenReturn(cartRequest);
    when(checkoutServiceMock.checkoutCart(cartRequest)).thenReturn("http://www.test.com");

    assertDoesNotThrow(() -> debtPositionCheckoutService.redirectToCheckout(loggedUserNoSessionData, accessToken));

    verify(organizationServiceMock).getOrganizationById(anyLong(), eq(accessToken));
    verify(debtPositionServiceMock, times(3)).getDebtPositionsByOrganizationIdAndIuv(
        eq(organization.getOrganizationId()), anyString(), eq(Constants.ORDINARY_DEBT_POSITION_ORIGINS),
        eq(accessToken));
    verify(cartRequestMapperMock).mapDebtPositionsToCartRequest(eq(debtPositionDTOList), eq(organization), anyString(),
        eq(null));
    verify(checkoutServiceMock).checkoutCart(cartRequest);
  }

  @Test
  void givenUserNotLimitedScopeWhenRedirectToCheckoutThenKo() {
    UserInfo notLimitedUser = AuthorizationServiceTest.buildAdminUser(1L, orgFiscalCode, orgIpaCode);

    assertThrows(AuthorizationDeniedException.class,
        () -> debtPositionCheckoutService.redirectToCheckout(notLimitedUser, accessToken));

    verify(organizationServiceMock, never()).getOrganizationById(anyLong(), anyString());
    verify(debtPositionServiceMock, never()).getDebtPositionsByOrganizationIdAndIuv(anyLong(), anyString(), any(),
        any());
    verify(cartRequestMapperMock, never()).mapDebtPositionsToCartRequest(any(), any(), anyString(), any());
    verify(checkoutServiceMock, never()).checkoutCart(any());
  }

  @Test
  void givenInvalidLimitedScopeResourceWhenRedirectToCheckoutThenKo() {
    UserInfoLimitedScope limitedScopeUser = AuthorizationServiceTest.buildUserLimitedScope(1L, orgFiscalCode,
        orgIpaCode, "INVALID-RESOURCE", iuvs);

    assertThrows(AuthorizationDeniedException.class,
        () -> debtPositionCheckoutService.redirectToCheckout(limitedScopeUser, accessToken));

    verify(organizationServiceMock, never()).getOrganizationById(anyLong(), anyString());
    verify(debtPositionServiceMock, never()).getDebtPositionsByOrganizationIdAndIuv(anyLong(), anyString(), any(),
        any());
    verify(cartRequestMapperMock, never()).mapDebtPositionsToCartRequest(any(), any(), anyString(), any());
    verify(checkoutServiceMock, never()).checkoutCart(any());
  }

  @Test
  void givenInvalidScopeWhenRedirectToCheckoutThenKo() {
    UserInfoLimitedScope limitedScopeUser = AuthorizationServiceTest.buildUserLimitedScope(1L, orgFiscalCode,
        orgIpaCode, "INVALID-RESOURCE", iuvs);
    limitedScopeUser.getResource().setApp("invalid-scope");

    assertThrows(AuthorizationDeniedException.class,
        () -> debtPositionCheckoutService.redirectToCheckout(limitedScopeUser, accessToken));

    verify(organizationServiceMock, never()).getOrganizationById(anyLong(), anyString());
    verify(debtPositionServiceMock, never()).getDebtPositionsByOrganizationIdAndIuv(anyLong(), anyString(), any(),
        any());
    verify(cartRequestMapperMock, never()).mapDebtPositionsToCartRequest(any(), any(), anyString(), any());
    verify(checkoutServiceMock, never()).checkoutCart(any());
  }

  @Test
  void givenNotFoundOrganizationWhenRedirectToCheckoutThenKo() {
    when(organizationServiceMock.getOrganizationById(anyLong(), eq(accessToken))).thenReturn(
        Optional.empty());

    assertThrows(SilFaultException.class,
        () -> debtPositionCheckoutService.redirectToCheckout(loggedUser, accessToken));

    verify(organizationServiceMock).getOrganizationById(anyLong(), eq(accessToken));
    verify(debtPositionServiceMock, never()).getDebtPositionsByOrganizationIdAndIuv(anyLong(), anyString(), any(),
        any());
    verify(cartRequestMapperMock, never()).mapDebtPositionsToCartRequest(any(), any(), anyString(), any());
    verify(checkoutServiceMock, never()).checkoutCart(any());
  }

  @Test
  void givenCheckoutResponseIsBlankWhenRedirectToCheckoutThenKo() {
    Organization organization = podamFactory.manufacturePojo(Organization.class);
    organization.setOrganizationId(1L);
    List<DebtPositionDTO> debtPositionDTOList = IntStream.range(1, 4).mapToObj(i -> {
      DebtPositionDTO dto = podamFactory.manufacturePojo(DebtPositionDTO.class);
      dto.setPaymentOptions(List.of(dto.getPaymentOptions().getFirst()));
      dto.getPaymentOptions().getFirst()
          .setInstallments(List.of(dto.getPaymentOptions().getFirst().getInstallments().getFirst()));
      dto.getPaymentOptions().getFirst().getInstallments().getFirst().setIuv("IUV" + i);
      dto.getPaymentOptions().getFirst().getInstallments().getFirst().setTransfers(List.of(new TransferDTO()));
      return dto;
    }).toList();
    CartRequest cartRequest = podamFactory.manufacturePojo(CartRequest.class);

    when(organizationServiceMock.getOrganizationById(anyLong(), eq(accessToken))).thenReturn(
        Optional.of(organization));
    when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIuv(eq(organization.getOrganizationId()),
        anyString(),
        eq(Constants.ORDINARY_DEBT_POSITION_ORIGINS), eq(accessToken)))
        .thenReturn(List.of(debtPositionDTOList.getFirst()))
        .thenReturn(List.of(debtPositionDTOList.get(1)))
        .thenReturn(List.of(debtPositionDTOList.get(2)));
    when(cartRequestMapperMock.mapDebtPositionsToCartRequest(any(), any(), anyString(), any()))
        .thenReturn(cartRequest);
    when(checkoutServiceMock.checkoutCart(cartRequest)).thenReturn("");

    assertThrows(SilFaultException.class,
        () -> debtPositionCheckoutService.redirectToCheckout(loggedUser, accessToken));

    verify(organizationServiceMock).getOrganizationById(anyLong(), eq(accessToken));
    verify(debtPositionServiceMock, times(3)).getDebtPositionsByOrganizationIdAndIuv(
        eq(organization.getOrganizationId()), anyString(), eq(Constants.ORDINARY_DEBT_POSITION_ORIGINS),
        eq(accessToken));
    verify(cartRequestMapperMock).mapDebtPositionsToCartRequest(eq(debtPositionDTOList), eq(organization), anyString(),
        eq(callbackUrl));
    verify(checkoutServiceMock).checkoutCart(cartRequest);
  }

  @Test
  void whenComposeDebtPositionsCheckoutUrlThenOk() {
    LimitedTokenRequest expectedTokenRequest = LimitedTokenRequest.builder()
        .app(PU_SIL_SCOPE)
        .organizationId(1L)
        .resource(CHECKOUT_RESOURCE)
        .resourceId(iuvs)
        .sessionData(Map.of(CALLBACK_URL_SESSION_DATA_KEY, callbackUrl))
        .expireInSeconds(86400L)
        .singleUsage(false)
        .build();
    String expectedResult = applicationBaseUrl
        + "/organization/%s/checkout?token=%s".formatted(orgFiscalCode, accessToken);

    when(authorizationServiceMock.requestLimitedToken(expectedTokenRequest, accessToken))
        .thenReturn(new AccessToken().accessToken(accessToken));

    String result = debtPositionCheckoutService.composeDebtPositionsCheckoutUrl(1L, iuvs, callbackUrl, orgFiscalCode,
        accessToken);

    assertNotNull(result);
    assertEquals(expectedResult, result);
  }

  @Test
  void givenNullCallbackUrlWhenComposeDebtPositionsCheckoutUrlThenOk() {
    LimitedTokenRequest expectedTokenRequest = LimitedTokenRequest.builder()
        .app(PU_SIL_SCOPE)
        .organizationId(1L)
        .resource(CHECKOUT_RESOURCE)
        .resourceId(iuvs)
        .sessionData(null)
        .expireInSeconds(86400L)
        .singleUsage(false)
        .build();
    String expectedResult = applicationBaseUrl
        + "/organization/%s/checkout?token=%s".formatted(orgFiscalCode, accessToken);

    when(authorizationServiceMock.requestLimitedToken(expectedTokenRequest, accessToken))
        .thenReturn(new AccessToken().accessToken(accessToken));

    String result = debtPositionCheckoutService.composeDebtPositionsCheckoutUrl(1L, iuvs, null, orgFiscalCode,
        accessToken);

    assertNotNull(result);
    assertEquals(expectedResult, result);
  }

}
