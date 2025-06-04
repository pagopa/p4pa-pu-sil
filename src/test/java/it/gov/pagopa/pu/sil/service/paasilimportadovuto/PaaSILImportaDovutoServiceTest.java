package it.gov.pagopa.pu.sil.service.paasilimportadovuto;


import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.enums.SilOutcome;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.paasillimportadovuto.PaaSILImportaDovutoService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovuto;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovutoRisposta;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class PaaSILImportaDovutoServiceTest {

  @InjectMocks
  private PaaSILImportaDovutoService paaSILImportaDovutoService;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @Test
  void givenValidRequestWhenPaaSILImportaDovutoServiceThenOk() {
    //given
    UserInfo userInfo = podamFactory.manufacturePojo(UserInfo.class);
    String orgIpaCode = userInfo.getOrganizations().getFirst().getOrganizationIpaCode();
    userInfo.getOrganizations().getFirst().setRoles(List.of("ROLE_X","ROLE_ADMIN"));
    PaaSILImportaDovuto request = podamFactory.manufacturePojo(PaaSILImportaDovuto.class);

    //when
    Triple<PaaSILImportaDovutoRisposta, String, SilOutcome> response = paaSILImportaDovutoService.paaSILImportaDovuto(userInfo, orgIpaCode, request);

    //verify
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SilOutcome.OK, response.getRight());
    Assertions.assertNotNull(response.getLeft());
    Assertions.assertEquals(SilOutcome.OK.name(), response.getLeft().getEsito());
    Assertions.assertNull(response.getLeft().getFault());
    Assertions.assertNotNull(response.getMiddle());
  }

  @Test
  void givenNotAuthorizedUserWhenPaaSILImportaDovutoServiceThenOk() {
    //given
    UserInfo userInfo = podamFactory.manufacturePojo(UserInfo.class);
    String orgIpaCode = podamFactory.manufacturePojo(String.class);
    PaaSILImportaDovuto request = podamFactory.manufacturePojo(PaaSILImportaDovuto.class);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);

      //when then
      assertThrows(UnauthorizedException.class, () ->
        paaSILImportaDovutoService.paaSILImportaDovuto(userInfo, orgIpaCode, request));
    }
  }
}
