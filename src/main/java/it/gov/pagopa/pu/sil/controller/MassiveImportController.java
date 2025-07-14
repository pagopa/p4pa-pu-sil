package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.sil.controller.generated.MassiveApi;
import it.gov.pagopa.pu.sil.dto.generated.ImportFileResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.ImportFileTypeAndVersion;
import org.springframework.http.ResponseEntity;

public class MassiveImportController implements MassiveApi {

  @Override
  public ResponseEntity<ImportFileResponseDTO> massiveImportRequest(String orgFiscalCode,
                                                                    ImportFileTypeAndVersion fileTypeAndVersion,
                                                                    String fileName) {
    return MassiveApi.super.massiveImportRequest(orgFiscalCode, fileTypeAndVersion, fileName);
  }
}
