package it.gov.pagopa.pu.sil.service.immediatepayments;


import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovutiRisposta;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class PaaSILInviaDovutiServiceTest {

  @InjectMocks
  private PaaSILInviaDovutiService paaSILInviaDovutiService;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @Test
  void givenValidRequestWhenPaaSILInviaDovutiServiceThenOk() {
    //given
    UserInfo userInfo = podamFactory.manufacturePojo(UserInfo.class);
    String orgIpaCode = userInfo.getOrganizations().getFirst().getOrganizationIpaCode();
    userInfo.getOrganizations().getFirst().setRoles(List.of("ROLE_X","ROLE_ADMIN"));
    PaaSILInviaDovuti request = podamFactory.manufacturePojo(PaaSILInviaDovuti.class);

    //when
    Triple<PaaSILInviaDovutiRisposta, String, RegistryOutcome> response = paaSILInviaDovutiService.paaSILInviaDovuti(userInfo, orgIpaCode, request);

    //verify
    Assertions.assertNotNull(response);
    Assertions.assertEquals(RegistryOutcome.OK, response.getRight());
    Assertions.assertNotNull(response.getLeft());
    Assertions.assertEquals(RegistryOutcome.OK.getValue(), response.getLeft().getEsito());
    Assertions.assertNull(response.getLeft().getFault());
    Assertions.assertNotNull(response.getMiddle());
  }

  @Test
  void givenNotAuthorizedUserWhenPaaSILInviaDovutiServiceThenOk() {
    //given
    UserInfo userInfo = podamFactory.manufacturePojo(UserInfo.class);
    String orgIpaCode = podamFactory.manufacturePojo(String.class);
    PaaSILInviaDovuti request = podamFactory.manufacturePojo(PaaSILInviaDovuti.class);

    //when
    Triple<PaaSILInviaDovutiRisposta, String, RegistryOutcome> response = paaSILInviaDovutiService.paaSILInviaDovuti(userInfo, orgIpaCode, request);

    //verify
    Assertions.assertNotNull(response);
    Assertions.assertEquals(RegistryOutcome.KO, response.getRight());
    Assertions.assertNotNull(response.getLeft());
    Assertions.assertEquals(RegistryOutcome.KO.getValue(), response.getLeft().getEsito());
    Assertions.assertNotNull(response.getLeft().getFault());
    Assertions.assertEquals(SilFaults.PAA_ENTE_NON_VALIDO.name(), response.getLeft().getFault().getFaultCode());
    Assertions.assertNull(response.getMiddle());
  }
}
