package it.gov.pagopa.pu.sil.service.immediatepayments;


import static it.gov.pagopa.pu.debtpositions.dto.generated.CategoryEnum.DEBT_POSITION_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionErrorDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.PaaSILInviaDovutiMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionCheckoutService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovutiRisposta;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpServerErrorException;
import uk.co.jemos.podam.api.PodamFactory;

@ExtendWith(MockitoExtension.class)
class PaaSILInviaDovutiServiceTest {

  @Mock
  private PaaSILInviaDovutiMapper paaSILInviaDovutiMapperMock;
  @Mock
  private InstantPaymentsFacade instantPaymentsFacadeMock;
  @Mock
  private OrganizationService organizationServiceMock;
  @Mock
  private SessionIdMapper sessionIdMapperMock;
  @Mock
  private DebtPositionCheckoutService debtPositionCheckoutServiceMock;

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
    Mockito.reset(paaSILInviaDovutiMapperMock, instantPaymentsFacadeMock, organizationServiceMock, sessionIdMapperMock, debtPositionCheckoutServiceMock);

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
  void givenNotAuthorizedUserWhenPaaSILInviaDovutiThenError() {
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
  void givenInvalidOrganizationWhenPaaSILInviaDovutiThenError(String testCase) {
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
    PaymentRequestMappingResult paymentRequestMappingResult = PaymentRequestMappingResult.ofDebtPositions(debtPositionDTOList);

    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(paaSILInviaDovutiMapperMock.mapRequestToDebtPositions(eq(request), eq(org), any(), eq(TOKEN)))
      .thenReturn(paymentRequestMappingResult);
    when(instantPaymentsFacadeMock.createDebtPositionsFromMapping(paymentRequestMappingResult, TOKEN))
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
    PaymentRequestMappingResult paymentRequestMappingResult = PaymentRequestMappingResult.ofDebtPositions(debtPositionDTOList);

    AtomicReference<String> cartId = new AtomicReference<>();

    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(paaSILInviaDovutiMapperMock.mapRequestToDebtPositions(eq(request), eq(org), argThat(c -> {cartId.set(c); return true;}), eq(TOKEN)))
      .thenReturn(paymentRequestMappingResult);
    when(instantPaymentsFacadeMock.createDebtPositionsFromMapping(paymentRequestMappingResult, TOKEN))
      .thenReturn(debtPositionDTOList);
    when(debtPositionCheckoutServiceMock.composeDebtPositionsCheckoutUrl(anyLong(), anyString(), anyString(), anyString(), anyString()))
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
    PaymentRequestMappingResult paymentRequestMappingResult = PaymentRequestMappingResult.ofDebtPositions(debtPositionDTOList);

    String iuvs = debtPositionDTOList.stream()
      .flatMap(dp -> dp.getPaymentOptions().stream())
      .flatMap(option -> option.getInstallments().stream())
      .map(InstallmentDTO::getIuv)
      .collect(Collectors.joining(Utilities.IUV_SEPARATOR));

    String sessionId = "SESSION_ID";

    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(paaSILInviaDovutiMapperMock.mapRequestToDebtPositions(eq(request), eq(org), argThat(c -> {cartId.set(c); return true;}), eq(TOKEN)))
      .thenReturn(paymentRequestMappingResult);
    when(instantPaymentsFacadeMock.createDebtPositionsFromMapping(paymentRequestMappingResult, TOKEN))
      .thenReturn(debtPositionDTOList);
    when(sessionIdMapperMock.mapDebtPositionsToSessionId(debtPositionDTOList)).thenReturn(sessionId);
    when(debtPositionCheckoutServiceMock.composeDebtPositionsCheckoutUrl(anyLong(), anyString(), anyString(), anyString(), anyString()))
      .thenReturn("https://example.com/checkout");

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

  @ParameterizedTest
  @MethodSource("provideErrorScenarios")
  void givenMockedExceptionWhenCreateDebtPositionThenSilFaultException(DebtPositionErrorDTO errorDTO, SilFaultException expected) {
    //given
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    List<DebtPositionDTO> debtPositionDTOList = List.of(debtPositionDTO);
    PaymentRequestMappingResult paymentRequestMappingResult = PaymentRequestMappingResult.ofDebtPositions(debtPositionDTOList);
    HttpServerErrorException mockedException = Mockito.mock(HttpServerErrorException.class);
    Mockito.when(mockedException.getResponseBodyAs(DebtPositionErrorDTO.class)).thenReturn(errorDTO);

    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(paaSILInviaDovutiMapperMock.mapRequestToDebtPositions(eq(request), eq(org), any(), eq(TOKEN)))
      .thenReturn(paymentRequestMappingResult);
    when(instantPaymentsFacadeMock.createDebtPositionsFromMapping(paymentRequestMappingResult, TOKEN))
      .thenThrow(mockedException);

    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> paaSILInviaDovutiService.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    assertEquals(expected.getFault().code(), exception.getFault().code());
    assertEquals(expected.getDescription(), exception.getDescription());
  }

  static Stream<Arguments> provideErrorScenarios() {
    DebtPositionErrorDTO.DebtPositionErrorDTOBuilder errorDTO = DebtPositionErrorDTO.builder().category(DEBT_POSITION_BAD_REQUEST);
    return Stream.of(
      Arguments.of(null, new SilFaultException(SilFaults.PAA_SYSTEM_ERROR, "errore durante la creazione delle posizioni debitorie")),
      Arguments.of(errorDTO.message("[P4PA_INVALID_TAXONOMY_CATEGORY] Some error occurred").build(), new SilFaultException(SilFaults.fromNativeFault2LegacyCode("P4PA_INVALID_TAXONOMY_CATEGORY"), "Some error occurred")),
      Arguments.of(errorDTO.message("Some error without brackets").build(), new SilFaultException(SilFaults.PAA_SYSTEM_ERROR, "Some error without brackets"))
    );
  }
}
