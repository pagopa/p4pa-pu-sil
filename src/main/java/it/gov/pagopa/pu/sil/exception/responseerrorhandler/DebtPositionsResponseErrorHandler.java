package it.gov.pagopa.pu.sil.exception.responseerrorhandler;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionErrorDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApiClientConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DebtPositionsResponseErrorHandler extends AbstractSilResponseErrorHandler<DebtPositionErrorDTO> {
  private static final String NAME = "DEBT-POSITIONS";

  public DebtPositionsResponseErrorHandler(DebtPositionsApiClientConfig clientConfig, ObjectMapper objectMapper) {
    super(objectMapper, clientConfig.isPrintBodyWhenError(), NAME);
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
