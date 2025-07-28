package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.dto.generated.InstantPaymentRequest;
import it.gov.pagopa.pu.sil.dto.generated.PaymentResponse;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.mapper.InstantPaymentMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.debtposition.ManageDebtPositionService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovutiRisposta;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstantPaymentServiceTest {

  @Mock
  private CheckoutService checkoutServiceMock;
  @Mock
  private ManageDebtPositionService manageDebtPositionServiceMock;
  @Mock
  private CartRequestMapper cartRequestMapperMock;
  @Mock
  private OrganizationService organizationServiceMock;
  @Mock
  private SessionIdMapper sessionIdMapperMock;
  @Mock
  private InstantPaymentMapper instantPaymentMapperMock;

  @InjectMocks
  private InstantPaymentService service;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  private UserInfo userInfo = null;
  private String orgIpaCode = null;
  private InstantPaymentRequest request = null;
  private static final String TOKEN = "ACCESS_TOKEN";
  private Organization org = null;
  private Long orgId = null;


  @BeforeEach
  void setUp() {
    Mockito.reset(instantPaymentMapperMock, checkoutServiceMock, manageDebtPositionServiceMock, organizationServiceMock, cartRequestMapperMock, sessionIdMapperMock);

    userInfo = podamFactory.manufacturePojo(UserInfo.class);
    orgIpaCode = userInfo.getOrganizations().getFirst().getOrganizationIpaCode();
    userInfo.getOrganizations().getFirst().setRoles(List.of("ROLE_X", "ROLE_ADMIN"));
    orgId = userInfo.getOrganizations().getFirst().getOrganizationId();
    org = podamFactory.manufacturePojo(Organization.class);
    org.setOrganizationId(orgId);
    org.setIpaCode(orgIpaCode);
    org.setStatus(OrganizationStatus.ACTIVE);

    request = podamFactory.manufacturePojo(InstantPaymentRequest.class);
    request.setCallbackUrl("http://callback.url");
  }


  @Test
  void givenNotAuthorizedUserWhenPaaSILInviaDovutiThenError() {
    //given
    userInfo.getOrganizations().getFirst().setOrganizationIpaCode("INVALID_IPA_CODE");

    //when
    SilFaultException response = Assertions.assertThrows(SilFaultException.class, () -> service.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_ENTE_NON_VALIDO, response.getFault());
  }

  @ParameterizedTest
  @ValueSource(strings = {"not_active_ipa"})
  @NullSource
  void givenInvalidOrganizationWhenPaaSILInviaDovutiThenError(String testCase) {
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
  void givenInvalidUrlWhenPaaSILInviaDovutiThenFault() {
    //given
    request.setCallbackUrl("http://");
    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));

    //when
    SilFaultException response = Assertions.assertThrows(SilFaultException.class, () -> service.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_URL_NON_VALIDA, response.getFault());
  }

  @Test
  void givenMapperFaultWhenPaaSILInviaDovutiThenFault() {
    //given
    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(instantPaymentMapperMock.mapRequestToDebtPositions(eq(request), eq(org), any(), eq(TOKEN)))
      .thenThrow(new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "mapper error"));

    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> service.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_ENTE_NON_VALIDO, exception.getFault());
    Assertions.assertEquals("mapper error", exception.getDescription());
  }

  @Test
  void givenCreateDebtPositionFaultWhenPaaSILInviaDovutiThenFault() {
    //given
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    List<DebtPositionDTO> debtPositionDTOList = List.of(debtPositionDTO);

    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(instantPaymentMapperMock.mapRequestToDebtPositions(eq(request), eq(org), any(), eq(TOKEN)))
      .thenReturn(debtPositionDTOList);
    when(manageDebtPositionServiceMock.createSyncedDebtPositions(debtPositionDTOList, TOKEN))
      .thenThrow(new SilFaultException(SilFaults.PAA_SYSTEM_ERROR, "system error"));

    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> service.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_SYSTEM_ERROR, exception.getFault());
    Assertions.assertEquals("system error", exception.getDescription());
  }

  @Test
  void givenMapCartRequestFaultWhenPaaSILInviaDovutiThenFault() {
    //given
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    List<DebtPositionDTO> debtPositionDTOList = List.of(debtPositionDTO);

    AtomicReference<String> cartId = new AtomicReference<>();

    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(instantPaymentMapperMock.mapRequestToDebtPositions(eq(request), eq(org), argThat(c -> {cartId.set(c); return true;}), eq(TOKEN)))
      .thenReturn(debtPositionDTOList);
    when(manageDebtPositionServiceMock.createSyncedDebtPositions(debtPositionDTOList, TOKEN))
      .thenReturn(debtPositionDTOList);
    when(cartRequestMapperMock.mapDebtPositionsToCartRequest(eq(debtPositionDTOList), eq(org), argThat(c -> c.equals(cartId.get())), eq(request.getCallbackUrl())))
      .thenThrow(new SilFaultException(SilFaults.PAA_URL_NON_VALIDA, "invalid url"));

    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> service.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_URL_NON_VALIDA, exception.getFault());
    Assertions.assertEquals("invalid url", exception.getDescription());
  }

  @Test
  void givenInvalidCheckoutUrlWhenPaaSILInviaDovutiThenException() {
    //given
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    List<DebtPositionDTO> debtPositionDTOList = List.of(debtPositionDTO);
    AtomicReference<String> cartId = new AtomicReference<>();
    CartRequest cartRequest = podamFactory.manufacturePojo(CartRequest.class);

    String sessionId = "SESSION_ID";

    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(instantPaymentMapperMock.mapRequestToDebtPositions(eq(request), eq(org), argThat(c -> {cartId.set(c); return true;}), eq(TOKEN)))
      .thenReturn(debtPositionDTOList);
    when(manageDebtPositionServiceMock.createSyncedDebtPositions(debtPositionDTOList, TOKEN))
      .thenReturn(debtPositionDTOList);
    when(sessionIdMapperMock.mapDebtPositionsToSessionId(debtPositionDTOList)).thenReturn(sessionId);
    when(cartRequestMapperMock.mapDebtPositionsToCartRequest(eq(debtPositionDTOList), eq(org), argThat(c -> c.equals(cartId.get())), eq(request.getCallbackUrl())))
      .thenReturn(cartRequest);
    when(checkoutServiceMock.checkoutCart(cartRequest)).thenReturn(null);

    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> service.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_SYSTEM_ERROR, exception.getFault());
  }

  @Test
  void givenValidRequestWhenPaaSILInviaDovutiThenOk() {
    //given
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    List<DebtPositionDTO> debtPositionDTOList = List.of(debtPositionDTO);
    AtomicReference<String> cartId = new AtomicReference<>();
    CartRequest cartRequest = podamFactory.manufacturePojo(CartRequest.class);

    String iuvs = debtPositionDTOList.stream()
      .flatMap(dp -> dp.getPaymentOptions().stream())
      .flatMap(option -> option.getInstallments().stream())
      .map(InstallmentDTO::getIuv)
      .collect(Collectors.joining(Utilities.IUV_SEPARATOR));

    String sessionId = "SESSION_ID";

    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(instantPaymentMapperMock.mapRequestToDebtPositions(eq(request), eq(org), argThat(c -> {cartId.set(c); return true;}), eq(TOKEN)))
      .thenReturn(debtPositionDTOList);
    when(manageDebtPositionServiceMock.createSyncedDebtPositions(debtPositionDTOList, TOKEN))
      .thenReturn(debtPositionDTOList);
    when(sessionIdMapperMock.mapDebtPositionsToSessionId(debtPositionDTOList)).thenReturn(sessionId);
    when(cartRequestMapperMock.mapDebtPositionsToCartRequest(eq(debtPositionDTOList), eq(org), argThat(c -> c.equals(cartId.get())), eq(request.getCallbackUrl())))
      .thenReturn(cartRequest);
    when(checkoutServiceMock.checkoutCart(cartRequest)).thenReturn("https://example.com/checkout");

    //when
    Triple<PaymentResponse, String, RegistryOutcome> response = service.processRequest(request, orgIpaCode, userInfo, TOKEN);

    //verify
    Assertions.assertNotNull(response);
    Assertions.assertEquals(RegistryOutcome.OK, response.getRight());
    Assertions.assertNotNull(response.getLeft());
    Assertions.assertEquals(RegistryOutcome.OK.getValue(), response.getLeft().getOutcome().getValue());
    Assertions.assertEquals("https://example.com/checkout", response.getLeft().getTriggerPaymentUrl());
    Assertions.assertEquals(sessionId, response.getLeft().getPaymentId());
    Assertions.assertEquals(iuvs, response.getMiddle());
  }

}
