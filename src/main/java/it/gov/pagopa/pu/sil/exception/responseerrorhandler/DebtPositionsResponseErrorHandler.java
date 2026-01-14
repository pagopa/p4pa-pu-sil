package it.gov.pagopa.pu.sil.exception.responseerrorhandler;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionErrorDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApiClientConfig;
import tools.jackson.databind.json.JsonMapper;

public class DebtPositionsResponseErrorHandler extends AbstractSilResponseErrorHandler<DebtPositionErrorDTO> {
  public DebtPositionsResponseErrorHandler(DebtPositionsApiClientConfig clientConfig, JsonMapper jsonMapper) {
    super(jsonMapper, clientConfig.isPrintBodyWhenError(), "DEBT-POSITIONS");
  }

  @Override
  protected Class<DebtPositionErrorDTO> getErrorDtoClass() {
    return DebtPositionErrorDTO.class;
  }

  @Override
  protected String extractMessageFromDto(DebtPositionErrorDTO errorDto) {
    return errorDto.getMessage();
  }
}
