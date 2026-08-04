package it.gov.pagopa.pu.sil.mapper.soap;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.InvalidValueException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments.soap.ValidationService;
import it.gov.pagopa.pu.sil.service.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovuto;
import it.veneto.regione.schemas._2012.pagamenti.ente.Bilancio;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiVersamento;
import it.veneto.regione.schemas._2012.pagamenti.ente.Versamento;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class PaaSILImportaDovutoMapper {

  public static final String IMPORT_DOVUTO = "_IMPORT-DOVUTO_";

  private final DebtPositionTypeService debtPositionTypeService;
  private final JAXBTransformService jaxbTransformService;
  private final PersonMapper personMapper;
  private final SecondaryTransferMapper secondaryTransferMapper;
  private final ValidationService immediatePaymentsValidationService;
  private final String auxDigit;

  public PaaSILImportaDovutoMapper(DebtPositionTypeService debtPositionTypeService,
                                   JAXBTransformService jaxbTransformService,
                                   PersonMapper personMapper,
                                   SecondaryTransferMapper secondaryTransferMapper,
                                   ValidationService immediatePaymentsValidationService,
                                   @Value("${nav.aux-digit}") String auxDigit) {
    this.debtPositionTypeService = debtPositionTypeService;
    this.jaxbTransformService = jaxbTransformService;
    this.personMapper = personMapper;
    this.secondaryTransferMapper = secondaryTransferMapper;
    this.immediatePaymentsValidationService = immediatePaymentsValidationService;
    this.auxDigit = auxDigit;
  }

  public Pair<DebtPositionDTO, String> mapRequestToDebtPosition(PaaSILImportaDovuto request, Organization organization, String accessToken) {

    //unmarshall versamento
    Versamento versamento;
    try {
      versamento = jaxbTransformService.unmarshalling(request.getDovuto(), Versamento.class, "/soap/wsdl/payments/PagInf_Dovuti_Pagati_6_2_0.xsd");
    } catch (Exception unmarshallingException) {
      String errorMessage = "XML non conforme: \n" +
        jaxbTransformService.getDetailUnmarshalExceptionMessage(unmarshallingException, request.getDovuto());
      log.error("error unmarshalling PaaSILImportaDovuto: [{}]", errorMessage, unmarshallingException);
      throw new SilFaultException(SilFaults.PAA_XML_NON_VALIDO, errorMessage);
    }

    //all debt-position data validations are made on DEBT-POSITION API (see task P4ADEV-3458)

    CtDatiVersamento datiVersamento = versamento.getDatiVersamento();
    DebtPositionTypeOrg debtPositionTypeOrg = debtPositionTypeService.getDebtPositionTypeOrgByOrgIdAndType(
      organization.getOrganizationId(), datiVersamento.getIdentificativoTipoDovuto(), accessToken);

    if (debtPositionTypeOrg == null) {
      throw new SilFaultException(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, "Identificativo tipo dovuto non valido");
    }

    Long amountCents = ConversionUtils.bigDecimalEuroAmountToCentsAmount(datiVersamento.getImportoSingoloVersamento());

    if(amountCents == null) {
      throw new SilFaultException(SilFaults.PAA_IMPORTO_SINGOLO_VERSAMENTO_NON_VALIDO, "Importo singolo versamento non valido");
    }

    DebtPositionDTO debtPositionDTO = DebtPositionDTO.builder()
      .status(DebtPositionStatus.UNPAID)
      .organizationId(Objects.requireNonNull(organization.getOrganizationId()))
      .debtPositionOrigin(DebtPositionOrigin.ORDINARY_SIL)
      .debtPositionTypeOrgId(Objects.requireNonNull(debtPositionTypeOrg.getDebtPositionTypeOrgId()))
      .description("Posizione debitoria " + debtPositionTypeOrg.getDescription())
      .flagPuPagoPaPayment(true)
      .paymentOptions(List.of(PaymentOptionDTO.builder()
        .status(PaymentOptionStatus.UNPAID)
        .paymentOptionIndex(1)
        .paymentOptionType(PaymentOptionType.SINGLE_INSTALLMENT)
        .description("Posizione debitoria " + debtPositionTypeOrg.getDescription())
        .totalAmountCents(amountCents)
        .installments(List.of(fillInstallmentFields(organization.getIpaCode(), versamento, new InstallmentDTO())))
        .build()))
      .build();

    //secondary transfer (multi-beneficiary payment)
    secondaryTransferMapper.mapToCtDatiVersamentoDovutiEntiSecondari(request.getListaDovutiEntiSecondari()).ifPresent(secondaryTransferData -> {
      immediatePaymentsValidationService.validateSecondaryDebtPositionData(secondaryTransferData, 1);
      secondaryTransferMapper.fillSecondaryTransferData(debtPositionDTO, secondaryTransferData, datiVersamento.getIdentificativoTipoDovuto(), accessToken);
    });

    return Pair.of(debtPositionDTO, versamento.getAzione());
  }

  private InstallmentDTO fillInstallmentFields(String ipaCode, Versamento versamento, InstallmentDTO installmentDTO) {
    CtDatiVersamento datiVersamento = versamento.getDatiVersamento();

    //the object Versamento is supposed to be validated before this mapping
    PersonDTO debtor = personMapper.getAndValidateDebtor(versamento.getSoggettoPagatore());

    long amountCents = ConversionUtils.bigDecimalEuroAmountToCentsAmount(datiVersamento.getImportoSingoloVersamento());

    LocalDate dueDate = Optional.ofNullable(datiVersamento.getDataEsecuzionePagamento())
      .map(ConversionUtils::toLocalDate)
      .orElse(null);

    String balanceString;
    try {
      balanceString = jaxbTransformService.marshallingNoNamespace(datiVersamento.getBilancio(), Bilancio.class);
    } catch (Exception e) {
      log.error("Error unmarshalling bilancio: {}", datiVersamento.getBilancio(), e);
      throw new InvalidValueException(ErrorCodeConstants.ERROR_CODE_INVALID_BALANCE, "Invalid bilancio format", e);
    }

    String now = LocalDate.now(Constants.ZONEID).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    return installmentDTO
      .status(InstallmentStatus.UNPAID)
      .iud(datiVersamento.getIdentificativoUnivocoDovuto())
      .iuv(datiVersamento.getIdentificativoUnivocoVersamento())
      .nav(Utilities.iuv2Nav(datiVersamento.getIdentificativoUnivocoVersamento(), auxDigit))
      .amountCents(amountCents)
      .dueDate(dueDate)
      .remittanceInformation(datiVersamento.getCausaleVersamento())
      .legacyPaymentMetadata(datiVersamento.getDatiSpecificiRiscossione())
      .balance(balanceString)
      .debtor(debtor)
      .generateNotice(false)
      .sourceFlowName(ipaCode + IMPORT_DOVUTO + now);
  }

}
