package it.gov.pagopa.pu.sil.service.paasillimportadovuto;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovuto;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovutoRisposta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaaSILImportaDovutoService {

  public Triple<PaaSILImportaDovutoRisposta, String, RegistryOutcome> paaSILImportaDovuto(UserInfo userInfo, String orgIpaCode, PaaSILImportaDovuto request) {
    PaaSILImportaDovutoRisposta response = new PaaSILImportaDovutoRisposta();
    //check if the logged user has the right to call this endpoint
    AuthorizationService.validateAdminRole(orgIpaCode, userInfo);

    //TODO P4ADEV-3013 : unmarshall the Dovuti and the DovutiEntiSecondari object

    //TODO P4ADEV-3015..3019: implement business logic
    String iuv = "iuv";
    response.setEsito(RegistryOutcome.OK.getValue());
    return Triple.of(response, iuv, RegistryOutcome.OK);
  }
}


