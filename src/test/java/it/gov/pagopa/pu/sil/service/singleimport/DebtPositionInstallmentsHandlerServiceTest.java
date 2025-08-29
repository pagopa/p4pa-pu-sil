package it.gov.pagopa.pu.sil.service.singleimport;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.ManageDebtPositionWithIudDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.DebtPositionMapper;
import it.gov.pagopa.pu.sil.mapper.PaaSILImportaDovutoMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.debtposition.ManageDebtPositionService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebtPositionInstallmentsHandlerServiceTest {
  @InjectMocks
  private DebtPositionInstallmentsHandlerService service;

  @Mock
  private OrganizationService organizationServiceMock;
  @Mock
  private DebtPositionMapper debtPositionMapperMock;
  @Mock
  private ManageDebtPositionService manageDebtPositionServiceMock;
  @Mock
  private PaaSILImportaDovutoMapper paaSILImportaDovutoMapperMock;
  @Mock
  private DebtPositionService debtPositionServiceMock;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();
  private static final String TOKEN = "ACCESS_TOKEN";
  private static final String IPA_CODE = "IPA_CODE";

  private UserInfo userInfo;
  private Organization org;
  ManageDebtPositionWithIudDTO request;

  @BeforeEach
  void init() {
    org = podamFactory.manufacturePojo(Organization.class);
    org.setStatus(OrganizationStatus.ACTIVE);
    org.setIpaCode(IPA_CODE);
    userInfo = AuthorizationServiceTest.buildAdminUser(org.getOrganizationId(), org.getOrgFiscalCode(), org.getIpaCode());
    request = podamFactory.manufacturePojo(ManageDebtPositionWithIudDTO.class);
  }

  @Test
  void givenNotAuthorizedUserWhenHandleActionThenError() {
    userInfo.getOrganizations().getFirst().setOrganizationIpaCode("INVALID_IPA_CODE");
    Assertions.assertThrows(AuthorizationDeniedException.class, () -> service.handleAction(request, IPA_CODE, userInfo, TOKEN));
  }

  @ParameterizedTest
  @ValueSource(strings = {"not_active_ipa"})
  @NullSource
  void givenInvalidOrganizationWhenHandleActionThenError(String testCase) {
    if (testCase == null) {
      org = null;
    } else {
      org.setStatus(OrganizationStatus.DRAFT);
    }
    when(organizationServiceMock.getOrganizationById(anyLong(), anyString())).thenReturn(Optional.ofNullable(org));
    Assertions.assertThrows(SilFaultException.class, () -> service.handleAction(request, IPA_CODE, userInfo, TOKEN));
  }


  @ParameterizedTest
  @CsvSource({
    "TO_SYNC, M",
    "UNPAID, A"
  })
  void whenHandleActionThenDebtPositionNotFound(String status, String action) {
    // Arrange
    String requestIud = "requestIud";
    request.getInstallments().getFirst().setAction(Action.fromValue(action));
    request.getInstallments().getFirst().getInstallment().setIud(requestIud);
    request.setIud(requestIud);
    InstallmentDTO installment = podamFactory.manufacturePojo(InstallmentDTO.class);
    installment.setIud(requestIud);
    DebtPositionDTO debtPosition = podamFactory.manufacturePojo(DebtPositionDTO.class);
    debtPosition.setStatus(DebtPositionStatus.fromValue(status));
    debtPosition.organizationId(org.getOrganizationId());
    debtPosition.getPaymentOptions().getFirst().setInstallments(List.of(installment));

    when(organizationServiceMock.getOrganizationById(org.getOrganizationId(), TOKEN)).thenReturn(Optional.ofNullable(org));
    when(debtPositionMapperMock.mapSilInstallmentToInstallmentDTO(request.getInstallments().getFirst().getInstallment(), org.getIpaCode()))
      .thenReturn(installment);
    when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIud(org.getOrganizationId(), requestIud, Constants.ORDINARY_DEBT_POSITION_ORIGINS, TOKEN))
      .thenReturn(List.of());

    // Act &Assert
    assertThrows(SilFaultException.class, () -> service.handleAction(request, IPA_CODE, userInfo, TOKEN));
  }

  @Test
  void givenNotCoherentIudWhenHandleActionThenError() {
    // Arrange
    String requestIud = "requestIud";
    request.getInstallments().getFirst().setAction(Action.M);
    request.getInstallments().getFirst().getInstallment().setIud(requestIud+"invalid");
    request.setIud(requestIud);

    when(organizationServiceMock.getOrganizationById(org.getOrganizationId(), TOKEN)).thenReturn(Optional.ofNullable(org));
    // Act &Assert
    SilFaultException response = assertThrows(SilFaultException.class, () -> service.handleAction(request, IPA_CODE, userInfo, TOKEN));
    assertEquals(SilFaults.PAA_IUD_NON_VALIDO, response.getFault());
  }

  @Test
  void givenNotUniqueIudWhenHandleActionThenError() {
    // Arrange
    String requestIud = "requestIud";
    Assertions.assertTrue(request.getInstallments().size()>1);
    request.getInstallments().getFirst().setAction(Action.M);
    request.getInstallments().getFirst().getInstallment().setIud(requestIud);
    request.getInstallments().getLast().setAction(Action.M);
    request.getInstallments().getLast().getInstallment().setIud(requestIud);
    request.setIud(requestIud);

    when(organizationServiceMock.getOrganizationById(org.getOrganizationId(), TOKEN)).thenReturn(Optional.ofNullable(org));
    // Act &Assert
    SilFaultException response = assertThrows(SilFaultException.class, () -> service.handleAction(request, IPA_CODE, userInfo, TOKEN));
    assertEquals(SilFaults.PAA_IUD_DUPLICATO, response.getFault());
  }

  @Test
  void givenInvalidActionWhenHandleActionThenError() {
    // Arrange
    String requestIud = "requestIud";
    request.getInstallments().getFirst().setAction(Action.I);
    request.getInstallments().getFirst().getInstallment().setIud(requestIud);
    request.setIud(requestIud);

    when(organizationServiceMock.getOrganizationById(org.getOrganizationId(), TOKEN)).thenReturn(Optional.ofNullable(org));

    // Act &Assert
    SilFaultException response = assertThrows(SilFaultException.class, () -> service.handleAction(request, IPA_CODE, userInfo, TOKEN));
    assertEquals(SilFaults.PAA_AZIONE_NON_VALIDA, response.getFault());
  }

  @ParameterizedTest
  @EnumSource(value = Action.class, names = {"A", "M"})
  void whenHendleActionThenOk(Action action) {
    // Arrange
    String iud = "iud";
    String iuv = "iuv";
    request.getInstallments().getFirst().setAction(action);
    request.getInstallments().getFirst().getInstallment().setIud(iud);
    request.setIud(iud);
    InstallmentDTO installment = podamFactory.manufacturePojo(InstallmentDTO.class);
    installment.setIud(iud);
    installment.setIuv(iuv);
    DebtPositionDTO debtPosition = podamFactory.manufacturePojo(DebtPositionDTO.class);
    debtPosition.setStatus(DebtPositionStatus.UNPAID);
    debtPosition.organizationId(org.getOrganizationId());
    debtPosition.getPaymentOptions().getFirst().setInstallments(List.of(installment));
    ManageDebtPositionDTO manageDebtPositionDTO = podamFactory.manufacturePojo(ManageDebtPositionDTO.class);
    Triple<DebtPositionDTO, String, RegistryOutcome> expected = Triple.of(debtPosition, iuv, RegistryOutcome.OK);

    when(organizationServiceMock.getOrganizationById(org.getOrganizationId(), TOKEN)).thenReturn(Optional.ofNullable(org));
    when(debtPositionMapperMock.mapSilInstallmentToInstallmentDTO(request.getInstallments().getFirst().getInstallment(), org.getIpaCode()))
      .thenReturn(installment);
    when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIud(org.getOrganizationId(), iud, Constants.ORDINARY_DEBT_POSITION_ORIGINS, TOKEN))
      .thenReturn(List.of(debtPosition));
    when(paaSILImportaDovutoMapperMock.mapToManageDebtPositionDTO(debtPosition, installment, request.getInstallments().getFirst().getAction().getValue()))
      .thenReturn(manageDebtPositionDTO);
    when(manageDebtPositionServiceMock.manageDebtPositionInstallments(debtPosition.getDebtPositionId(), manageDebtPositionDTO, TOKEN))
      .thenReturn(debtPosition);

    Triple<DebtPositionDTO, String, RegistryOutcome> result = service.handleAction(request, IPA_CODE, userInfo, TOKEN);

    assertEquals(expected, result);
  }
}
