package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.MixedDebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionInstallmentService;
import it.gov.pagopa.pu.sil.service.immediatepayments.PaymentRequestMappingResult;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.veneto.regione.pagamenti.ente.ElementoListaDovuti;
import it.veneto.regione.pagamenti.ente.ListaDovutiEntiSecondari;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class PaaSILInviaCarrelloDovutiMapper extends AbstractImmediatePaymentsMapper {

  private final SecondaryTransferMapper secondaryTransferMapper;

  public PaaSILInviaCarrelloDovutiMapper(JAXBTransformService jaxbTransformService,
                                         DebtPositionTypeService debtPositionTypeService,
                                         PersonMapper personMapper,
                                         ValidationService validationService,
                                         SecondaryTransferMapper secondaryTransferMapper,
                                         DebtPositionInstallmentService debtPositionInstallmentService) {
    super(jaxbTransformService, debtPositionTypeService, validationService, personMapper, debtPositionInstallmentService);
    this.secondaryTransferMapper = secondaryTransferMapper;
  }

  public PaymentRequestMappingResult mapRequestToDebtPositions(PaaSILInviaCarrelloDovuti request, Organization organization, String cartId, String accessToken) {
    //validate "dovuti"
    validationService.validatePrimaryDebtPositionOrganization(request, organization.getIpaCode());
    //validate "dovuti secondari"
    validationService.validateSecondaryDebtPositionCount(request, request.getListaDovuti().getElementoListaDovutis().size());

    //unmarshall "dovuti"
    List<Dovuti> dovutiList = new ArrayList<>();
    int idx = 1;
    for (ElementoListaDovuti elem : request.getListaDovuti().getElementoListaDovutis()) {
      try {
        dovutiList.add(jaxbTransformService.unmarshalling(elem.getDovuti(), Dovuti.class, "/soap/wsdl/payments/PagInf_Dovuti_Pagati_6_2_0.xsd"));
        idx++;
      } catch (Exception unmarshallingException) {
        String errorMessage = "XML dovuti [" + idx + "] non conforme: \n" +
          jaxbTransformService.getDetailUnmarshalExceptionMessage(unmarshallingException, elem.getDovuti());
        log.error("error unmarshalling PaaSILInviaCarrelloDovuti [dovuti {}]: [{}]", idx, errorMessage, unmarshallingException);
        throw new SilFaultException(SilFaults.PAA_XML_NON_VALIDO, errorMessage);
      }
    }

    //check max number of debt positions in the cart
    validationService.validateCartSize(dovutiList.size());

    List<DebtPositionDTO> debtPositionList = new ArrayList<>();
    List<MixedDebtPositionDTO> mixedDebtPositionList = new ArrayList<>();

    for (int i = 0; i < dovutiList.size(); i++) {
      Dovuti d = dovutiList.get(i);
      String indexedCartId = cartId + "-" + (i + 1);
      if (d.getDatiVersamento().getDatiSingoloVersamentos().size() > 1) {
        mixedDebtPositionList.add(mixedDebtPositionDTOMapper(RegistryEventType.PTDP_paaSILInviaCarrelloDovuti, indexedCartId, d, organization, accessToken));
      } else {
        debtPositionList.add(dovutiMapper(RegistryEventType.PTDP_paaSILInviaCarrelloDovuti, indexedCartId, d, organization, accessToken));
      }
    }
    handleSecondaryTransfer(debtPositionList, request.getListaDovutiEntiSecondari(), dovutiList, accessToken);

    return new PaymentRequestMappingResult(debtPositionList, mixedDebtPositionList);
  }

  private void handleSecondaryTransfer(List<DebtPositionDTO> debtPositionList,
                                       ListaDovutiEntiSecondari dovutiSecondariList, List<Dovuti> dovutiList, String accessToken) {
    secondaryTransferMapper.mapToCtDatiVersamentoDovutiEntiSecondari(dovutiSecondariList).ifPresent(secondaryTransferData -> {
      validationService.validateSecondaryDebtPositionData(secondaryTransferData, debtPositionList.size());
      secondaryTransferMapper.fillSecondaryTransferData(debtPositionList.getFirst(), secondaryTransferData,
        dovutiList.getFirst().getDatiVersamento().getDatiSingoloVersamentos().getFirst().getIdentificativoTipoDovuto(), accessToken);
    });
  }
}
