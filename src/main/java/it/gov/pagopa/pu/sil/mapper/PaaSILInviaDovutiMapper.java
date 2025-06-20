package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.RegistrySilEventType;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class PaaSILInviaDovutiMapper extends AbstractImmediatePaymentsMapper {

  private final OrganizationService organizationService;

  public PaaSILInviaDovutiMapper(JAXBTransformService jaxbTransformService,
                                 OrganizationService organizationService,
                                 DebtPositionService debtPositionService,
                                 PersonMapper personMapper,
                                 ValidationService validationService) {
    super(jaxbTransformService, debtPositionService, validationService, personMapper);
    this.organizationService = organizationService;
  }

  public Triple<List<DebtPositionDTO>, SilFaults, String> mapRequestToDebtPositionsOrFault(PaaSILInviaDovuti request, UserInfo userInfo, String orgIpaCode, String accessToken) {

    //validate organization
    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);
    Organization organization = organizationService.getOrganizationById(organizationId, accessToken)
      .orElse(null);
    if (organization == null || !OrganizationStatus.ACTIVE.equals(organization.getStatus())) {
      return Triple.of(null, SilFaults.PAA_ENTE_NON_VALIDO, "L'ente non è valido o non è abilitato");
    }

    //unmarshall "dovuti"
    try {
      Dovuti dovutiObj = jaxbTransformService.unmarshalling(request.getDovuti(), Dovuti.class, "/soap/wsdl/payments/PagInf_Dovuti_Pagati_6_2_0.xsd");
      return dovutiMapper(RegistrySilEventType.paaSILInviaDovuti, dovutiObj, organization, accessToken);
    } catch (ApplicationException unmarshallingException) {
      String errorMessage = "XML non conforme: \n" +
        jaxbTransformService.getDetailUnmarshalExceptionMessage(unmarshallingException, request.getDovuti());
      log.error("error unmarshalling PaaSILInviaDovuti: [{}]", errorMessage, unmarshallingException);
      return Triple.of(null, SilFaults.PAA_XML_NON_VALIDO, errorMessage);
    }

  }


}
