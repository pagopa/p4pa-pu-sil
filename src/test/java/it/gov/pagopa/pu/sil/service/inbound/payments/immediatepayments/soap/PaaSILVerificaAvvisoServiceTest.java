package it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments.soap;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.exception.common.InvalidValueException;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.inbound.payments.debtposition.DebtPositionCheckoutService;
import it.gov.pagopa.pu.sil.service.inbound.payments.debtposition.InstallmentFacadeService;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILVerificaAvviso;
import it.veneto.regione.pagamenti.ente.PaaSILVerificaAvvisoRisposta;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaaSILVerificaAvvisoServiceTest {

  @Mock
  private InstallmentFacadeService installmentFacadeServiceMock;
  @Mock
  private OrganizationService organizationServiceMock;
  @Mock
  private DebtPositionCheckoutService debtPositionCheckoutServiceMock;

  private PaaSILVerificaAvvisoService paaSILVerificaAvvisoService;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  private UserInfo userInfo = null;
  private String orgIpaCode = null;
  private PaaSILVerificaAvviso request = null;
  private static final String TOKEN = "ACCESS_TOKEN";
  private Organization org = null;
  private Long orgId = null;

  private static final String AUX_DIGIT = "3";

  @BeforeEach
  void setUp() {
    Mockito.reset(installmentFacadeServiceMock, organizationServiceMock, debtPositionCheckoutServiceMock);

    userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", "OTHERIPACODE");
    orgIpaCode = userInfo.getOrganizations().getFirst().getOrganizationIpaCode();
    userInfo.getOrganizations().getFirst().setRoles(List.of("ROLE_X", "ROLE_ADMIN"));
    orgId = userInfo.getOrganizations().getFirst().getOrganizationId();
    org = podamFactory.manufacturePojo(Organization.class);
    org.setOrganizationId(orgId);
    org.setIpaCode(orgIpaCode);
    org.setStatus(OrganizationStatus.ACTIVE);

    request = podamFactory.manufacturePojo(PaaSILVerificaAvviso.class);
    request.setEnteSILInviaRispostaPagamentoUrl("https://example.com/callback");

    paaSILVerificaAvvisoService = new PaaSILVerificaAvvisoService(organizationServiceMock, installmentFacadeServiceMock, debtPositionCheckoutServiceMock, AUX_DIGIT);
  }

  @Test
  void givenNotAuthorizedUserWhenPaaSILVerificaAvvisoServiceThenError() {
    //given
    userInfo.getOrganizations().getFirst().setOrganizationIpaCode("INVALID_IPA_CODE");

    //when then
    Assertions.assertThrows(AuthorizationDeniedException.class, () -> paaSILVerificaAvvisoService.processRequest(request, orgIpaCode, userInfo, TOKEN));
  }

  @ParameterizedTest
  @ValueSource(strings = {"not_active_ipa"})
  @NullSource
  void givenInvalidOrganizationWhenPaaSILVerificaAvvisoServiceThenError(String testCase) {
    if(testCase==null){
      org = null;
    } else {
      org.setStatus(OrganizationStatus.DRAFT);
    }

    when(organizationServiceMock.getOrganizationById(anyLong(), anyString())).thenReturn(Optional.ofNullable(org));

    InvalidValueException response = Assertions.assertThrows(InvalidValueException.class, () -> paaSILVerificaAvvisoService.processRequest(request, orgIpaCode, userInfo, TOKEN));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_ORGANIZATION, response.getCode());
  }

  @Test
  void givenInvalidUrlWhenPaaSILVerificaAvvisoThenException() {
    //given
    request.setEnteSILInviaRispostaPagamentoUrl("http://");
    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));

    //when
    InvalidValueException response = Assertions.assertThrows(InvalidValueException.class, () -> paaSILVerificaAvvisoService.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_CALLBACK_URL, response.getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void givenNullIUVWhenPaaSILVerificaAvvisoThenException(String iuv) {
    //given
    request.setIdentificativoUnivocoVersamento(iuv);
    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));

    //when
    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> paaSILVerificaAvvisoService.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(ErrorCodeConstants.ERROR_CODE_MISSING_IUV, exception.getCode());
    Assertions.assertEquals("Identificativo univoco del versamento non indicato", exception.getSilFaultCustomMessage());
  }

  @Test
  void givenNotFoundIUVWhenPaaSILVerificaAvvisoThenException() {
    //given
    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(installmentFacadeServiceMock.getInstallmentsByOrganizationIdAndNav(orgId, "3"+request.getIdentificativoUnivocoVersamento(), TOKEN))
      .thenReturn(List.of());

    //when
    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> paaSILVerificaAvvisoService.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_IUV, exception.getCode());
    Assertions.assertEquals("Nessun avviso pagabile trovato per l'identificativo univoco del versamento indicato", exception.getSilFaultCustomMessage());
  }

  @Test
  void givenNotPayableIUVWhenPaaSILVerificaAvvisoThenException() {
    //given
    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    installmentDTO.setStatus(InstallmentStatus.EXPIRED);
    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(installmentFacadeServiceMock.getInstallmentsByOrganizationIdAndNav(orgId, "3"+request.getIdentificativoUnivocoVersamento(), TOKEN))
      .thenReturn(List.of(installmentDTO));

    //when
    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> paaSILVerificaAvvisoService.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_IUV, exception.getCode());
    Assertions.assertEquals("Nessun avviso pagabile trovato per l'identificativo univoco del versamento indicato", exception.getSilFaultCustomMessage());
  }

  @Test
  void givenMapCartRequestFaultWhenPaaSILVerificaAvvisoThenException() {
    //given
    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    installmentDTO.setStatus(InstallmentStatus.UNPAID);

    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(installmentFacadeServiceMock.getInstallmentsByOrganizationIdAndNav(orgId, "3"+request.getIdentificativoUnivocoVersamento(), TOKEN))
      .thenReturn(List.of(installmentDTO));
    when(debtPositionCheckoutServiceMock.composeDebtPositionsCheckoutUrl(anyLong(), anyString(), anyString(), anyString(), anyString()))
      .thenThrow(new InvalidValueException(ErrorCodeConstants.ERROR_CODE_INVALID_CALLBACK_URL, "invalid url"));

    //when
    InvalidValueException exception = Assertions.assertThrows(InvalidValueException.class, () -> paaSILVerificaAvvisoService.processRequest(request, orgIpaCode, userInfo, TOKEN));

    //verify
    Assertions.assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_CALLBACK_URL, exception.getCode());
    Assertions.assertEquals("invalid url", exception.getMessage());
  }

  @Test
  void givenValidRequestWhenPaaSILVerificaAvvisoThenOk() {
    //given
    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    installmentDTO.setStatus(InstallmentStatus.UNPAID);

    String sessionId = String.valueOf(installmentDTO.getInstallmentId());

    when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    when(installmentFacadeServiceMock.getInstallmentsByOrganizationIdAndNav(orgId, "3"+request.getIdentificativoUnivocoVersamento(), TOKEN))
      .thenReturn(List.of(installmentDTO));
    when(debtPositionCheckoutServiceMock.composeDebtPositionsCheckoutUrl(anyLong(), anyString(), anyString(), anyString(), anyString()))
      .thenReturn("https://example.com/checkout");

    //when
    PaaSILVerificaAvvisoRisposta response = paaSILVerificaAvvisoService.processRequest(request, orgIpaCode, userInfo, TOKEN);

    //verify
    Assertions.assertNotNull(response);
    Assertions.assertNull(response.getFault());
    Assertions.assertEquals(RegistryOutcome.OK.getValue(), response.getEsito());
    Assertions.assertEquals("https://example.com/checkout", response.getUrl());
    Assertions.assertEquals(1, response.getRedirect());
    Assertions.assertEquals(sessionId, response.getIdSession());
  }

}
