package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class PaaSILInviaDovutiMapper extends AbstractImmediatePaymentsMapper {

  public PaaSILInviaDovutiMapper(JAXBTransformService jaxbTransformService,
                                 DebtPositionTypeService debtPositionService,
                                 PersonMapper personMapper,
                                 ValidationService validationService) {
    super(jaxbTransformService, debtPositionService, validationService, personMapper);
  }

  public List<DebtPositionDTO> mapRequestToDebtPositions(PaaSILInviaDovuti request, Organization organization, String cartId, String accessToken) {
    //unmarshall "dovuti"
    Dovuti dovutiObj;
    try {
      dovutiObj = jaxbTransformService.unmarshalling(request.getDovuti(), Dovuti.class, "/soap/wsdl/payments/PagInf_Dovuti_Pagati_6_2_0.xsd");
    } catch (ApplicationException unmarshallingException) {
      String errorMessage = "XML non conforme: \n" +
        jaxbTransformService.getDetailUnmarshalExceptionMessage(unmarshallingException, request.getDovuti());
      log.error("error unmarshalling PaaSILInviaDovuti: [{}]", errorMessage, unmarshallingException);
      throw new SilFaultException(SilFaults.PAA_XML_NON_VALIDO, errorMessage);
    }
    return dovutiMapper(RegistryEventType.PTDP_paaSILInviaDovuti, cartId, dovutiObj, organization, accessToken);

  }


}
