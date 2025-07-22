package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovuto;
import it.veneto.regione.schemas._2012.pagamenti.ente.Bilancio;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiVersamentoDovutiEntiSecondari;
import it.veneto.regione.schemas._2012.pagamenti.ente.Versamento;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaaSILImportaDovutoMapperTest {

  @InjectMocks
  private PaaSILImportaDovutoMapper mapper;

  @Mock
  private DebtPositionTypeService debtPositionTypeServiceMock;
  @Mock
  private JAXBTransformService jaxbTransformServiceMock;
  @Mock
  private PersonMapper personMapperMock;
  @Mock
  private SecondaryTransferMapper secondaryTransferMapperMock;
  @Mock
  private ValidationService immediatePaymentsValidationServiceMock;

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

  //region: mapRequestToDebtPosition
  @Test
  void mapRequestToDebtPositionPair_UnmarshallingFailure_ReturnsError() {
    PaaSILImportaDovuto request = new PaaSILImportaDovuto();
    when(jaxbTransformServiceMock.unmarshalling(eq(request.getDovuto()), eq(Versamento.class), any())).thenThrow(new ApplicationException("Error"));

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> mapper.mapRequestToDebtPosition(request, org, ACCESS_TOKEN));

    assertEquals(SilFaults.PAA_XML_NON_VALIDO, exception.getFault());
    assertTrue(exception.getDescription().contains("XML non conforme"));
  }

  @Test
  void mapRequestToDebtPositionPair_BilancioUnmarshallingFailure_ReturnsError() {
    PaaSILImportaDovuto request = new PaaSILImportaDovuto();
    Versamento versamento = podamFactory.manufacturePojo(Versamento.class);
    when(jaxbTransformServiceMock.unmarshalling(eq(request.getDovuto()), eq(Versamento.class), any())).thenReturn(versamento);
    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(org.getOrganizationId(), versamento.getDatiVersamento().getIdentificativoTipoDovuto(), ACCESS_TOKEN))
      .thenReturn(podamFactory.manufacturePojo(DebtPositionTypeOrg.class));
    when(personMapperMock.getAndValidateDebtor(versamento.getSoggettoPagatore())).thenReturn(podamFactory.manufacturePojo(PersonDTO.class));
    when(jaxbTransformServiceMock.marshallingNoNamespace(versamento.getDatiVersamento().getBilancio(), Bilancio.class)).thenThrow(new RuntimeException("simulated error"));

    ApplicationException exception = Assertions.assertThrows(ApplicationException.class, () -> mapper.mapRequestToDebtPosition(request, org, ACCESS_TOKEN));

    assertTrue(exception.getMessage().startsWith("Invalid bilancio format"));
  }

  @Test
  void mapRequestToDebtPositionPair_validData_ReturnsOk() {
    PaaSILImportaDovuto request = podamFactory.manufacturePojo(PaaSILImportaDovuto.class);
    Versamento versamento = podamFactory.manufacturePojo(Versamento.class);
    when(jaxbTransformServiceMock.unmarshalling(eq(request.getDovuto()), eq(Versamento.class), any())).thenReturn(versamento);
    DebtPositionTypeOrg debtPositionTypeOrg = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);
    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(org.getOrganizationId(), versamento.getDatiVersamento().getIdentificativoTipoDovuto(), ACCESS_TOKEN))
      .thenReturn(debtPositionTypeOrg);
    when(personMapperMock.getAndValidateDebtor(versamento.getSoggettoPagatore())).thenReturn(podamFactory.manufacturePojo(PersonDTO.class));
    when(jaxbTransformServiceMock.marshallingNoNamespace(versamento.getDatiVersamento().getBilancio(), Bilancio.class)).thenReturn("bilancioString");
    CtDatiVersamentoDovutiEntiSecondari secondaryTransferData = podamFactory.manufacturePojo(CtDatiVersamentoDovutiEntiSecondari.class);
    when(secondaryTransferMapperMock.mapToCtDatiVersamentoDovutiEntiSecondari(request.getListaDovutiEntiSecondari())).thenReturn(Optional.of(secondaryTransferData));
    doNothing().when(immediatePaymentsValidationServiceMock).validateSecondaryDebtPositionData(secondaryTransferData, 1);
    doNothing().when(secondaryTransferMapperMock).fillSecondaryTransferData(any(), eq(secondaryTransferData), eq(debtPositionTypeOrg.getCode()), eq(ACCESS_TOKEN));


    Pair<DebtPositionDTO, String> response = mapper.mapRequestToDebtPosition(request, org, ACCESS_TOKEN);

    assertNotNull(response);
    assertEquals(versamento.getAzione(), response.getRight());

    DebtPositionDTO dp = response.getLeft();
    TestUtils.checkAllNotNullFields(dp,
      "debtPositionId", "validityDate", "iupdOrg", "description", "multiDebtor", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
    dp.getPaymentOptions().forEach(po -> {
      TestUtils.checkAllNotNullFields(po, "paymentOptionId", "debtPositionId", "description", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
      po.getInstallments().forEach(i -> {
        TestUtils.checkAllNotNullFields(i, "installmentId", "paymentOptionId", "syncStatus", "iupdPagopa",
          "iuv", "iur", "iuf", "nav", "iun", "notificationFeeCents", "transfers", "notificationDate", "ingestionFlowFileId",
          "ingestionFlowFileAction", "ingestionFlowFileLineNumber", "receiptId", "sourceFlowName", "switchToExpired",
          "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
        if (i.getTransfers() != null) {
          i.getTransfers().forEach(t -> {
            TestUtils.checkAllNotNullFields(t, "transferId", "installmentId",
              "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
          });
        }
      });
    });

  }
  //endregion

  //region: mapRequestToDebtPositionList
  @ParameterizedTest
  @ValueSource(strings = {Constants.LEGACY_IMPORT_ACTION_CANCEL, Constants.LEGACY_IMPORT_ACTION_MODIFY})
  void mapToManageDebtPositionDTO_validData_ReturnsOk(String action) {
    DebtPositionDTO debtPositionOnDb = podamFactory.manufacturePojo(DebtPositionDTO.class);
    InstallmentDTO installmentToSync = podamFactory.manufacturePojo(InstallmentDTO.class);
    InstallmentDTO installmentOnDb = debtPositionOnDb.getPaymentOptions().getLast().getInstallments().getLast();
    installmentOnDb.setIud(installmentToSync.getIud());
    debtPositionOnDb.getPaymentOptions().getLast().setPaymentOptionId(installmentToSync.getPaymentOptionId());

    if(action.equals(Constants.LEGACY_IMPORT_ACTION_MODIFY)){
      doNothing().when(secondaryTransferMapperMock).checkAndFillSupportedTransfersConfigurationForModify(installmentOnDb, installmentToSync);
    }

    ManageDebtPositionDTO response = mapper.mapToManageDebtPositionDTO(debtPositionOnDb, installmentToSync, action);

    Assertions.assertNotNull(response);
    TestUtils.checkNotNullFields(response);
  }

  @Test
  void mapToManageDebtPositionDTO_UnmarshallingFailure_ReturnsError() {
    DebtPositionDTO debtPositionOnDb = podamFactory.manufacturePojo(DebtPositionDTO.class);
    InstallmentDTO installmentToSync = podamFactory.manufacturePojo(InstallmentDTO.class);
    InstallmentDTO installmentOnDb = debtPositionOnDb.getPaymentOptions().getLast().getInstallments().getLast();
    installmentOnDb.setIud(installmentToSync.getIud()+"invalid");

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> mapper.mapToManageDebtPositionDTO(debtPositionOnDb, installmentToSync, Constants.LEGACY_IMPORT_ACTION_MODIFY));

    assertEquals(SilFaults.PAA_IMPORT_DOVUTO_NON_PRESENTE, exception.getFault());
    assertTrue(exception.getDescription().contains("Dovuto non trovato"));
  }
  //endregion


}

