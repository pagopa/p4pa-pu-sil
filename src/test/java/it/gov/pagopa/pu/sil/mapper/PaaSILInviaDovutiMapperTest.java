package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtSoggettoPagatore;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaaSILInviaDovutiMapperTest {

  @InjectMocks
  private PaaSILInviaDovutiMapper mapper;

  @Mock
  private JAXBTransformService jaxbTransformServiceMock;

  @Mock
  private OrganizationService organizationServiceMock;

  @Mock
  private DebtPositionService debtPositionServiceMock;

  @Mock
  private PersonMapper personMapperMock;

  @Mock
  private ValidationService validationServiceMock;

  UserInfo userInfo;
  Organization org;

  private static final String ACCESS_TOKEN = "token";

  private static final String ORG_IPA_CODE = "ORG_IPA";

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setup() {
    userInfo = podamFactory.manufacturePojo(UserInfo.class);
    userInfo.getOrganizations().getFirst().setOrganizationIpaCode(ORG_IPA_CODE);
    userInfo.getOrganizations().getFirst().setRoles(List.of("ROLE_X", "ROLE_ADMIN"));
    org = podamFactory.manufacturePojo(Organization.class);
    org.setIpaCode(ORG_IPA_CODE);
    org.setStatus(OrganizationStatus.ACTIVE);
  }

  @Test
  void mapRequestToDebtPositionsOrFault_UnmarshallingFailure_ReturnsError() {
    PaaSILInviaDovuti request = new PaaSILInviaDovuti();
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenThrow(new ApplicationException("Error"));
    when(organizationServiceMock.getOrganizationById(anyLong(), anyString())).thenReturn(Optional.ofNullable(org));

    Triple<List<DebtPositionDTO>, SilFaults, String> result = mapper.mapRequestToDebtPositionsOrFault(request, "CART", userInfo, ORG_IPA_CODE, ACCESS_TOKEN);

    assertNotNull(result);
    assertNull(result.getLeft());
    assertEquals(SilFaults.PAA_XML_NON_VALIDO, result.getMiddle());
    assertTrue(result.getRight().contains("XML non conforme"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"not_active_ipa"})
  @NullSource
  void mapRequestToDebtPositionsOrFault_InvalidOrganization_ReturnsError(String orgIpaCode) {
    PaaSILInviaDovuti request = new PaaSILInviaDovuti();
    if(orgIpaCode==null){
      org = null;
    } else {
      org.setStatus(OrganizationStatus.DRAFT);
    }

    when(organizationServiceMock.getOrganizationById(anyLong(), anyString())).thenReturn(Optional.ofNullable(org));

    Triple<List<DebtPositionDTO>, SilFaults, String> result = mapper.mapRequestToDebtPositionsOrFault(request, "CART", userInfo, ORG_IPA_CODE, ACCESS_TOKEN);

    assertNotNull(result);
    assertNull(result.getLeft());
    assertEquals(SilFaults.PAA_ENTE_NON_VALIDO, result.getMiddle());
    assertEquals("L'ente non è valido o non è abilitato", result.getRight());
  }

  @Test
  void mapRequestToDebtPositionsOrFault_InvalidIUV_ReturnsError() {
    PaaSILInviaDovuti request = new PaaSILInviaDovuti();
    Dovuti dovuti = podamFactory.manufacturePojo(Dovuti.class);
    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento("IUV");
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);
    when(organizationServiceMock.getOrganizationById(anyLong(), anyString())).thenReturn(Optional.of(org));

    Triple<List<DebtPositionDTO>, SilFaults, String> result = mapper.mapRequestToDebtPositionsOrFault(request, "CART", userInfo, ORG_IPA_CODE, ACCESS_TOKEN);

    assertNotNull(result);
    assertNull(result.getLeft());
    assertEquals(SilFaults.PAA_IUV_NON_VALIDO, result.getMiddle());
    assertEquals("L'inserimento dello IUV è deprecato", result.getRight());
  }

  @Test
  void mapRequestToDebtPositionsOrFault_InvalidDebtor_ReturnsError() {
    PaaSILInviaDovuti request = new PaaSILInviaDovuti();
    Dovuti dovuti = podamFactory.manufacturePojo(Dovuti.class);
    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento(null);
    dovuti.setSoggettoPagatore(new CtSoggettoPagatore());
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);
    when(organizationServiceMock.getOrganizationById(anyLong(), anyString())).thenReturn(Optional.of(org));
    when(personMapperMock.getAndValidateDebtor(any())).thenReturn(Triple.of(null, SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "Error"));

    Triple<List<DebtPositionDTO>, SilFaults, String> result = mapper.mapRequestToDebtPositionsOrFault(request, "CART", userInfo, ORG_IPA_CODE, ACCESS_TOKEN);

    assertNotNull(result);
    assertNull(result.getLeft());
    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, result.getMiddle());
    assertEquals("Error", result.getRight());
  }

  @Test
  void mapRequestToDebtPositionsOrFault_ValidFlow_ReturnsDebtPositions() {
    PaaSILInviaDovuti request = new PaaSILInviaDovuti();
    Dovuti dovuti = podamFactory.manufacturePojo(Dovuti.class);
    dovuti.setSoggettoPagatore(new CtSoggettoPagatore());
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    versamento.setCausaleVersamento("causale");
    versamento.setImportoSingoloVersamento(BigDecimal.TWO);
    versamento.setDatiSpecificiRiscossione("9/DATI_SPECIFICI");
    versamento.setIdentificativoTipoDovuto("COD_TIPO_DOVUTO");
    versamento.setIdentificativoUnivocoDovuto("IUD");
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().clear();
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().add(versamento);
    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento(null);
    PersonDTO debtor = podamFactory.manufacturePojo(PersonDTO.class);
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);
    when(organizationServiceMock.getOrganizationById(anyLong(), anyString())).thenReturn(Optional.of(org));
    when(personMapperMock.getAndValidateDebtor(any())).thenReturn(Triple.of(debtor, null, null));
    when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(
      org.getOrganizationId(), versamento.getIdentificativoTipoDovuto(), ACCESS_TOKEN)).thenReturn(podamFactory.manufacturePojo(DebtPositionTypeOrg.class));
    Triple<List<DebtPositionDTO>, SilFaults, String> result = mapper.mapRequestToDebtPositionsOrFault(request, "CART", userInfo, ORG_IPA_CODE, ACCESS_TOKEN);

    assertNotNull(result);
    assertNotNull(result.getLeft());
    assertNull(result.getMiddle());
    assertNull(result.getRight());
    assertEquals(1, result.getLeft().size());

    result.getLeft().forEach(dp -> {
      TestUtils.checkNotNullFields(dp,
        "debtPositionId", "validityDate", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
      dp.getPaymentOptions().forEach(po -> {
        TestUtils.checkNotNullFields(po, "paymentOptionId", "debtPositionId", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
        po.getInstallments().forEach(i -> {
          TestUtils.checkNotNullFields(i, "installmentId", "paymentOptionId", "syncStatus", "iupdPagopa",
            "iuv", "iur", "iuf", "nav", "iun", "notificationFeeCents", "transfers", "notificationDate", "ingestionFlowFileId",
            "ingestionFlowFileAction", "ingestionFlowFileLineNumber", "receiptId",
            "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
          if(i.getTransfers()!=null) {
            i.getTransfers().forEach(t -> {
              TestUtils.checkNotNullFields(t, "transferId", "installmentId",
                "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
            });
          }
        });
      });
    });

  }
}

