package it.gov.pagopa.pu.sil.service.debtposition;

import static it.gov.pagopa.pu.sil.service.debtposition.DebtPositionCheckoutService.CHECKOUT_RESOURCE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserInfoLimitedScope;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionOrigin;
import it.gov.pagopa.pu.debtpositions.dto.generated.TransferDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.util.TestUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import uk.co.jemos.podam.api.PodamFactory;

@ExtendWith(MockitoExtension.class)
class DebtPositionCheckoutServiceTest {

  @Mock
  private OrganizationService organizationServiceMock;
  @Mock
  private DebtPositionService debtPositionServiceMock;
  @Mock
  private CheckoutService checkoutServiceMock;
  @Mock
  private CartRequestMapper cartRequestMapperMock;

  @InjectMocks
  private DebtPositionCheckoutService debtPositionCheckoutService;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();
  private final String accessToken = "header.payload.signature";
  private final String orgFiscalCode = "ORG123456789";
  private final String orgIpaCode = "userIpaCode";
  private final UserInfoLimitedScope loggedUser = AuthorizationServiceTest.buildUserLimitedScope(
    1L, orgFiscalCode, orgIpaCode, CHECKOUT_RESOURCE, "IUV1,IUV2,IUV3");

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      organizationServiceMock,
      debtPositionServiceMock,
      checkoutServiceMock,
      cartRequestMapperMock
    );
  }

  @Test
  void givenValidAuthorizationWhenRedirectToCheckoutThenOk() {

    Organization organization = podamFactory.manufacturePojo(Organization.class);
    organization.setOrganizationId(1L);
    List<DebtPositionDTO> debtPositionDTOList = IntStream.range(1, 4).mapToObj(i -> {
      DebtPositionDTO dto = podamFactory.manufacturePojo(DebtPositionDTO.class);
      dto.setPaymentOptions(List.of(dto.getPaymentOptions().getFirst()));
      dto.getPaymentOptions().getFirst().setInstallments(List.of(dto.getPaymentOptions().getFirst().getInstallments().getFirst()));
      dto.getPaymentOptions().getFirst().getInstallments().getFirst().setIuv("IUV"+i);
      dto.getPaymentOptions().getFirst().getInstallments().getFirst().setTransfers(List.of(new TransferDTO()));
      return dto;
    }).toList();
    CartRequest cartRequest = podamFactory.manufacturePojo(CartRequest.class);

    when(organizationServiceMock.getOrganizationById(anyLong(), eq(accessToken))).thenReturn(
      Optional.of(organization));
    when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIuv(eq(organization.getOrganizationId()), anyString(),
      eq(List.of(DebtPositionOrigin.SPONTANEOUS_SIL)), eq(accessToken)))
      .thenReturn(List.of(debtPositionDTOList.getFirst()))
      .thenReturn(List.of(debtPositionDTOList.get(1)))
      .thenReturn(List.of(debtPositionDTOList.get(2)));
    when(cartRequestMapperMock.mapDebtPositionsToCartRequest(any(), any(), anyString(), any()))
      .thenReturn(cartRequest);
    when(checkoutServiceMock.checkoutCart(cartRequest)).thenReturn("http://www.test.com");

    assertDoesNotThrow(() -> debtPositionCheckoutService.redirectToCheckout(loggedUser, accessToken));

    verify(organizationServiceMock).getOrganizationById(anyLong(), eq(accessToken));
    verify(debtPositionServiceMock, times(3)).getDebtPositionsByOrganizationIdAndIuv(eq(organization.getOrganizationId()), anyString(), eq(List.of(DebtPositionOrigin.SPONTANEOUS_SIL)), eq(accessToken));
    verify(cartRequestMapperMock).mapDebtPositionsToCartRequest(eq(debtPositionDTOList), eq(organization), anyString(), eq(null));
    verify(checkoutServiceMock).checkoutCart(cartRequest);
  }

  @Test
  void givenUserNotLimitedScopeWhenRedirectToCheckoutThenKo() {
    UserInfo notLimitedUser = AuthorizationServiceTest.buildAdminUser(1L, orgFiscalCode, orgIpaCode);

    assertThrows(AuthorizationDeniedException.class, () -> debtPositionCheckoutService.redirectToCheckout(notLimitedUser, accessToken));

    verify(organizationServiceMock, never()).getOrganizationById(anyLong(), anyString());
    verify(debtPositionServiceMock, never()).getDebtPositionsByOrganizationIdAndIuv(anyLong(), anyString(), any(), any());
    verify(cartRequestMapperMock, never()).mapDebtPositionsToCartRequest(any(), any(), anyString(), any());
    verify(checkoutServiceMock, never()).checkoutCart(any());
  }

  @Test
  void givenInvalidLimitedScopeResourceWhenRedirectToCheckoutThenKo() {
    UserInfoLimitedScope limitedScopeUser = AuthorizationServiceTest.buildUserLimitedScope(1L, orgFiscalCode, orgIpaCode, "INVALID-RESOURCE", "IUV1,IUV2,IUV3");

    assertThrows(AuthorizationDeniedException.class, () -> debtPositionCheckoutService.redirectToCheckout(limitedScopeUser, accessToken));

    verify(organizationServiceMock, never()).getOrganizationById(anyLong(), anyString());
    verify(debtPositionServiceMock, never()).getDebtPositionsByOrganizationIdAndIuv(anyLong(), anyString(), any(), any());
    verify(cartRequestMapperMock, never()).mapDebtPositionsToCartRequest(any(), any(), anyString(), any());
    verify(checkoutServiceMock, never()).checkoutCart(any());
  }

  @Test
  void givenInvalidScopeWhenRedirectToCheckoutThenKo() {
    UserInfoLimitedScope limitedScopeUser = AuthorizationServiceTest.buildUserLimitedScope(1L, orgFiscalCode, orgIpaCode, "INVALID-RESOURCE", "IUV1,IUV2,IUV3");
    limitedScopeUser.getResource().setApp("invalid-scope");

    assertThrows(AuthorizationDeniedException.class, () -> debtPositionCheckoutService.redirectToCheckout(limitedScopeUser, accessToken));

    verify(organizationServiceMock, never()).getOrganizationById(anyLong(), anyString());
    verify(debtPositionServiceMock, never()).getDebtPositionsByOrganizationIdAndIuv(anyLong(), anyString(), any(), any());
    verify(cartRequestMapperMock, never()).mapDebtPositionsToCartRequest(any(), any(), anyString(), any());
    verify(checkoutServiceMock, never()).checkoutCart(any());
  }

  @Test
  void givenNotFoundOrganizationWhenRedirectToCheckoutThenKo() {
    when(organizationServiceMock.getOrganizationById(anyLong(), eq(accessToken))).thenReturn(
      Optional.empty());

    assertThrows(SilFaultException.class, () -> debtPositionCheckoutService.redirectToCheckout(loggedUser, accessToken));

    verify(organizationServiceMock).getOrganizationById(anyLong(), eq(accessToken));
    verify(debtPositionServiceMock, never()).getDebtPositionsByOrganizationIdAndIuv(anyLong(), anyString(), any(), any());
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
      dto.getPaymentOptions().getFirst().setInstallments(List.of(dto.getPaymentOptions().getFirst().getInstallments().getFirst()));
      dto.getPaymentOptions().getFirst().getInstallments().getFirst().setIuv("IUV"+i);
      dto.getPaymentOptions().getFirst().getInstallments().getFirst().setTransfers(List.of(new TransferDTO()));
      return dto;
    }).toList();
    CartRequest cartRequest = podamFactory.manufacturePojo(CartRequest.class);

    when(organizationServiceMock.getOrganizationById(anyLong(), eq(accessToken))).thenReturn(
      Optional.of(organization));
    when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIuv(eq(organization.getOrganizationId()), anyString(),
      eq(List.of(DebtPositionOrigin.SPONTANEOUS_SIL)), eq(accessToken)))
      .thenReturn(List.of(debtPositionDTOList.getFirst()))
      .thenReturn(List.of(debtPositionDTOList.get(1)))
      .thenReturn(List.of(debtPositionDTOList.get(2)));
    when(cartRequestMapperMock.mapDebtPositionsToCartRequest(any(), any(), anyString(), any()))
      .thenReturn(cartRequest);
    when(checkoutServiceMock.checkoutCart(cartRequest)).thenReturn("");

    assertThrows(SilFaultException.class, () -> debtPositionCheckoutService.redirectToCheckout(loggedUser, accessToken));

    verify(organizationServiceMock).getOrganizationById(anyLong(), eq(accessToken));
    verify(debtPositionServiceMock, times(3)).getDebtPositionsByOrganizationIdAndIuv(eq(organization.getOrganizationId()), anyString(), eq(List.of(DebtPositionOrigin.SPONTANEOUS_SIL)), eq(accessToken));
    verify(cartRequestMapperMock).mapDebtPositionsToCartRequest(eq(debtPositionDTOList), eq(organization), anyString(), eq(null));
    verify(checkoutServiceMock).checkoutCart(cartRequest);
  }

}
