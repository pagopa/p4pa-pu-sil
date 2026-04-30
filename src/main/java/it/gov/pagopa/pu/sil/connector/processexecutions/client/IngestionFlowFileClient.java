package it.gov.pagopa.pu.sil.connector.processexecutions.client;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.config.ProcessExecutionsApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IngestionFlowFileClient {
  private final ProcessExecutionsApisHolder processExecutionsApisHolder;

  public IngestionFlowFileClient(
    ProcessExecutionsApisHolder processExecutionsApisHolder) {
    this.processExecutionsApisHolder = processExecutionsApisHolder;
  }

  public Long createIngestionFlowFileReservation(IngestionFlowFileRequestDTO ingestionFlowFileRequestDTO, String accessToken) {
    return processExecutionsApisHolder.getIngestionFlowFileControllerApi(accessToken)
      .createIngestionFlowFileReservation(ingestionFlowFileRequestDTO);
  }
}
