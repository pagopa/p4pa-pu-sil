package it.gov.pagopa.pu.sil.mapper.soap;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.MixedDebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.exception.common.InvalidValueException;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.service.inbound.payments.debtposition.DebtPositionInstallmentService;
import it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments.PaymentRequestMappingResult;
import it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments.soap.ValidationService;
import it.gov.pagopa.pu.sil.service.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
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
                                 ValidationService validationService,
                                 DebtPositionInstallmentService debtPositionInstallmentService) {
    super(jaxbTransformService, debtPositionService, validationService, personMapper, debtPositionInstallmentService);
  }

  public PaymentRequestMappingResult mapRequestToDebtPositions(PaaSILInviaDovuti request, Organization organization, String cartId, String accessToken) {
    //unmarshall "dovuti"
    Dovuti dovutiObj;
    try {
      dovutiObj = jaxbTransformService.unmarshalling(request.getDovuti(), Dovuti.class, "/soap/wsdl/payments/PagInf_Dovuti_Pagati_6_2_0.xsd");
    } catch (Exception unmarshallingException) {
      String detailUnmarshalExceptionMessage = jaxbTransformService.getDetailUnmarshalExceptionMessage(unmarshallingException, request.getDovuti());
      String silFaultCustomMessage = "XML non conforme: \n" + detailUnmarshalExceptionMessage;
      String message = String.format("error unmarshalling PaaSILInviaDovuti: [%s]", detailUnmarshalExceptionMessage);

      log.error(message, unmarshallingException);

      throw new InvalidValueException(ErrorCodeConstants.ERROR_CODE_XML_UNMARSHALLING_ERROR, message, silFaultCustomMessage);
    }

    validationService.validateCartSize(dovutiObj.getDatiVersamento().getDatiSingoloVersamentos().size());

    if (dovutiObj.getDatiVersamento().getDatiSingoloVersamentos().size() == 1) {
      DebtPositionDTO debtPositionDTO = dovutiMapper(RegistryEventType.PTDP_paaSILInviaDovuti, cartId, dovutiObj, organization, accessToken);
      return PaymentRequestMappingResult.ofDebtPositions(List.of(debtPositionDTO));
    }
    MixedDebtPositionDTO mixedDebtPositionDTO = mixedDebtPositionDTOMapper(RegistryEventType.PTDP_paaSILInviaDovuti, cartId, dovutiObj, organization, accessToken);
    return PaymentRequestMappingResult.ofMixedDebtPositions(List.of(mixedDebtPositionDTO));
  }
}
