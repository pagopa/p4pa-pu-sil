package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.dto.ManageDebtPositionWithIudDTO;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.RegistryLoggerTest;
import it.gov.pagopa.pu.sil.security.SecurityUtilsTest;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.singleimport.DebtPositionCreationService;
import it.gov.pagopa.pu.sil.service.singleimport.DebtPositionInstallmentsHandlerService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.co.jemos.podam.api.PodamFactory;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebtPositionControllerTest {
  @Mock
  private RegistryLogger registryLoggerMock;
  @Mock
  private DebtPositionCreationService debtPositionCreationServiceMock;
  @Mock
  private DebtPositionInstallmentsHandlerService debtPositionInstallmentsHandlerServiceMock;

  @InjectMocks
  private DebtPositionController controller;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();
  private final String accessToken = "fakeAccessToken";
  private final String orgFiscalCode = "ORG123456789";
  private final String orgIpaCode = "userIpaCode";
  private UserInfo userInfo;

  @BeforeEach
  void setUp() {
    userInfo = AuthorizationServiceTest.buildAdminUser(1L, orgFiscalCode, orgIpaCode);
    userInfo.setMappedExternalUserId("fakeExternalUser");
    SecurityUtilsTest.configureSecurityContext(accessToken, userInfo);
  }

  @AfterEach
  void clear() { SecurityUtilsTest.clearSecurityContext(); }

  @Test
  void whenCreateDebtPositionThenOk() {
    // Given
    it.gov.pagopa.pu.sil.dto.generated.DebtPositionDTO requestDebtPosition = podamFactory.manufacturePojo(it.gov.pagopa.pu.sil.dto.generated.DebtPositionDTO.class);
    DebtPositionDTO expectedDebtPositionCreated = podamFactory.manufacturePojo(DebtPositionDTO.class);
    String iuv = expectedDebtPositionCreated.getPaymentOptions().getFirst().getInstallments().getFirst().getIuv();
    Triple<DebtPositionDTO, String, RegistryOutcome> outcomeTriple = Triple.of(expectedDebtPositionCreated, iuv, RegistryOutcome.OK);
    RegistryContextData expectedContextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.PTDP_paaSILImportaDovuto)
      .loggedUser(userInfo)
      .build();
    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, expectedContextData, requestDebtPosition, false, false);

    when(debtPositionCreationServiceMock.handleAction(
      requestDebtPosition,
      orgIpaCode,
      userInfo,
      accessToken)).thenReturn(outcomeTriple);

    // When
    ResponseEntity<DebtPositionDTO> response = controller.createSingleDebtPosition(orgFiscalCode, requestDebtPosition);

    // Then
    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertSame(outcomeTriple.getLeft(), response.getBody());
  }

  @Test
  void whenManageDebtPositionInstallmentsThenOk() {
    // Given
    ManageDebtPositionWithIudDTO requestManageDebtPositionDTO = podamFactory.manufacturePojo(ManageDebtPositionWithIudDTO.class);
    String iud = requestManageDebtPositionDTO.getInstallments().getFirst().getInstallment().getIud();
    String iuv = requestManageDebtPositionDTO.getInstallments().getFirst().getInstallment().getIuv();
    requestManageDebtPositionDTO.setIud(iud);
    DebtPositionDTO expectedDebtPosition = podamFactory.manufacturePojo(DebtPositionDTO.class);

    Triple<DebtPositionDTO, String, RegistryOutcome> outcomeTriple = Triple.of(expectedDebtPosition, iuv, RegistryOutcome.OK);

    RegistryContextData expectedContextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.PTDP_paaSILImportaDovuto)
      .loggedUser(userInfo)
      .iuv(iuv)
      .build();
    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, expectedContextData, requestManageDebtPositionDTO, false, false);

    when(debtPositionInstallmentsHandlerServiceMock.handleAction(
      requestManageDebtPositionDTO,
      orgIpaCode,
      userInfo,
      accessToken)).thenReturn(outcomeTriple);

    // When
    ResponseEntity<DebtPositionDTO> response = controller.manageDebtPositionInstallments(orgFiscalCode, iud, requestManageDebtPositionDTO);

    // Then
    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertSame(outcomeTriple.getLeft(), response.getBody());
  }
}
