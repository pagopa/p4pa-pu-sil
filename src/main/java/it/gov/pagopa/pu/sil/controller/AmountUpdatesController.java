package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.sil.controller.generated.ActualizationApi;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.actualization.ActualizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class AmountUpdatesController implements ActualizationApi {
  private final ActualizationService actualizationService;

  public AmountUpdatesController(ActualizationService actualizationService) {
    this.actualizationService = actualizationService;
  }

  @Override
  public ResponseEntity<ActualizationResultDTO> actualize(Long orgSilServiceId, String nav) {
    log.info("Requested getAmountUpdates for orgSilServiceId: {}, nav: {}", orgSilServiceId, nav);
    return ResponseEntity.ok(actualizationService.actualize(orgSilServiceId, nav,
      SecurityUtils.getLoggedUser(), SecurityUtils.getAccessToken()));
  }
}
