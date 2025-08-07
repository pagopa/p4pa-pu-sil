package it.gov.pagopa.pu.sil.service.singleimport;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.mapper.DebtPositionMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.debtposition.ManageDebtPositionService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebtPositionCreationServiceTest {

  @InjectMocks
  private DebtPositionCreationService service;

  @Mock
  private OrganizationService organizationServiceMock;
  @Mock
  private DebtPositionMapper debtPositionMapperMock;
  @Mock
  private ManageDebtPositionService manageDebtPositionServiceMock;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();
  private static final String TOKEN = "ACCESS_TOKEN";
  private static final String IPA_CODE = "IPA_CODE";

  private UserInfo userInfo;
  private Organization org;
  private it.gov.pagopa.pu.sil.dto.generated.DebtPositionDTO request;

  @BeforeEach
  void init() {
    org = podamFactory.manufacturePojo(Organization.class);
    org.setStatus(OrganizationStatus.ACTIVE);
    org.setIpaCode(IPA_CODE);
    userInfo = AuthorizationServiceTest.buildAdminUser(org.getOrganizationId(), org.getOrgFiscalCode(), org.getIpaCode());
    request = podamFactory.manufacturePojo(it.gov.pagopa.pu.sil.dto.generated.DebtPositionDTO.class);
  }

  @Test
  void givenNotAuthorizedUserWhenHandleInsertThenError() {
    userInfo.getOrganizations().getFirst().setOrganizationIpaCode("INVALID_IPA_CODE");
    Assertions.assertThrows(AuthorizationDeniedException.class, () -> service.handleInsert(request, IPA_CODE, userInfo, TOKEN));
  }

  @ParameterizedTest
  @ValueSource(strings = {"not_active_ipa"})
  @NullSource
  void givenInvalidOrganizationWhenHandleInsertThenError(String testCase) {
    if(testCase==null){
      org = null;
    } else {
      org.setStatus(OrganizationStatus.DRAFT);
    }
    when(organizationServiceMock.getOrganizationById(anyLong(), anyString())).thenReturn(Optional.ofNullable(org));
    Assertions.assertThrows(IllegalArgumentException.class, () -> service.handleInsert(request, IPA_CODE, userInfo, TOKEN));
  }

  @Test
  void givenValidOrganizationWhenHandleInsertThenOk() {
    // Given
    DebtPositionDTO mappedDebtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    mappedDebtPositionDTO.setDebtPositionId(null);
    DebtPositionDTO createdDebtPositionDTO = mappedDebtPositionDTO;
    createdDebtPositionDTO.setDebtPositionId(1L);

    Triple<DebtPositionDTO, String, RegistryOutcome> expected = Triple.of(
      createdDebtPositionDTO,
      createdDebtPositionDTO.getPaymentOptions().getFirst().getInstallments().getFirst().getIuv(),
      RegistryOutcome.OK
    );

    when(organizationServiceMock.getOrganizationById(anyLong(), anyString())).thenReturn(Optional.of(org));
    when(debtPositionMapperMock.mapRequestToDebtPosition(request, org, TOKEN))
        .thenReturn(mappedDebtPositionDTO);
    when(manageDebtPositionServiceMock.createSyncedDebtPositions(List.of(mappedDebtPositionDTO), TOKEN))
      .thenReturn(List.of(createdDebtPositionDTO));

    // When
    Triple<DebtPositionDTO, String, RegistryOutcome> result = service.handleInsert(request, IPA_CODE, userInfo, TOKEN);

    // Then
    assertEquals(expected.getLeft(), result.getLeft());
    assertEquals(expected.getMiddle(), result.getMiddle());
    assertEquals(expected.getRight(), result.getRight());
  }
}
