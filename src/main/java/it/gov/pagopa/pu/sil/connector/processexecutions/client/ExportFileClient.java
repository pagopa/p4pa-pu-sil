package it.gov.pagopa.pu.sil.connector.processexecutions.client;

import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.config.ProcessExecutionsApisHolder;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import it.gov.pagopa.pu.sil.exception.InvalidValueException;
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

  public Long createClassificationsExportFile(ClassificationsExportFileRequestDTO classificationsExportFileRequestDTO, String accessToken) {
    ResponseEntity<Void> response;
    try {
      response = processExecutionsApisHolder.getExportFileControllerApi(accessToken)
        .createClassificationsExportFileWithHttpInfo(classificationsExportFileRequestDTO);
    } catch (InvalidValueException invalidValueException) {
      throw resolveException(invalidValueException);
    }
    return locationHeader(response);
  }

  public Long createPaidExportFile(PaidExportFileRequestDTO paidExportFileRequestDTO, String accessToken) {
    ResponseEntity<Void> response;
    try {
      response = processExecutionsApisHolder.getExportFileControllerApi(accessToken)
      .createPaidExportFileWithHttpInfo(paidExportFileRequestDTO);
    } catch (InvalidValueException invalidValueException) {
      throw resolveException(invalidValueException);
    }
    return locationHeader(response);
  }

  private RuntimeException resolveException(InvalidValueException invalidValueException) {
    return new ExportFileClientException(invalidValueException);
  }

  private Long locationHeader(ResponseEntity<Void> response) {
    return Optional.ofNullable(response)
      .map(HttpEntity::getHeaders)
      .flatMap(h -> Optional.ofNullable(h.getLocation()))
      .map(URI::toString)
      .map(Long::valueOf)
      .orElse(null);
  }

}
