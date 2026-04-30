package it.gov.pagopa.pu.sil.exception.responseerrorhandler;

import it.gov.pagopa.pu.classification.dto.generated.ClassificationErrorDTO;
import it.gov.pagopa.pu.sil.connector.classification.config.ClassificationApiClientConfig;
import tools.jackson.databind.json.JsonMapper;

public class ClassificationResponseErrorHandler extends AbstractSilResponseErrorHandler<ClassificationErrorDTO> {
  public ClassificationResponseErrorHandler(ClassificationApiClientConfig clientConfig, JsonMapper jsonMapper) {
    super(jsonMapper, clientConfig.isPrintBodyWhenError(), "CLASSIFICATION");
  }

  @Override
  protected Class<ClassificationErrorDTO> getErrorDtoClass() {
    return ClassificationErrorDTO.class;
  }

  @Override
  protected String extractMessageFromDto(ClassificationErrorDTO errorDto) {
    return errorDto.getMessage();
  }
}
