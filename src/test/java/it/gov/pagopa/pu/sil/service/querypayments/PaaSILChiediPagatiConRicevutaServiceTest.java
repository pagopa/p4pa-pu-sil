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
import it.veneto.regione.pagamenti.ente.PaaSILChiediPagatiConRicevuta;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPagatiConRicevutaRisposta;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.pu.sil.service.querypayments.PaaSILChiediPagatiConRicevutaService.ALLOWED_ORIGINS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaaSILChiediPagatiConRicevutaServiceTest {

  @InjectMocks
  private PaaSILChiediPagatiConRicevutaService paaSILChiediPagatiConRicevutaService;

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
  private PaaSILChiediPagatiConRicevuta request;
  private List<Long> installmentIds;
  private List<Pair<DebtPositionDTO, InstallmentDTO>> pairList;

  PaaSILChiediPagatiConRicevutaServiceTest() {
    this.podamFactory = TestUtils.getPodamFactory();
    podamFactory.getStrategy().setDefaultNumberOfCollectionElements(2);
  }

  @BeforeEach
  void init() {
    accessToken = "accessToken";
    sessionId = "sessionId";
    userInfo = podamFactory.manufacturePojo(UserInfo.class);
    request = podamFactory.manufacturePojo(PaaSILChiediPagatiConRicevuta.class);
    organization = podamFactory.manufacturePojo(Organization.class);
    organization.setStatus(OrganizationStatus.ACTIVE);
    UserOrganizationRoles uor = userInfo.getOrganizations().getFirst();
    uor.setOrganizationId(organization.getOrganizationId());
    uor.setOrganizationIpaCode(organization.getIpaCode());
    uor.setRoles(List.of(AuthorizationService.ROLE_ADMIN));
    request.setCodIpaEnte(organization.getIpaCode());
    request.setIdSession(null);
    request.setIdentificativoUnivocoDovuto(null);
    request.setIdentificativoUnivocoVersamento(null);

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
  @ValueSource(strings = {"sessionId", "iuv", "iud"})
  void testGetDebtPositionsAndInstallmentsOk(String testCase) throws IOException {
    byte[] encodedPagati = "encodedPagati".getBytes(StandardCharsets.UTF_8);
    byte[] encodedRt = "encodedRt".getBytes(StandardCharsets.UTF_8);

    if ("sessionId".equals(testCase)) {
      request.setIdSession(sessionId);
      when(sessionIdMapperMock.mapSessionIdToInstallmentIds(sessionId)).thenReturn(installmentIds);
      pairList.forEach(pair ->
        when(debtPositionServiceMock.getDebtPositionByInstallmentId(pair.getRight().getInstallmentId(), accessToken)).thenReturn(pair.getLeft())
      );
    } else if ("iuv".equals(testCase)) {
      String iuv = pairList.getFirst().getRight().getIuv();
      request.setIdentificativoUnivocoVersamento(iuv);
      when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIuv(organization.getOrganizationId(),
        iuv, ALLOWED_ORIGINS, accessToken)).thenReturn(List.of(pairList.getFirst().getLeft()));
    } else if ("iud".equals(testCase)) {
      String iud = pairList.getFirst().getRight().getIud();
      request.setIdentificativoUnivocoDovuto(iud);
      when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIud(organization.getOrganizationId(),
        iud, ALLOWED_ORIGINS, accessToken)).thenReturn(List.of(pairList.getFirst().getLeft()));
    }

    when(organizationServiceMock.getOrganizationById(organization.getOrganizationId(), accessToken)).thenReturn(Optional.of(organization));
    //TODO currently support only one debt position and installment, but could be extended to support multiple
    Pair<DebtPositionDTO, InstallmentDTO> firstPair = pairList.getFirst();
    when(pagatiMapperMock.mapDebtPositionsToEncodedPagatiConRicevuta(firstPair.getRight(), organization, accessToken)).thenReturn(encodedPagati);
    when(receiptServiceMock.getReceiptById(firstPair.getRight().getReceiptId(), organization.getOrganizationId(), accessToken)).thenReturn(encodedRt);

    PaaSILChiediPagatiConRicevutaRisposta result = paaSILChiediPagatiConRicevutaService.processRequest(request, userInfo, accessToken);

    assertNotNull(result);
    assertNull(result.getFault());
    assertNotNull(result.getPagati());
    assertInstanceOf(ByteArrayDataSource.class, result.getPagati().getDataSource());
    assertInstanceOf(ByteArrayInputStream.class, result.getPagati().getDataSource().getInputStream());
    assertArrayEquals(encodedPagati, ((ByteArrayInputStream) result.getPagati().getDataSource().getInputStream()).readAllBytes());
    assertArrayEquals(encodedRt, ((ByteArrayInputStream) result.getRt().getDataSource().getInputStream()).readAllBytes());
  }

  @ParameterizedTest
  @CsvSource(value = {
    "userNotAuth,PAA_ENTE_NON_VALIDO,Utente non autorizzato",
    "invalidOrgStatus,PAA_ENTE_NON_VALIDO,L'ente non è valido o non è abilitato",
    "noSearchCriteria,PAA_SYSTEM_ERROR,'Errore, è obbligatorio specificare esattamente un parametro tra idSession, identificativoUnivocoVersamento e identificativoUnivocoDovuto.'",
    "multipleSearchCriteria,PAA_SYSTEM_ERROR,'Errore, è obbligatorio specificare esattamente un parametro tra idSession, identificativoUnivocoVersamento e identificativoUnivocoDovuto.'",
    "emptyDebtPositionList,PAA_ID_SESSION_NON_VALIDO,Nessuna posizione debitoria trovata",
    "emptyDebtPositionListIud,PAA_IUD_NON_VALIDO,Nessuna posizione debitoria trovata",
    "emptyDebtPositionListIuv,PAA_IUV_NON_VALIDO,Nessuna posizione debitoria trovata",
    "invalidOrgDebtPosition,PAA_ID_SESSION_NON_VALIDO,Posizione debitoria non trovata",
    "invalidOrgDebtPositionIud,PAA_IUD_NON_VALIDO,Posizione debitoria non trovata",
    "unpaidInstallment,PAA_PAGAMENTO_NON_INIZIATO,Pagamento non effettuato",
    "unpaidToSyncInstallment,PAA_PAGAMENTO_NON_INIZIATO,Pagamento non effettuato",
    "expiredInstallment,PAA_PAGAMENTO_SCADUTO,Pagamento scaduto",
    "invalidStatusInstallment,PAA_DOVUTO_NON_PAGABILE,Dovuto non pagabile"
  }, nullValues = {"null"})
  void testGetDebtPositionsAndInstallmentsFault(String testCase, String silFaultCode, String faultDescription) {

    request.setIdSession(sessionId);

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
      case "noSearchCriteria":
        request.setIdSession(null);
        break;
      case "multipleSearchCriteria":
        request.setIdentificativoUnivocoDovuto(pairList.getFirst().getRight().getIud());
        break;
      case "emptyDebtPositionList":
        installmentIds = List.of();
        break;
      case "emptyDebtPositionListIud":
        request.setIdSession(null);
        request.setIdentificativoUnivocoDovuto(pairList.getFirst().getRight().getIud());
        break;
      case "emptyDebtPositionListIuv":
        request.setIdSession(null);
        request.setIdentificativoUnivocoVersamento(pairList.getFirst().getRight().getIuv());
        break;
      case "invalidOrgDebtPosition":
        pairList.getFirst().getLeft().setOrganizationId(organization.getOrganizationId() + 1);
        break;
      case "invalidOrgDebtPositionIud":
        request.setIdSession(null);
        request.setIdentificativoUnivocoDovuto(pairList.getFirst().getRight().getIud());
        pairList.getFirst().getLeft().setOrganizationId(organization.getOrganizationId() + 1);
        break;
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

    // mock only used methods of testCase
    switch (testCase) {
      case "invalidStatusInstallment", "unpaidInstallment",
           "unpaidToSyncInstallment", "expiredInstallment",
           "invalidOrgDebtPosition":
        pairList.forEach(pair ->
          when(debtPositionServiceMock.getDebtPositionByInstallmentId(pair.getRight().getInstallmentId(), accessToken)).thenReturn(pair.getLeft())
        );
      case "emptyDebtPositionList":
        when(sessionIdMapperMock.mapSessionIdToInstallmentIds(sessionId)).thenReturn(installmentIds);
      case "invalidOrgDebtPositionIud", "emptyDebtPositionListIud",
           "emptyDebtPositionListIuv", "invalidOrgStatus", "noSearchCriteria",
           "multipleSearchCriteria":
        if ("invalidOrgDebtPositionIud".equals(testCase)) {
          when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIud(organization.getOrganizationId(), pairList.getFirst().getRight().getIud(), ALLOWED_ORIGINS, accessToken))
            .thenReturn(List.of(pairList.getFirst().getLeft()));
        }
        when(organizationServiceMock.getOrganizationById(organization.getOrganizationId(), accessToken)).thenReturn(Optional.of(organization));
      case "userNotAuth":
      default:
        //do nothing
    }

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> paaSILChiediPagatiConRicevutaService.processRequest(request, userInfo, accessToken));

    assertNotNull(result);
    assertNotNull(result.getFault());
    assertEquals(silFaultCode, result.getFault().code());
    assertEquals(faultDescription, result.getDescription());
  }

}
