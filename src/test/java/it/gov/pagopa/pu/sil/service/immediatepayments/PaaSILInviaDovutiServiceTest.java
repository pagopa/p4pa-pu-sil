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
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.mapper.PaaSILInviaDovutiMapper;
import it.gov.pagopa.pu.sil.service.debtposition.CreateDebtPositionService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovutiRisposta;
import org.apache.commons.lang3.tuple.Triple;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaaSILInviaDovutiServiceTest {

  @Mock
  private PaaSILInviaDovutiMapper paaSILInviaDovutiMapperMock;
  @Mock
  private CheckoutService checkoutServiceMock;
  @Mock
  private CreateDebtPositionService createDebtPositionServiceMock;
  @Mock
  private CartRequestMapper cartRequestMapperMock;
  @Mock
  private OrganizationService organizationServiceMock;

  @InjectMocks
  private PaaSILInviaDovutiService paaSILInviaDovutiService;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  private UserInfo userInfo = null;
  private String orgIpaCode = null;
  private PaaSILInviaDovuti request = null;
  private static final String TOKEN = "ACCESS_TOKEN";
  private Organization org = null;
  private Long orgId = null;

  @BeforeEach
  void setUp() {
    Mockito.reset(paaSILInviaDovutiMapperMock, checkoutServiceMock, createDebtPositionServiceMock, organizationServiceMock, cartRequestMapperMock);

    userInfo = podamFactory.manufacturePojo(UserInfo.class);
    orgIpaCode = userInfo.getOrganizations().getFirst().getOrganizationIpaCode();
    userInfo.getOrganizations().getFirst().setRoles(List.of("ROLE_X", "ROLE_ADMIN"));
    orgId = userInfo.getOrganizations().getFirst().getOrganizationId();
    org = podamFactory.manufacturePojo(Organization.class);
    org.setOrganizationId(orgId);
    org.setIpaCode(orgIpaCode);
    org.setStatus(OrganizationStatus.ACTIVE);

    request = podamFactory.manufacturePojo(PaaSILInviaDovuti.class);
    request.setEnteSILInviaRispostaPagamentoUrl("https://example.com/callback");
  }

  @Test
  void givenNotAuthorizedUserWhenPaaSILInviaCarrelloDovutiServiceThenError() {
    //given
    userInfo.getOrganizations().getFirst().setOrganizationIpaCode("INVALID_IPA_CODE");

    //when
    SilFaultException response = Assertions.assertThrows(SilFaultException.class, () -> paaSILInviaDovutiService.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_ENTE_NON_VALIDO, response.getFault());
  }

  @ParameterizedTest
  @ValueSource(strings = {"not_active_ipa"})
  @NullSource
  void givenInvalidAuthorizationWhenPaaSILInviaDovutiServiceThenError(String testCase) {
    if(testCase==null){
      org = null;
    } else {
      org.setStatus(OrganizationStatus.DRAFT);
    }

    when(organizationServiceMock.getOrganizationById(anyLong(), anyString())).thenReturn(Optional.ofNullable(org));

    SilFaultException response = Assertions.assertThrows(SilFaultException.class, () -> paaSILInviaDovutiService.processRequest(request, orgIpaCode, userInfo, TOKEN));

    assertEquals(SilFaults.PAA_ENTE_NON_VALIDO, response.getFault());
  }

  @Test
  void givenInvalidUrlWhenPaaSILInviaDovutiThenFault() {
    //given
    request.setEnteSILInviaRispostaPagamentoUrl("http://");
    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));

    //when
    SilFaultException response = Assertions.assertThrows(SilFaultException.class, () -> paaSILInviaDovutiService.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_URL_NON_VALIDA, response.getFault());
  }

  @Test
  void givenMapperFaultWhenPaaSILInviaDovutiThenFault() {
    //given
    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(paaSILInviaDovutiMapperMock.mapRequestToDebtPositions(eq(request), eq(org), any(), eq(TOKEN)))
      .thenThrow(new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "mapper error"));

    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> paaSILInviaDovutiService.processRequest(request, orgIpaCode, userInfo, TOKEN));

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
    when(paaSILInviaDovutiMapperMock.mapRequestToDebtPositions(eq(request), eq(org), any(), eq(TOKEN)))
      .thenReturn(debtPositionDTOList);
    when(createDebtPositionServiceMock.createSyncedDebtPositions(debtPositionDTOList, TOKEN))
      .thenThrow(new SilFaultException(SilFaults.PAA_SYSTEM_ERROR, "system error"));

    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> paaSILInviaDovutiService.processRequest(request, orgIpaCode, userInfo, TOKEN));

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
    when(paaSILInviaDovutiMapperMock.mapRequestToDebtPositions(eq(request), eq(org), argThat(c -> {cartId.set(c); return true;}), eq(TOKEN)))
      .thenReturn(debtPositionDTOList);
    when(createDebtPositionServiceMock.createSyncedDebtPositions(debtPositionDTOList, TOKEN))
      .thenReturn(debtPositionDTOList);
    when(cartRequestMapperMock.mapDebtPositionsToCartRequest(eq(debtPositionDTOList), eq(org), argThat(c -> c.equals(cartId.get())), eq(request.getEnteSILInviaRispostaPagamentoUrl())))
      .thenThrow(new SilFaultException(SilFaults.PAA_URL_NON_VALIDA, "invalid url"));

    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> paaSILInviaDovutiService.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_URL_NON_VALIDA, exception.getFault());
    Assertions.assertEquals("invalid url", exception.getDescription());
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

    String sessionId = debtPositionDTOList.stream()
      .flatMap(dp -> dp.getPaymentOptions().stream())
      .flatMap(option -> option.getInstallments().stream())
      .map(InstallmentDTO::getInstallmentId)
      .map(String::valueOf)
      .collect(Collectors.joining(Constants.SESSION_ID_SEPARATOR));

    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(paaSILInviaDovutiMapperMock.mapRequestToDebtPositions(eq(request), eq(org), argThat(c -> {cartId.set(c); return true;}), eq(TOKEN)))
      .thenReturn(debtPositionDTOList);
    when(createDebtPositionServiceMock.createSyncedDebtPositions(debtPositionDTOList, TOKEN))
      .thenReturn(debtPositionDTOList);
    when(cartRequestMapperMock.mapDebtPositionsToCartRequest(eq(debtPositionDTOList), eq(org), argThat(c -> c.equals(cartId.get())), eq(request.getEnteSILInviaRispostaPagamentoUrl())))
      .thenReturn(cartRequest);
    when(checkoutServiceMock.checkoutCart(cartRequest)).thenReturn("https://example.com/checkout");

    //when
    Triple<PaaSILInviaDovutiRisposta, String, RegistryOutcome> response = paaSILInviaDovutiService.processRequest(request, orgIpaCode, userInfo, TOKEN);

    //verify
    Assertions.assertNotNull(response);
    Assertions.assertEquals(RegistryOutcome.OK, response.getRight());
    Assertions.assertNotNull(response.getLeft());
    Assertions.assertNull(response.getLeft().getFault());
    Assertions.assertEquals(RegistryOutcome.OK.getValue(), response.getLeft().getEsito());
    Assertions.assertEquals("https://example.com/checkout", response.getLeft().getUrl());
    Assertions.assertEquals(1, response.getLeft().getRedirect());
    Assertions.assertEquals(sessionId, response.getLeft().getIdSession());
    Assertions.assertEquals(iuvs, response.getMiddle());
  }
}
