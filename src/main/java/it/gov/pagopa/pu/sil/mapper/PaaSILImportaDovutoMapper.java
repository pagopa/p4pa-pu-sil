package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaaSILImportaDovutoMapper {

  private final JAXBTransformService jaxbTransformService;

  public PaaSILImportaDovutoMapper(JAXBTransformService jaxbTransformService) {
    this.jaxbTransformService = jaxbTransformService;
  }

}
