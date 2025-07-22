package it.gov.pagopa.pu.sil.service.paasilimportadovuto;


import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.paasillimportadovuto.PaaSILImportaDovutoService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovuto;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovutoRisposta;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import uk.co.jemos.podam.api.PodamFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PaaSILImportaDovutoServiceTest {

  @InjectMocks
  private PaaSILImportaDovutoService paaSILImportaDovutoService;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @Test
  void givenValidRequestWhenPaaSILImportaDovutoServiceThenOk() {
    //given
    String orgIpaCode = "orgIpaCode";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    PaaSILImportaDovuto request = podamFactory.manufacturePojo(PaaSILImportaDovuto.class);

    //when
    Triple<PaaSILImportaDovutoRisposta, String, RegistryOutcome> response = paaSILImportaDovutoService.paaSILImportaDovuto(userInfo, orgIpaCode, request);

    //verify
    Assertions.assertNotNull(response);
    Assertions.assertEquals(RegistryOutcome.OK, response.getRight());
    Assertions.assertNotNull(response.getLeft());
    Assertions.assertEquals(RegistryOutcome.OK.getValue(), response.getLeft().getEsito());
    Assertions.assertNull(response.getLeft().getFault());
    Assertions.assertNotNull(response.getMiddle());
  }

  @Test
  void givenNotAuthorizedUserWhenPaaSILImportaDovutoServiceThenOk() {
    //given
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(-1L, "ORGFC", "OTHERIPACODE");
    String orgIpaCode = podamFactory.manufacturePojo(String.class);
    PaaSILImportaDovuto request = podamFactory.manufacturePojo(PaaSILImportaDovuto.class);


    //when then
    assertThrows(AuthorizationDeniedException.class, () ->
      paaSILImportaDovutoService.paaSILImportaDovuto(userInfo, orgIpaCode, request));
  }
}
