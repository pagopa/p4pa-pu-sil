package it.gov.pagopa.pu.sil.service.paasillimportadovuto;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.enums.SilOutcome;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovuto;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovutoRisposta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class PaaSILImportaDovutoService {

  public Triple<PaaSILImportaDovutoRisposta, String, SilOutcome> paaSILImportaDovuto(UserInfo userInfo, String orgIpaCode, PaaSILImportaDovuto request) {
    PaaSILImportaDovutoRisposta response = new PaaSILImportaDovutoRisposta();

    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);
    //check if the logged user has the right to call this endpoint
    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call paaSILImportaDovuto for organization {}", clientId, orgIpaCode);
      throw new UnauthorizedException("Utente non autorizzato");
    }

    //TODO P4ADEV-3013 : unmarshall the Dovuti and the DovutiEntiSecondari object

    //TODO P4ADEV-3015..3019: implement business logic
    String iuv = "iuv";
    response.setEsito(SilOutcome.OK.name());
    return Triple.of(response, iuv, SilOutcome.OK);
  }
}


