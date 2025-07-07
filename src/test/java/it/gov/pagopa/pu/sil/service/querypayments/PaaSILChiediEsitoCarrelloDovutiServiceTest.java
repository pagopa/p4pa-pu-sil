package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILChiediEsitoCarrelloDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILChiediEsitoCarrelloDovutiRisposta;
import it.veneto.regione.pagamenti.ente.RispostaCarrello;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaaSILChiediEsitoCarrelloDovutiServiceTest {

  @InjectMocks
  private PaaSILChiediEsitoCarrelloDovutiService paaSILChiediEsitoCarrelloDovutiService;

  @Mock
  private DebtPositionService debtPositionServiceMock;
  @Mock
  private PagatiMapper pagatiMapperMock;
  @Mock
  private SessionIdMapper sessionIdMapperMock;
  @Mock
  private OrganizationService organizationServiceMock;
  @Mock
  private ReceiptService receiptServiceMock;

  private final PodamFactory podamFactory;

  private String accessToken;
  private String sessionId;
  private UserInfo userInfo;
  private Organization organization;
  private PaaSILChiediEsitoCarrelloDovuti request;
  private List<Long> installmentIds;
  private List<Pair<DebtPositionDTO, InstallmentDTO>> pairList;

  PaaSILChiediEsitoCarrelloDovutiServiceTest() {
    this.podamFactory = TestUtils.getPodamFactory();
    podamFactory.getStrategy().setDefaultNumberOfCollectionElements(2);
  }

  @BeforeEach
  void init() {
    accessToken = "accessToken";
    sessionId = "sessionId";
    userInfo = podamFactory.manufacturePojo(UserInfo.class);
    request = podamFactory.manufacturePojo(PaaSILChiediEsitoCarrelloDovuti.class);
    organization = podamFactory.manufacturePojo(Organization.class);
    organization.setStatus(OrganizationStatus.ACTIVE);
    UserOrganizationRoles uor = userInfo.getOrganizations().getFirst();
    uor.setOrganizationId(organization.getOrganizationId());
    uor.setOrganizationIpaCode(organization.getIpaCode());
    uor.setRoles(List.of(AuthorizationService.ROLE_ADMIN));
    request.setCodIpaEnte(organization.getIpaCode());
    request.setIdSessionCarrello(sessionId);

    installmentIds = List.of(1L, 2L);
    pairList = installmentIds.stream().map(i -> {
      DebtPositionDTO dp = podamFactory.manufacturePojo(DebtPositionDTO.class);
      dp.setOrganizationId(organization.getOrganizationId());
      dp.setStatus(DebtPositionStatus.PAID);
      InstallmentDTO inst = dp.getPaymentOptions().getFirst().getInstallments().getFirst();
      inst.setInstallmentId(i);
      inst.setStatus(InstallmentStatus.PAID);
      return Pair.of(dp, inst);
    }).toList();
  }


  @ParameterizedTest
  @CsvSource(value = {
    "unpaidInstallment,NUOVO_CARRELLO",
    "unpaidToSyncInstallment,NUOVO_CARRELLO",
    "expiredInstallment,SCADUTO",
    "invalidStatusInstallment,NON_PAGATO",
    "valid,PAGATO"
  }, nullValues = {"null"})
  void testGetDebtPositionsAndInstallmentsOk(String testCase, String expectedOutcome) throws IOException {
    byte[] encodedPagati = "encodedPagati".getBytes(StandardCharsets.UTF_8);
    byte[] encodedRt = "encodedRt".getBytes(StandardCharsets.UTF_8);

    // change input data to fit testCase
    switch (testCase) {
      case "unpaidInstallment":
        pairList.getFirst().getRight().setStatus(InstallmentStatus.UNPAID);
        break;
      case "unpaidToSyncInstallment":
        pairList.getFirst().getRight().setStatus(InstallmentStatus.TO_SYNC);
        pairList.getFirst().getRight().setSyncStatus(new InstallmentSyncStatus(InstallmentStatus.DRAFT, InstallmentStatus.UNPAID, null));
        break;
      case "expiredInstallment":
        pairList.getFirst().getRight().setStatus(InstallmentStatus.EXPIRED);
        break;
      case "invalidStatusInstallment":
        pairList.getFirst().getRight().setStatus(InstallmentStatus.UNPAYABLE);
        break;
      default:
        //nothing to do
    }

    when(organizationServiceMock.getOrganizationById(organization.getOrganizationId(), accessToken)).thenReturn(Optional.of(organization));
    when(sessionIdMapperMock.mapSessionIdToInstallmentIds(sessionId)).thenReturn(installmentIds);
    pairList.forEach(pair ->
      when(debtPositionServiceMock.getDebtPositionByInstallmentId(pair.getRight().getInstallmentId(), accessToken)).thenReturn(pair.getLeft())
    );
    if(testCase.equals("valid")) {
      //TODO currently support only one debt position and installment, but could be extended to support multiple
      Pair<DebtPositionDTO, InstallmentDTO> firstPair = pairList.getFirst();
      when(pagatiMapperMock.mapDebtPositionsToEncodedPagatiConRicevuta(firstPair.getRight(), organization, accessToken)).thenReturn(encodedPagati);
      when(receiptServiceMock.getReceiptById(firstPair.getRight().getReceiptId(), organization.getOrganizationId(), accessToken)).thenReturn(encodedRt);
    }

    PaaSILChiediEsitoCarrelloDovutiRisposta result = paaSILChiediEsitoCarrelloDovutiService.processRequest(request, userInfo, accessToken);

    assertNotNull(result);
    assertNull(result.getFault());
    assertNotNull(result.getListaCarrelli());
    assertEquals(1, result.getListaCarrelli().getRispostaCarrellos().size());
    RispostaCarrello cartResponse = result.getListaCarrelli().getRispostaCarrellos().getFirst();
    assertEquals(organization.getIpaCode(), cartResponse.getCodIpaEnte());
    assertEquals(expectedOutcome, cartResponse.getEsito());
    if(testCase.equals("valid")) {
      assertInstanceOf(ByteArrayDataSource.class, cartResponse.getPagati().getDataSource());
      assertInstanceOf(ByteArrayInputStream.class, cartResponse.getPagati().getDataSource().getInputStream());
      assertArrayEquals(encodedPagati, ((ByteArrayInputStream) cartResponse.getPagati().getDataSource().getInputStream()).readAllBytes());
    } else {
      assertNull(cartResponse.getPagati());
      assertNull(cartResponse.getRt());
    }
  }

  @ParameterizedTest
  @CsvSource(value = {
    "userNotAuth,PAA_ENTE_NON_VALIDO,Utente non autorizzato",
    "invalidOrgStatus,PAA_ENTE_NON_VALIDO,L'ente non è valido o non è abilitato",
    "emptyDebtPositionList,PAA_ID_SESSION_NON_VALIDO,Nessuna posizione debitoria trovata",
    "invalidOrgDebtPosition,PAA_ID_SESSION_NON_VALIDO,Posizione debitoria non trovata",
  }, nullValues = {"null"})
  void testGetDebtPositionsAndInstallmentsFault(String testCase, String silFaultCode, String faultDescription) {

    // change input data to fit testCase
    switch (testCase) {
      case "userNotAuth":
        userInfo.getOrganizations().stream()
          .filter(o -> o.getOrganizationIpaCode().equals(request.getCodIpaEnte()))
          .findFirst()
          .ifPresent(o -> o.setRoles(List.of("NOT_ADMIN")));
        break;
      case "invalidOrgStatus":
        organization.setStatus(OrganizationStatus.DRAFT);
        break;
      case "emptyDebtPositionList":
        installmentIds = List.of();
        break;
      case "invalidOrgDebtPosition":
        pairList.getFirst().getLeft().setOrganizationId(organization.getOrganizationId() + 1);
        break;
      default:
        //nothing to do
    }

    // mock only used methods of testCase
    switch (testCase) {
      case "invalidOrgStatus":
        when(organizationServiceMock.getOrganizationById(organization.getOrganizationId(), accessToken)).thenReturn(Optional.of(organization));
        break;
      case "emptyDebtPositionList":
        when(organizationServiceMock.getOrganizationById(organization.getOrganizationId(), accessToken)).thenReturn(Optional.of(organization));
        when(sessionIdMapperMock.mapSessionIdToInstallmentIds(sessionId)).thenReturn(installmentIds);
        break;
      case "invalidOrgDebtPosition":
        when(organizationServiceMock.getOrganizationById(organization.getOrganizationId(), accessToken)).thenReturn(Optional.of(organization));
        when(sessionIdMapperMock.mapSessionIdToInstallmentIds(sessionId)).thenReturn(installmentIds);
        pairList.forEach(pair ->
          when(debtPositionServiceMock.getDebtPositionByInstallmentId(pair.getRight().getInstallmentId(), accessToken)).thenReturn(pair.getLeft())
        );
        break;
      case "userNotAuth":
      default:
        //do nothing
    }

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> paaSILChiediEsitoCarrelloDovutiService.processRequest(request, userInfo, accessToken));

    assertNotNull(result);
    assertNotNull(result.getFault());
    assertEquals(silFaultCode, result.getFault().code());
    assertEquals(faultDescription, result.getDescription());
  }

}
