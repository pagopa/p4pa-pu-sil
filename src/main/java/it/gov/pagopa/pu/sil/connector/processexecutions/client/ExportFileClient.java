package it.gov.pagopa.pu.sil.connector.processexecutions.client;

import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.config.ProcessExecutionsApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Optional;

@Slf4j
@Service
public class ExportFileClient {
  private final ProcessExecutionsApisHolder processExecutionsApisHolder;

  public ExportFileClient(
    ProcessExecutionsApisHolder processExecutionsApisHolder) {
    this.processExecutionsApisHolder = processExecutionsApisHolder;
  }

  public String createClassificationsExportFile(ClassificationsExportFileRequestDTO classificationsExportFileRequestDTO, String accessToken) {
    ResponseEntity<Void> response = processExecutionsApisHolder.getExportFileControllerApi(accessToken)
      .createClassificationsExportFileWithHttpInfo(classificationsExportFileRequestDTO);
    return locationHeader(response);
  }

  public String createPaidExportFile(PaidExportFileRequestDTO paidExportFileRequestDTO, String accessToken) {
    ResponseEntity<Void> response = processExecutionsApisHolder.getExportFileControllerApi(accessToken)
      .createPaidExportFileWithHttpInfo(paidExportFileRequestDTO);
    return locationHeader(response);
  }

  private String locationHeader(ResponseEntity<Void> response) {
    return Optional.ofNullable(response)
      .map(HttpEntity::getHeaders)
      .flatMap(h -> Optional.ofNullable(h.getLocation()))
      .map(URI::toString)
      .orElse(null);
  }

}
