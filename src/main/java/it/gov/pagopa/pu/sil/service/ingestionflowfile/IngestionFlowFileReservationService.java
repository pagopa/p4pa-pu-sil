package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum;

public interface IngestionFlowFileReservationService {
  String uploadUrlGenerator(IngestionFlowFileTypeEnum ingestionFlowFileType, Long organizationId, String accessToken);
}
