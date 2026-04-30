package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionInstallmentFacadeService;
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
import org.springframework.security.authorization.AuthorizationDeniedException;
import uk.co.jemos.podam.api.PodamFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.pu.sil.dto.generated.QueryPaymentStatusType.INSTALLMENT_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaaSILChiediEsitoCarrelloDovutiServiceTest {

  @InjectMocks
  private PaaSILChiediEsitoCarrelloDovutiService paaSILChiediEsitoCarrelloDovutiService;

  @Mock
  private DebtPositionInstallmentFacadeService debtPositionInstallmentFacadeServiceMock;
  @Mock
  private PagatiMapper pagatiMapperMock;
  @Mock
  private OrganizationService organizationServiceMock;
  @Mock
  private ReceiptService receiptServiceMock;

  private final PodamFactory podamFactory;

  private String accessToken;
  private String sessionId;
  private UserInfo userInfo;
  private Organization org;
  private PaaSILChiediEsitoCarrelloDovuti request;
  private List<Long> installmentIds;
  private List<Pair<DebtPositionDTO, InstallmentDTO>> pairList;
  private PaymentStatusRequest transformedRequest;

  PaaSILChiediEsitoCarrelloDovutiServiceTest() {
    this.podamFactory = TestUtils.getPodamFactory();
    podamFactory.getStrategy().setDefaultNumberOfCollectionElements(2);
  }

  @BeforeEach
  void init() {
    accessToken = "accessToken";
    sessionId = "sessionId";
    org = podamFactory.manufacturePojo(Organization.class);
    org.setStatus(OrganizationStatus.ACTIVE);
    userInfo = AuthorizationServiceTest.buildAdminUser(org.getOrganizationId(), org.getOrgFiscalCode(), org.getIpaCode());
    request = podamFactory.manufacturePojo(PaaSILChiediEsitoCarrelloDovuti.class);
    request.setCodIpaEnte(org.getIpaCode());
    request.setIdSessionCarrello(sessionId);

    installmentIds = List.of(1L, 2L);
    pairList = installmentIds.stream().map(i -> {
      DebtPositionDTO dp = podamFactory.manufacturePojo(DebtPositionDTO.class);
      dp.setOrganizationId(org.getOrganizationId());
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
    transformedRequest = new PaymentStatusRequest(request.getCodIpaEnte(), INSTALLMENT_ID, request.getIdSessionCarrello(), false);

    when(organizationServiceMock.getOrganizationById(org.getOrganizationId(), accessToken)).thenReturn(Optional.of(org));
    when(debtPositionInstallmentFacadeServiceMock.fetch(transformedRequest, org, accessToken))
      .thenReturn(pairList);
    if(testCase.equals("valid")) {
      Pair<DebtPositionDTO, InstallmentDTO> firstPair = pairList.getFirst();
      when(pagatiMapperMock.mapDebtPositionsToEncodedPagatiConRicevuta(firstPair.getRight(), org, accessToken)).thenReturn(encodedPagati);
      when(receiptServiceMock.getReceiptById(firstPair.getRight().getReceiptId(), org.getOrganizationId(), accessToken)).thenReturn(encodedRt);
    }

    PaaSILChiediEsitoCarrelloDovutiRisposta result = paaSILChiediEsitoCarrelloDovutiService.processRequest(request, userInfo, accessToken);

    assertNotNull(result);
    assertNull(result.getFault());
    assertNotNull(result.getListaCarrelli());
    assertEquals(2, result.getListaCarrelli().getRispostaCarrellos().size());
    RispostaCarrello cartResponse = result.getListaCarrelli().getRispostaCarrellos().getFirst();
    assertEquals(org.getIpaCode(), cartResponse.getCodIpaEnte());
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
    "userNotAuth,PAA_ENTE_NON_VALIDO,null",
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
        org.setStatus(OrganizationStatus.DRAFT);
        break;
      case "emptyDebtPositionList":
        request.setIdSessionCarrello(null);
        installmentIds = List.of();
        break;
      case "invalidOrgDebtPosition":
        pairList.getFirst().getLeft().setOrganizationId(org.getOrganizationId() + 1);
        break;
      default:
        //nothing to do
    }
    transformedRequest = new PaymentStatusRequest(request.getCodIpaEnte(), INSTALLMENT_ID, request.getIdSessionCarrello(), false);

    // mock only used methods of testCase
    switch (testCase) {
      case "invalidOrgStatus":
        when(organizationServiceMock.getOrganizationById(org.getOrganizationId(), accessToken)).thenReturn(Optional.of(org));
        break;
      case "emptyDebtPositionList":
        when(organizationServiceMock.getOrganizationById(org.getOrganizationId(), accessToken)).thenReturn(Optional.of(org));
        when(debtPositionInstallmentFacadeServiceMock.fetch(transformedRequest, org, accessToken))
          .thenReturn(List.of());
        break;
      case "invalidOrgDebtPosition":
        when(organizationServiceMock.getOrganizationById(org.getOrganizationId(), accessToken)).thenReturn(Optional.of(org));
        when(debtPositionInstallmentFacadeServiceMock.fetch(transformedRequest, org, accessToken))
          .thenReturn(pairList);
        break;
      case "userNotAuth":
      default:
        //do nothing
    }

    if(testCase.equals("userNotAuth")) {
      AuthorizationDeniedException authorizationDeniedException = Assertions.assertThrows(AuthorizationDeniedException.class, () -> paaSILChiediEsitoCarrelloDovutiService.processRequest(request, userInfo, accessToken));

      assertNotNull(authorizationDeniedException);
      assertTrue(authorizationDeniedException.getMessage().contains("Access denied on orgIpaCode "));
    } else {
      SilFaultException silFaultException = Assertions.assertThrows(SilFaultException.class, () -> paaSILChiediEsitoCarrelloDovutiService.processRequest(request, userInfo, accessToken));

      assertNotNull(silFaultException);
      assertNotNull(silFaultException.getFault());
      assertEquals(silFaultCode, silFaultException.getFault().code());
      assertEquals(faultDescription, silFaultException.getDescription());
    }
  }

}
