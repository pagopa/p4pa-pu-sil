package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.Bilancio;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class PaaSILInviaDovutiMapper {

  private final JAXBTransformService jaxbTransformService;
  private final OrganizationService organizationService;
  private final DebtPositionService debtPositionService;
  private final PersonMapper personMapper;
  private final ValidationService validationService;

  public PaaSILInviaDovutiMapper(JAXBTransformService jaxbTransformService,
                                 OrganizationService organizationService,
                                 DebtPositionService debtPositionService,
                                 PersonMapper personMapper,
                                 ValidationService validationService) {
    this.jaxbTransformService = jaxbTransformService;
    this.organizationService = organizationService;
    this.debtPositionService = debtPositionService;
    this.personMapper = personMapper;
    this.validationService = validationService;
  }

  public Triple<List<DebtPositionDTO>, SilFaults, String> mapRequestToDebtPositionsOrFault(PaaSILInviaDovuti request, UserInfo userInfo, String orgIpaCode, String accessToken) {

    Dovuti dovuti;
    try {
      dovuti = jaxbTransformService.unmarshalling(request.getDovuti(), Dovuti.class);
    } catch (ApplicationException unmarshallingException) {
      String errorMessage = "XML non conforme: \n" +
        jaxbTransformService.getDetailUnmarshalExceptionMessage(unmarshallingException, request.getDovuti());
      log.error("error unmarshalling PaaSILInviaDovuti: [{}]", errorMessage, unmarshallingException);
      return Triple.of(null, SilFaults.PAA_XML_NON_VALIDO, errorMessage);
    }
    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);
    Organization organization = organizationService.getOrganizationById(organizationId, accessToken)
      .orElse(null);
    if (organization == null || !OrganizationStatus.ACTIVE.equals(organization.getStatus())) {
      return Triple.of(null, SilFaults.PAA_ENTE_NON_VALIDO, "L'ente non è valido o non è abilitato");
    }

    if (StringUtils.isNotBlank(dovuti.getDatiVersamento().getIdentificativoUnivocoVersamento())) {
      return Triple.of(null, SilFaults.PAA_IUV_NON_VALIDO, "L'inserimento dello IUV è deprecato");
    }

    Triple<PersonDTO, SilFaults, String> debtorResult = personMapper.getAndValidateDebtor(dovuti.getSoggettoPagatore());
    if (debtorResult.getMiddle() != null) {
      return Triple.of(null, debtorResult.getMiddle(), debtorResult.getRight());
    }

    String description = "";
    if (dovuti.getDatiVersamento().getDatiSingoloVersamentos().size() == 1) {
      description = dovuti.getDatiVersamento().getDatiSingoloVersamentos().getFirst().getCausaleVersamento();
    } else if (dovuti.getDatiVersamento().getDatiSingoloVersamentos().size() > 1) {
      description = "Pagamento multiplo";
    }

    String cartId = UUID.randomUUID().toString();

    List<DebtPositionDTO> debtPositions = new ArrayList<>();
    int idx = 1;
    for (CtDatiSingoloVersamentoDovuti versamento : dovuti.getDatiVersamento().getDatiSingoloVersamentos()) {
      DebtPositionDTO debtPosition = initDebtPosition(description, organization.getOrganizationId());
      Pair<SilFaults, String> fault = fillAndValidateVersamentoFieldsOfDebtPosition(idx++, debtPosition, debtorResult.getLeft(),
        organization, versamento, cartId, accessToken);
      if (fault != null) {
        return Triple.of(null, fault.getLeft(), fault.getRight());
      }
      debtPositions.add(debtPosition);
    }

    return Triple.of(debtPositions, null, null);
  }

  private DebtPositionDTO initDebtPosition(String description, Long orgId) {
    return DebtPositionDTO.builder()
      .debtPositionOrigin(DebtPositionOrigin.SPONTANEOUS) //TODO use SPONTANEOUS_SIL when it will be available P4ADEV-3122
      .organizationId(orgId)
      .flagIuvVolatile(true)
      .description(description)
      .flagPuPagoPaPayment(true)
      .multiDebtor(false)
      .debtPositionTypeOrgId(0L) // will be filled later
      .paymentOptions(List.of(PaymentOptionDTO.builder()
        .description(description)
        .paymentOptionIndex(1)
        .paymentOptionType(PaymentOptionTypeEnum.SINGLE_INSTALLMENT)
        .totalAmountCents(0L) // will be filled later
        .installments(List.of()) // will be filled later
        .build()))
      .build();
  }

  private Pair<SilFaults, String> fillAndValidateVersamentoFieldsOfDebtPosition(int idx, DebtPositionDTO debtPosition, PersonDTO debtor,
                                                                                Organization org, CtDatiSingoloVersamentoDovuti versamento,
                                                                                String cartId, String accessToken) {

    DebtPositionTypeOrg debtPositionTypeOrg = debtPositionService.getDebtPositionTypeOrgByOrgIdAndType(
      org.getOrganizationId(), versamento.getIdentificativoTipoDovuto(), accessToken);

    Pair<SilFaults, String> fault = Optional.ofNullable(validationService.validateDebtPositionTypeOrg(debtPositionTypeOrg, versamento.getIdentificativoTipoDovuto()))
      .or(() -> Optional.ofNullable(personMapper.validateAnonymousDebtor(debtPositionTypeOrg, debtor)))
      .or(() -> Optional.ofNullable(validationService.validateStamp(versamento.getDatiMarcaBolloDigitale(), versamento.getIdentificativoTipoDovuto())))
      .or(() -> Optional.ofNullable(validationService.validatePaymentData(versamento)))
      .or(() -> Optional.ofNullable(validationService.validateIud(org.getOrganizationId(), versamento.getIdentificativoUnivocoDovuto(), accessToken)))
      .orElse(null);
    if (fault != null) {
      return fault;
    }

    String balance = Optional.ofNullable(versamento.getBilancio())
      .map(b -> jaxbTransformService.marshalling(b, Bilancio.class))
      .orElse(debtPositionTypeOrg.getBalance());

    Long amount = ConversionUtils.bigDecimalEuroAmountToCentsAmount(versamento.getImportoSingoloVersamento());

    debtPosition.setIupdOrg(cartId + "-" + idx);
    debtPosition.setDebtPositionTypeOrgId(Objects.requireNonNull(debtPositionTypeOrg.getDebtPositionTypeOrgId()));
    debtPosition.getPaymentOptions().getFirst().setTotalAmountCents(amount);
    debtPosition.getPaymentOptions().getFirst().setInstallments(List.of(
      InstallmentDTO.builder()
        .iud(versamento.getIdentificativoUnivocoDovuto())
        .amountCents(amount)
        .balance(balance)
        .dueDate(Utilities.getSpontaneousSilExpirationDate())
        .debtor(debtor)
        .legacyPaymentMetadata(versamento.getDatiSpecificiRiscossione())
        .remittanceInformation(versamento.getCausaleVersamento())
        .sourceFlowName(Constants.SOURCE_FLOW_NAME_PREFIX_INVIADOVUTI + cartId)
        .build()
    ));

    return null;
  }


}
