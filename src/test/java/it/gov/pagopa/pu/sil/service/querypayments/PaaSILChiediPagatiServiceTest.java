package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentSyncStatus;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPagati;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPagatiRisposta;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class PaaSILChiediPagatiServiceTest {

  @InjectMocks
  private PaaSILChiediPagatiService paaSILChiediPagatiService;

  @Mock
  private DebtPositionService debtPositionServiceMock;
  @Mock
  private PagatiMapper pagatiMapperMock;
  @Mock
  private SessionIdMapper sessionIdMapperMock;
  @Mock
  private OrganizationService organizationServiceMock;

  private final PodamFactory podamFactory;

  private String accessToken;
  private String sessionId;
  private UserInfo userInfo;
  private Organization organization;
  private PaaSILChiediPagati request;
  private List<Long> installmentIds;
  private List<Pair<DebtPositionDTO, InstallmentDTO>> pairList;

  PaaSILChiediPagatiServiceTest() {
    this.podamFactory = TestUtils.getPodamFactory();
    podamFactory.getStrategy().setDefaultNumberOfCollectionElements(2);
  }

  @BeforeEach
  void init() {
    accessToken = "accessToken";
    sessionId = "sessionId";
    userInfo = podamFactory.manufacturePojo(UserInfo.class);
    request = podamFactory.manufacturePojo(PaaSILChiediPagati.class);
    organization = podamFactory.manufacturePojo(Organization.class);
    organization.setStatus(OrganizationStatus.ACTIVE);
    UserOrganizationRoles uor = userInfo.getOrganizations().getFirst();
    uor.setOrganizationId(organization.getOrganizationId());
    uor.setOrganizationIpaCode(organization.getIpaCode());
    uor.setRoles(List.of(AuthorizationService.ROLE_ADMIN));
    request.setCodIpaEnte(organization.getIpaCode());
    request.setIdSession(sessionId);

    installmentIds = List.of(1L, 2L);
    pairList = installmentIds.stream().map(i -> {
      DebtPositionDTO dp = podamFactory.manufacturePojo(DebtPositionDTO.class);
      dp.setOrganizationId(organization.getOrganizationId());
      InstallmentDTO inst = dp.getPaymentOptions().getFirst().getInstallments().getFirst();
      inst.setInstallmentId(i);
      inst.setStatus(InstallmentStatus.PAID);
      return Pair.of(dp, inst);
    }).toList();
  }


  @Test
  void testGetDebtPositionsAndInstallmentsOk() throws IOException {
    byte[] encodedPagati = "encodedPagati".getBytes(StandardCharsets.UTF_8);

    when(organizationServiceMock.getOrganizationById(organization.getOrganizationId(), accessToken)).thenReturn(Optional.of(organization));
    when(sessionIdMapperMock.mapSessionIdToInstallmentIds(sessionId)).thenReturn(installmentIds);
    pairList.forEach(pair ->
      when(debtPositionServiceMock.getDebtPositionByInstallmentId(pair.getRight().getInstallmentId(), accessToken)).thenReturn(pair.getLeft())
    );
    //TODO currently support only one debt position and installment, but could be extended to support multiple
    Pair<DebtPositionDTO, InstallmentDTO> firstPair = pairList.getFirst();
    when(pagatiMapperMock.mapDebtPositionsToEncodedPagati(firstPair.getLeft(), firstPair.getRight(), organization, accessToken)).thenReturn(encodedPagati);

    PaaSILChiediPagatiRisposta result = paaSILChiediPagatiService.processRequest(request, userInfo, accessToken);

    assertNotNull(result);
    assertNull(result.getFault());
    assertNotNull(result.getPagati());
    assertInstanceOf(ByteArrayDataSource.class, result.getPagati().getDataSource());
    assertInstanceOf(ByteArrayInputStream.class, result.getPagati().getDataSource().getInputStream());
    assertArrayEquals(encodedPagati, ((ByteArrayInputStream) result.getPagati().getDataSource().getInputStream()).readAllBytes());
  }

  @ParameterizedTest
  @CsvSource(value = {
    "userNotAuth,PAA_ENTE_NON_VALIDO,Utente non autorizzato",
    "invalidOrgStatus,PAA_ENTE_NON_VALIDO,L'ente non è valido o non è abilitato",
    "emptyDebtPositionList,PAA_ID_SESSION_NON_VALIDO,Nessuna posizione debitoria trovata",
    "invalidOrgDebtPosition,PAA_ID_SESSION_NON_VALIDO,Posizione debitoria non trovata",
    "unpaidInstallment,PAA_PAGAMENTO_NON_INIZIATO,Pagamento non effettuato",
    "unpaidToSyncInstallment,PAA_PAGAMENTO_NON_INIZIATO,Pagamento non effettuato",
    "expiredInstallment,PAA_PAGAMENTO_SCADUTO,Pagamento scaduto",
    "invalidStatusInstallment,PAA_DOVUTO_NON_PAGABILE,Dovuto non pagabile"
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
      case "invalidStatusInstallment", "unpaidInstallment", "unpaidToSyncInstallment", "expiredInstallment", "invalidOrgDebtPosition":
        pairList.forEach(pair ->
          when(debtPositionServiceMock.getDebtPositionByInstallmentId(pair.getRight().getInstallmentId(), accessToken)).thenReturn(pair.getLeft())
        );
      case "emptyDebtPositionList":
        when(sessionIdMapperMock.mapSessionIdToInstallmentIds(sessionId)).thenReturn(installmentIds);
      case "invalidOrgStatus":
        when(organizationServiceMock.getOrganizationById(organization.getOrganizationId(), accessToken)).thenReturn(Optional.of(organization));
      case "userNotAuth":
      default:
        //do nothing
    }

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> paaSILChiediPagatiService.processRequest(request, userInfo, accessToken));

    assertNotNull(result);
    assertNotNull(result.getFault());
    assertEquals(silFaultCode, result.getFault().code());
    assertEquals(faultDescription, result.getDescription());
  }

}
