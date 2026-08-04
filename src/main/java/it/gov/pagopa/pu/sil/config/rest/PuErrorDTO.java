package it.gov.pagopa.pu.sil.config.rest;

import it.gov.pagopa.pu.sil.dto.generated.ErrorFieldDTO;

import java.util.List;

public record PuErrorDTO(
  String category,
  String code,
  String message,
  List<ErrorFieldDTO> fields
) {
}
