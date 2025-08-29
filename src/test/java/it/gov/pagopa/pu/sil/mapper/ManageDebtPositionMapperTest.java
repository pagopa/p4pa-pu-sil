package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.ManageDebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.TestUtils;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class ManageDebtPositionMapperTest {

  @InjectMocks
  private ManageDebtPositionMapper mapper;

  @Mock
  private SecondaryTransferMapper secondaryTransferMapperMock;

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

  //region: mapRequestToDebtPositionList
  @ParameterizedTest
  @CsvSource({"M, true", "M, false", "A, true", "A, false"})
  void mapToManageDebtPositionDTO_validData_ReturnsOk(String action, boolean legacyMode) {
    DebtPositionDTO debtPositionOnDb = podamFactory.manufacturePojo(DebtPositionDTO.class);
    InstallmentDTO installmentToSync = podamFactory.manufacturePojo(InstallmentDTO.class);
    InstallmentDTO installmentOnDb = debtPositionOnDb.getPaymentOptions().getLast().getInstallments().getLast();
    installmentOnDb.setIud(installmentToSync.getIud());
    debtPositionOnDb.getPaymentOptions().getLast().setPaymentOptionId(installmentOnDb.getPaymentOptionId());

    if(action.equals(Constants.LEGACY_IMPORT_ACTION_MODIFY)){
      doNothing().when(secondaryTransferMapperMock).checkAndFillSupportedTransfersConfigurationForModify(installmentOnDb, installmentToSync, legacyMode);
    }

    ManageDebtPositionDTO response = mapper.mapToManageDebtPositionDTO(debtPositionOnDb, installmentToSync, action, legacyMode);

    Assertions.assertNotNull(response);
    TestUtils.checkNotNullFields(response);
  }

  @Test
  void mapToManageDebtPositionDTO_UnmarshallingFailure_ReturnsError() {
    DebtPositionDTO debtPositionOnDb = podamFactory.manufacturePojo(DebtPositionDTO.class);
    InstallmentDTO installmentToSync = podamFactory.manufacturePojo(InstallmentDTO.class);
    InstallmentDTO installmentOnDb = debtPositionOnDb.getPaymentOptions().getLast().getInstallments().getLast();
    installmentOnDb.setIud(installmentToSync.getIud()+"invalid");

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> mapper.mapToManageDebtPositionDTO(debtPositionOnDb, installmentToSync, Constants.LEGACY_IMPORT_ACTION_MODIFY, false));

    assertEquals(SilFaults.PAA_IMPORT_DOVUTO_NON_PRESENTE, exception.getFault());
    assertTrue(exception.getDescription().contains("Dovuto non trovato"));
  }
  //endregion


}

