package it.gov.pagopa.pu.sil.service.immediatepayments;


import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.mapper.PaaSILInviaCarrelloDovutiMapper;
import it.gov.pagopa.pu.sil.service.debtposition.CreateDebtPositionService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovutiRisposta;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaaSILInviaCarrelloDovutiServiceTest {

  @Mock
  private PaaSILInviaCarrelloDovutiMapper paaSILInviaCarrelloDovutiMapperMock;
  @Mock
  private CheckoutService checkoutServiceMock;
  @Mock
  private CreateDebtPositionService createDebtPositionServiceMock;
  @Mock
  private CartRequestMapper cartRequestMapperMock;

  @InjectMocks
  private PaaSILInviaCarrelloDovutiService paaSILInviaCarrelloDovutiService;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  private UserInfo userInfo = null;
  private String orgIpaCode = null;
  private PaaSILInviaCarrelloDovuti request = null;
  private static final String TOKEN = "ACCESS_TOKEN";

  @BeforeEach
  void setUp() {
    Mockito.reset(paaSILInviaCarrelloDovutiMapperMock, checkoutServiceMock, createDebtPositionServiceMock, cartRequestMapperMock);

    userInfo = podamFactory.manufacturePojo(UserInfo.class);
    orgIpaCode = userInfo.getOrganizations().getFirst().getOrganizationIpaCode();
    userInfo.getOrganizations().getFirst().setRoles(List.of("ROLE_X", "ROLE_ADMIN"));

    request = podamFactory.manufacturePojo(PaaSILInviaCarrelloDovuti.class);
    request.setEnteSILInviaRispostaPagamentoUrl("https://example.com/callback");
  }

  @Test
  void givenNotAuthorizedUserWhenPaaSILInviaCarrelloDovutiServiceThenOk() {
    //given
    userInfo.getOrganizations().getFirst().setOrganizationIpaCode("INVALID_IPA_CODE");

    //when
    SilFaultException response = Assertions.assertThrows(SilFaultException.class, () -> paaSILInviaCarrelloDovutiService.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_ENTE_NON_VALIDO, response.getFault());
  }

  @Test
  void givenInvalidUrlWhenPaaSILInviaDovutiThenFault() {
    //given
    request.setEnteSILInviaRispostaPagamentoUrl("http://");

    //when
    SilFaultException response = Assertions.assertThrows(SilFaultException.class, () -> paaSILInviaCarrelloDovutiService.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_URL_NON_VALIDA, response.getFault());
  }

  @Test
  void givenMapperFaultWhenPaaSILInviaDovutiThenFault() {
    //given
    when(paaSILInviaCarrelloDovutiMapperMock.mapRequestToDebtPositions(eq(request), any(), eq(userInfo), eq(orgIpaCode), eq(TOKEN)))
      .thenThrow(new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "mapper error"));

    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> paaSILInviaCarrelloDovutiService.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(SilFaults.PAA_ENTE_NON_VALIDO, exception.getFault());
    Assertions.assertEquals("mapper error", exception.getDescription());
  }

  @Test
  void givenCreateDebtPositionFaultWhenPaaSILInviaDovutiThenFault() {
    //given
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    List<DebtPositionDTO> debtPositionDTOList = List.of(debtPositionDTO);

    when(paaSILInviaCarrelloDovutiMapperMock.mapRequestToDebtPositions(eq(request), any(), eq(userInfo), eq(orgIpaCode), eq(TOKEN)))
      .thenReturn(debtPositionDTOList);
    when(createDebtPositionServiceMock.createSyncedDebtPositions(debtPositionDTOList, TOKEN))
      .thenThrow(new SilFaultException(SilFaults.PAA_SYSTEM_ERROR, "system error"));

    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> paaSILInviaCarrelloDovutiService.processRequest(request, orgIpaCode, userInfo, TOKEN));

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

    when(paaSILInviaCarrelloDovutiMapperMock.mapRequestToDebtPositions(eq(request), argThat(c -> {cartId.set(c); return true;}), eq(userInfo), eq(orgIpaCode), eq(TOKEN)))
      .thenReturn(debtPositionDTOList);
    when(createDebtPositionServiceMock.createSyncedDebtPositions(debtPositionDTOList, TOKEN)).thenReturn(debtPositionDTOList);
    when(cartRequestMapperMock.mapDebtPositionsToCartRequest(eq(debtPositionDTOList), argThat(c -> c.equals(cartId.get())), eq(request.getEnteSILInviaRispostaPagamentoUrl())))
      .thenThrow(new SilFaultException(SilFaults.PAA_URL_NON_VALIDA, "invalid url"));

    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> paaSILInviaCarrelloDovutiService.processRequest(request, orgIpaCode, userInfo, TOKEN));

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
      .map(DebtPositionDTO::getDebtPositionId)
      .map(String::valueOf)
      .collect(Collectors.joining(Constants.SESSION_ID_SEPARATOR));

    when(paaSILInviaCarrelloDovutiMapperMock.mapRequestToDebtPositions(eq(request), argThat(c -> {cartId.set(c); return true;}), eq(userInfo), eq(orgIpaCode), eq(TOKEN)))
      .thenReturn(debtPositionDTOList);
    when(createDebtPositionServiceMock.createSyncedDebtPositions(debtPositionDTOList, TOKEN))
      .thenReturn(debtPositionDTOList);
    when(cartRequestMapperMock.mapDebtPositionsToCartRequest(eq(debtPositionDTOList), argThat(c -> c.equals(cartId.get())), eq(request.getEnteSILInviaRispostaPagamentoUrl())))
      .thenReturn(cartRequest);
    when(checkoutServiceMock.checkoutCart(cartRequest)).thenReturn("https://example.com/checkout");

    //when
    Triple<PaaSILInviaCarrelloDovutiRisposta, String, RegistryOutcome> response = paaSILInviaCarrelloDovutiService.processRequest(request, orgIpaCode, userInfo, TOKEN);

    //verify
    Assertions.assertNotNull(response);
    Assertions.assertEquals(RegistryOutcome.OK, response.getRight());
    Assertions.assertNotNull(response.getLeft());
    Assertions.assertNull(response.getLeft().getFault());
    Assertions.assertEquals(RegistryOutcome.OK.getValue(), response.getLeft().getEsito());
    Assertions.assertEquals("https://example.com/checkout", response.getLeft().getUrl());
    Assertions.assertEquals(1, response.getLeft().getRedirect());
    Assertions.assertEquals(sessionId, response.getLeft().getIdSessionCarrello());
    Assertions.assertEquals(iuvs, response.getMiddle());
  }
}
