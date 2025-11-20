package it.gov.pagopa.pu.sil.service.singleimport;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.ManageDebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.ManageDebtPositionMapper;
import it.gov.pagopa.pu.sil.mapper.PaaSILImportaDovutoMapper;
import it.gov.pagopa.pu.sil.service.debtposition.ManageDebtPositionService;
import it.gov.pagopa.pu.sil.service.notice.NoticeService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovuto;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovutoRisposta;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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
class PaaSILImportaDovutoServiceTest {

  private PaaSILImportaDovutoService paaSILImportaDovutoService;

  @Mock
  private OrganizationService organizationServiceMock;
  @Mock
  private PaaSILImportaDovutoMapper paaSILImportaDovutoMapperMock;
  @Mock
  private ManageDebtPositionMapper manageDebtPositionMapperMock;
  @Mock
  private DebtPositionService debtPositionServiceMock;
  @Mock
  private ManageDebtPositionService manageDebtPositionServiceMock;
  @Mock
  private NoticeService noticeServiceMock;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  private UserInfo userInfo = null;
  private String orgIpaCode = null;
  private PaaSILImportaDovuto request = null;
  private static final String TOKEN = "ACCESS_TOKEN";
  private Organization org = null;
  private Long orgId = null;

  @BeforeEach
  void setUp() {
    podamFactory.getStrategy().setDefaultNumberOfCollectionElements(1);

    Mockito.reset(organizationServiceMock, paaSILImportaDovutoMapperMock, debtPositionServiceMock, manageDebtPositionServiceMock, noticeServiceMock);

    this.paaSILImportaDovutoService = new PaaSILImportaDovutoService(
      organizationServiceMock,
      debtPositionServiceMock,
      manageDebtPositionServiceMock,
      noticeServiceMock,
      paaSILImportaDovutoMapperMock,
      manageDebtPositionMapperMock,
      "PU_SIL_BASE_URL");

    userInfo = podamFactory.manufacturePojo(UserInfo.class);
    orgIpaCode = userInfo.getOrganizations().getFirst().getOrganizationIpaCode();
    userInfo.getOrganizations().getFirst().setRoles(List.of("ROLE_X", "ROLE_ADMIN"));
    orgId = userInfo.getOrganizations().getFirst().getOrganizationId();
    org = podamFactory.manufacturePojo(Organization.class);
    org.setOrganizationId(orgId);
    org.setIpaCode(orgIpaCode);
    org.setStatus(OrganizationStatus.ACTIVE);

    request = podamFactory.manufacturePojo(PaaSILImportaDovuto.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {Constants.LEGACY_IMPORT_ACTION_INSERT, Constants.LEGACY_IMPORT_ACTION_MODIFY, Constants.LEGACY_IMPORT_ACTION_CANCEL, Constants.LEGACY_IMPORT_ACTION_PRINT})
  void givenValidDataWhenPaaSILImportaDovutoThenOk(String action) {
    // Given
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    InstallmentDTO installmentDTO = debtPositionDTO.getPaymentOptions().getLast().getInstallments().getLast();
    debtPositionDTO.setOrganizationId(orgId);
    Mockito.when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    String iud = installmentDTO.getIud();
    String iuv = installmentDTO.getIuv();
    DebtPositionDTO processedDebtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    processedDebtPositionDTO.setDebtPositionTypeOrgId(debtPositionDTO.getDebtPositionTypeOrgId());
    processedDebtPositionDTO.setStatus(DebtPositionStatus.UNPAID);
    InstallmentDTO processedInstallmentDTO = processedDebtPositionDTO.getPaymentOptions().getLast().getInstallments().getLast();
    processedInstallmentDTO.setIud(iud);
    processedInstallmentDTO.setIuv(iuv);
    Mockito.when(paaSILImportaDovutoMapperMock.mapRequestToDebtPosition(request, org, TOKEN)).thenReturn(Pair.of(debtPositionDTO, action));

    switch (action) {
      case Constants.LEGACY_IMPORT_ACTION_INSERT:
        Mockito.when(manageDebtPositionServiceMock.createDebtPositions(List.of(debtPositionDTO), TOKEN)).thenReturn(List.of(processedDebtPositionDTO));
        break;
      case Constants.LEGACY_IMPORT_ACTION_MODIFY, Constants.LEGACY_IMPORT_ACTION_CANCEL:
        Mockito.when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIud(eq(orgId), eq(iud), Mockito.any(), eq(TOKEN)))
          .thenReturn(List.of(processedDebtPositionDTO));
        ManageDebtPositionDTO manageDebtPositionDTO = podamFactory.manufacturePojo(ManageDebtPositionDTO.class);
        Mockito.when(manageDebtPositionMapperMock.mapToManageDebtPositionDTO(
          eq(processedDebtPositionDTO),
          eq(debtPositionDTO),
          eq(installmentDTO),
          eq(action),
          eq(true)
        )).thenReturn(manageDebtPositionDTO);
        Mockito.when(manageDebtPositionServiceMock.manageDebtPositionInstallments(processedDebtPositionDTO.getDebtPositionId(), manageDebtPositionDTO, TOKEN))
          .thenReturn(processedDebtPositionDTO);
        break;
      case Constants.LEGACY_IMPORT_ACTION_PRINT:
        Mockito.when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIud(eq(orgId), eq(iud), Mockito.any(), eq(TOKEN)))
          .thenReturn(List.of(processedDebtPositionDTO));
        Mockito.when(noticeServiceMock.generateNotice(processedInstallmentDTO.getIuv(), processedDebtPositionDTO, TOKEN))
          .thenReturn("PDF NOTICE".getBytes());
        break;
      default:
        Assertions.fail("Unexpected action: " + action);
    }

    // When
    Triple<PaaSILImportaDovutoRisposta, String, RegistryOutcome> response = paaSILImportaDovutoService.handleAction(request, orgIpaCode, userInfo, TOKEN);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(iuv, response.getMiddle());
    Assertions.assertEquals(RegistryOutcome.OK, response.getRight());
    PaaSILImportaDovutoRisposta paaSILImportaDovutoRisposta = response.getLeft();
    Assertions.assertEquals(RegistryOutcome.OK.getValue(), paaSILImportaDovutoRisposta.getEsito());
    Assertions.assertEquals(iuv, paaSILImportaDovutoRisposta.getIdentificativoUnivocoVersamento());

    if (action.equals(Constants.LEGACY_IMPORT_ACTION_INSERT) || action.equals(Constants.LEGACY_IMPORT_ACTION_MODIFY)) {
      Assertions.assertNotNull(paaSILImportaDovutoRisposta.getUrlFileAvviso());
    } else {
      Assertions.assertNull(paaSILImportaDovutoRisposta.getUrlFileAvviso());
    }
    if (action.equals(Constants.LEGACY_IMPORT_ACTION_PRINT)) {
      Assertions.assertNotNull(paaSILImportaDovutoRisposta.getBase64ZipAvviso());
      Assertions.assertInstanceOf(ByteArrayDataSource.class, paaSILImportaDovutoRisposta.getBase64ZipAvviso().getDataSource());
    } else {
      Assertions.assertNull(paaSILImportaDovutoRisposta.getBase64ZipAvviso());
    }

  }

  @Test
  void givenNotAuthorizedUserWhenPaaSILInviaDovutiThenError() {
    //given
    userInfo.getOrganizations().getFirst().setOrganizationIpaCode("INVALID_IPA_CODE");

    //when then
    Assertions.assertThrows(AuthorizationDeniedException.class, () -> paaSILImportaDovutoService.handleAction(request, orgIpaCode, userInfo, TOKEN));
  }

  @ParameterizedTest
  @ValueSource(strings = {"not_active_ipa"})
  @NullSource
  void givenInvalidOrganizationWhenPaaSILInviaDovutiThenError(String testCase) {
    if (testCase == null) {
      org = null;
    } else {
      org.setStatus(OrganizationStatus.DRAFT);
    }

    when(organizationServiceMock.getOrganizationById(anyLong(), anyString())).thenReturn(Optional.ofNullable(org));

    SilFaultException response = Assertions.assertThrows(SilFaultException.class, () -> paaSILImportaDovutoService.handleAction(request, orgIpaCode, userInfo, TOKEN));

    assertEquals(SilFaults.PAA_ENTE_NON_VALIDO, response.getFault());
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalidAction",})
  void givenInvalidDataWhenPaaSILImportaDovutoThenException(String testCase) {
    // Given
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    InstallmentDTO installmentDTO = debtPositionDTO.getPaymentOptions().getLast().getInstallments().getLast();
    debtPositionDTO.setOrganizationId(orgId);
    Mockito.when(organizationServiceMock.getOrganizationById(orgId, TOKEN)).thenReturn(Optional.of(org));
    String iud = installmentDTO.getIud();
    String iuv = installmentDTO.getIuv();
    DebtPositionDTO processedDebtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    processedDebtPositionDTO.setStatus(DebtPositionStatus.UNPAID);
    InstallmentDTO processedInstallmentDTO = processedDebtPositionDTO.getPaymentOptions().getLast().getInstallments().getLast();
    processedInstallmentDTO.setIud(iud);
    processedInstallmentDTO.setIuv(iuv);


    switch (testCase) {
      case "invalidAction":
        Mockito.when(paaSILImportaDovutoMapperMock.mapRequestToDebtPosition(request, org, TOKEN)).thenReturn(Pair.of(debtPositionDTO, "INVALID"));
        break;
      case "invalidDpStatus":
        processedDebtPositionDTO.setStatus(DebtPositionStatus.PAID);
        Mockito.when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIud(eq(orgId), eq(iud), Mockito.any(), eq(TOKEN)))
          .thenReturn(List.of(processedDebtPositionDTO));
        break;
      case "installmentNotFound":
        processedInstallmentDTO.setIud("invalid_iud");
        Mockito.when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIud(eq(orgId), eq(iud), Mockito.any(), eq(TOKEN)))
          .thenReturn(List.of(processedDebtPositionDTO));
        break;
      default:
        Assertions.fail("Unexpected testCase: " + testCase);
    }

    // When
    SilFaultException response = Assertions.assertThrows(SilFaultException.class, () -> paaSILImportaDovutoService.handleAction(request, orgIpaCode, userInfo, TOKEN));

    // Then
    switch (testCase) {
      case "invalidAction":
        assertEquals(SilFaults.PAA_AZIONE_NON_VALIDA, response.getFault());
        break;
      case "invalidDpStatus", "installmentNotFound":
        assertEquals(SilFaults.PAA_IMPORT_DOVUTO_NON_PRESENTE, response.getFault());
        break;
      default:
        Assertions.fail("Unexpected testCase: " + testCase);
    }
  }

}
