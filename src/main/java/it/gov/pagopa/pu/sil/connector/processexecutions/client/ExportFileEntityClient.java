package it.gov.pagopa.pu.sil.connector.processexecutions.client;

import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile;
import it.gov.pagopa.pu.sil.connector.processexecutions.config.ProcessExecutionsApisHolder;
import it.gov.pagopa.pu.sil.exception.common.RestInvokeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExportFileEntityClient {
  private final ProcessExecutionsApisHolder processExecutionsApisHolder;

  public ExportFileEntityClient(ProcessExecutionsApisHolder processExecutionsApisHolder) {
    this.processExecutionsApisHolder = processExecutionsApisHolder;
  }

  public ExportFile getExportFile(Long exportFileId, String accessToken) {
    try {
      log.debug("Fetching export file with ID [{}]", exportFileId);
      return processExecutionsApisHolder.getExportFileEntityControllerApi(accessToken)
        .crudGetExportfile(String.valueOf(exportFileId));
    } catch (RestInvokeNotFoundException e) {
      log.info("Cannot find ExportFile with ID [{}]", exportFileId);
      return null;
    }
  }
}
