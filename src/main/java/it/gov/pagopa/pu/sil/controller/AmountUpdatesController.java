package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.sil.controller.generated.ActualizationApi;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.actualization.ActualizationFacadeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class AmountUpdatesController implements ActualizationApi {
  private final ActualizationFacadeService actualizationFacadeService;

  public AmountUpdatesController(ActualizationFacadeService actualizationFacadeService) {
    this.actualizationFacadeService = actualizationFacadeService;
  }

  @Override
  public ResponseEntity<ActualizationResultDTO> actualize(Long orgSilServiceId, String nav) {
    log.info("Requested getAmountUpdates for orgSilServiceId: {}, nav: {}", orgSilServiceId, nav);
    return ResponseEntity.ok(actualizationFacadeService.actualize(orgSilServiceId, nav,
      SecurityUtils.getLoggedUser(), SecurityUtils.getAccessToken()));
  }
}
