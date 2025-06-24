package it.gov.pagopa.pu.sil.connector.processexecutions.client;

import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.config.ProcessExecutionsApisHolder;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

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
    ResponseEntity<Void> response = processExecutionsApisHolder.getExportFileControllerApi(accessToken)
      .createClassificationsExportFileWithHttpInfo(classificationsExportFileRequestDTO);
    return locationHeader(response);
  }

  public Long createPaidExportFile(PaidExportFileRequestDTO paidExportFileRequestDTO, String accessToken) {
    ResponseEntity<Void> response;
    try {
      response = processExecutionsApisHolder.getExportFileControllerApi(accessToken)
      .createPaidExportFileWithHttpInfo(paidExportFileRequestDTO);
    } catch (HttpClientErrorException.BadRequest br) {
      throw resolveException(br);
    }
    return locationHeader(response);
  }

  private RuntimeException resolveException(HttpClientErrorException.BadRequest br) {
    RuntimeException ex = br;
    ProcessExecutionsErrorDTO error = br.getResponseBodyAs(ProcessExecutionsErrorDTO.class);
    if (error != null) {
      ex = new ExportFileClientException(error.getCode(), error.getMessage());
    }
    return ex;
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
