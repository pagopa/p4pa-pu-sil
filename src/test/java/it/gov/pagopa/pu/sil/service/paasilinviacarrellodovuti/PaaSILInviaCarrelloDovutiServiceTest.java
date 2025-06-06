package it.gov.pagopa.pu.sil.service.paasilinviacarrellodovuti;


import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.SilOutcome;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovutiRisposta;
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
class PaaSILInviaCarrelloDovutiServiceTest {

  @InjectMocks
  private PaaSILInviaCarrelloDovutiService paaSILInviaCarrelloDovutiService;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @Test
  void givenValidRequestWhenPaaSILInviaCarrelloDovutiServiceThenOk() {
    //given
    UserInfo userInfo = podamFactory.manufacturePojo(UserInfo.class);
    String orgIpaCode = userInfo.getOrganizations().getFirst().getOrganizationIpaCode();
    userInfo.getOrganizations().getFirst().setRoles(List.of("ROLE_X","ROLE_ADMIN"));
    PaaSILInviaCarrelloDovuti request = podamFactory.manufacturePojo(PaaSILInviaCarrelloDovuti.class);

    //when
    Triple<PaaSILInviaCarrelloDovutiRisposta, String, SilOutcome> response = paaSILInviaCarrelloDovutiService.paaSILInviaCarrelloDovuti(userInfo, orgIpaCode, request);

    //verify
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SilOutcome.OK, response.getRight());
    Assertions.assertNotNull(response.getLeft());
    Assertions.assertEquals(SilOutcome.OK.name(), response.getLeft().getEsito());
    Assertions.assertNull(response.getLeft().getFault());
    Assertions.assertNotNull(response.getMiddle());
  }

  @Test
  void givenNotAuthorizedUserWhenPaaSILInviaCarrelloDovutiServiceThenOk() {
    //given
    UserInfo userInfo = podamFactory.manufacturePojo(UserInfo.class);
    String orgIpaCode = podamFactory.manufacturePojo(String.class);
    PaaSILInviaCarrelloDovuti request = podamFactory.manufacturePojo(PaaSILInviaCarrelloDovuti.class);

    //when
    Triple<PaaSILInviaCarrelloDovutiRisposta, String, SilOutcome> response = paaSILInviaCarrelloDovutiService.paaSILInviaCarrelloDovuti(userInfo, orgIpaCode, request);

    //verify
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SilOutcome.KO, response.getRight());
    Assertions.assertNotNull(response.getLeft());
    Assertions.assertEquals(SilOutcome.KO.name(), response.getLeft().getEsito());
    Assertions.assertNotNull(response.getLeft().getFault());
    Assertions.assertEquals(SilFaults.PAA_ENTE_NON_VALIDO.name(), response.getLeft().getFault().getFaultCode());
    Assertions.assertNull(response.getMiddle());
  }
}
