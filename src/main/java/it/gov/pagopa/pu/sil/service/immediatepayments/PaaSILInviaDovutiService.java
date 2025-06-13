package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.SilOutcome;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.util.soap.FaultUtils;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovutiRisposta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class PaaSILInviaDovutiService {

  public Triple<PaaSILInviaDovutiRisposta, String, SilOutcome> paaSILInviaDovuti(UserInfo userInfo, String orgIpaCode, PaaSILInviaDovuti request) {
    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);
    //check if the logged user has the right to call this endpoint
    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call paaSILInviaDovuti for organization {}", clientId, orgIpaCode);
      return setFaultResponse(SilFaults.PAA_ENTE_NON_VALIDO, "Utente non autorizzato");
    }

    //TODO P4ADEV-3076: unmarshall the request

    //TODO P4ADEV-3078: implement business logic
    String iuv = "iuv";
    PaaSILInviaDovutiRisposta response = new PaaSILInviaDovutiRisposta();
    response.setEsito(SilOutcome.OK.name());
    return Triple.of(response, iuv, SilOutcome.OK);
  }

  private Triple<PaaSILInviaDovutiRisposta, String, SilOutcome> setFaultResponse(SilFaults fault, String description) {
    PaaSILInviaDovutiRisposta response = new PaaSILInviaDovutiRisposta();
    response.setEsito(SilOutcome.KO.name());
    return Triple.of(FaultUtils.setFaultOnResponse(response, fault, description), null, SilOutcome.KO);
  }
}
