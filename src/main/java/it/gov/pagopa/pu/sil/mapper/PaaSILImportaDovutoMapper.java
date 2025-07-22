package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovuto;
import it.veneto.regione.schemas._2012.pagamenti.ente.Bilancio;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiVersamento;
import it.veneto.regione.schemas._2012.pagamenti.ente.Versamento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaaSILImportaDovutoMapper {

  private final DebtPositionService debtPositionService;
  private final JAXBTransformService jaxbTransformService;
  private final PersonMapper personMapper;
  private final SecondaryTransferMapper secondaryTransferMapper;
  private final ValidationService immediatePaymentsValidationService;


  public Pair<DebtPositionDTO, String> mapRequestToDebtPosition(PaaSILImportaDovuto request, Organization organization, String accessToken) {

    //unmarshall versamento
    Versamento versamento;
    try {
      versamento = jaxbTransformService.unmarshalling(request.getDovuto(), Versamento.class, "/soap/wsdl/payments/PagInf_Dovuti_Pagati_6_2_0.xsd");
    } catch (ApplicationException unmarshallingException) {
      String errorMessage = "XML non conforme: \n" +
        jaxbTransformService.getDetailUnmarshalExceptionMessage(unmarshallingException, request.getDovuto());
      log.error("error unmarshalling PaaSILImportaDovuto: [{}]", errorMessage, unmarshallingException);
      throw new SilFaultException(SilFaults.PAA_XML_NON_VALIDO, errorMessage);
    }

    //all debt-position data validations are made on DEBT-POSITION API (see task P4ADEV-3458)

    CtDatiVersamento datiVersamento = versamento.getDatiVersamento();
    DebtPositionTypeOrg debtPositionTypeOrg = debtPositionService.getDebtPositionTypeOrgByOrgIdAndType(
      organization.getOrganizationId(), datiVersamento.getIdentificativoTipoDovuto(), accessToken);

    long amountCents = ConversionUtils.bigDecimalEuroAmountToCentsAmount(datiVersamento.getImportoSingoloVersamento());

    DebtPositionDTO debtPositionDTO = DebtPositionDTO.builder()
      .status(DebtPositionStatus.UNPAID)
      .organizationId(organization.getOrganizationId())
      .debtPositionOrigin(DebtPositionOrigin.ORDINARY_SIL)
      .debtPositionTypeOrgId(debtPositionTypeOrg.getDebtPositionTypeOrgId())
      .flagPuPagoPaPayment(true)
      .flagIuvVolatile(true)
      .paymentOptions(List.of(PaymentOptionDTO.builder()
        .status(PaymentOptionStatus.UNPAID)
        .paymentOptionIndex(1)
        .paymentOptionType(PaymentOptionTypeEnum.SINGLE_INSTALLMENT)
        .totalAmountCents(amountCents)
        .installments(List.of(fillInstallmentFields(versamento, new InstallmentDTO())))
        .build()))
      .build();

    //secondary transfer (multi-beneficiary payment)
    secondaryTransferMapper.mapToCtDatiVersamentoDovutiEntiSecondari(request.getListaDovutiEntiSecondari()).ifPresent(secondaryTransferData -> {
      immediatePaymentsValidationService.validateSecondaryDebtPositionData(secondaryTransferData, 1);
      secondaryTransferMapper.fillSecondaryTransferData(debtPositionDTO, secondaryTransferData, debtPositionTypeOrg.getCode(), accessToken);
    });

    return Pair.of(debtPositionDTO, versamento.getAzione());
  }

  private InstallmentDTO fillInstallmentFields(Versamento versamento, InstallmentDTO installmentDTO) {
    CtDatiVersamento datiVersamento = versamento.getDatiVersamento();

    //the object Versamento is supposed to be validated before this mapping
    PersonDTO debtor = personMapper.getAndValidateDebtor(versamento.getSoggettoPagatore());

    long amountCents = ConversionUtils.bigDecimalEuroAmountToCentsAmount(datiVersamento.getImportoSingoloVersamento());

    LocalDate dueDate = Optional.ofNullable(datiVersamento.getDataEsecuzionePagamento())
      .map(ConversionUtils::toOffsetDateTime)
      .map(OffsetDateTime::toLocalDate)
      .orElse(null);

    String balanceString;
    try {
      balanceString = jaxbTransformService.marshallingNoNamespace(datiVersamento.getBilancio(), Bilancio.class);
    } catch (Exception e) {
      log.error("Error unmarshalling bilancio: {}", datiVersamento.getBilancio(), e);
      throw new ApplicationException("Invalid bilancio format", e);
    }

    return installmentDTO
      .status(InstallmentStatus.UNPAID)
      .iud(datiVersamento.getIdentificativoUnivocoDovuto())
      .iuv(datiVersamento.getIdentificativoUnivocoVersamento())
      .nav(Utilities.iuv2Nav(datiVersamento.getIdentificativoUnivocoVersamento()))
      .amountCents(amountCents)
      .dueDate(dueDate)
      .remittanceInformation(datiVersamento.getCausaleVersamento())
      .legacyPaymentMetadata(datiVersamento.getDatiSpecificiRiscossione())
      .balance(balanceString)
      .debtor(debtor);
  }

  private void fillInstallmentOnDbWithFieldsToSync(InstallmentDTO installmentOnDb, InstallmentDTO installmentToSync) {
    installmentOnDb.setIud(installmentToSync.getIud());
    installmentOnDb.setIuv(installmentToSync.getIuv());
    installmentOnDb.setNav(installmentToSync.getNav());
    installmentOnDb.setAmountCents(installmentToSync.getAmountCents());
    installmentOnDb.setDueDate(installmentToSync.getDueDate());
    installmentOnDb.setRemittanceInformation(installmentToSync.getRemittanceInformation());
    installmentOnDb.setLegacyPaymentMetadata(installmentToSync.getLegacyPaymentMetadata());
    installmentOnDb.setBalance(installmentToSync.getBalance());
    installmentOnDb.setDebtor(installmentToSync.getDebtor());

    if(installmentToSync.getTransfers()!=null){
      //multi-beneficiary handling
      secondaryTransferMapper.checkAndFillSupportedTransfersConfigurationForModify(installmentOnDb, installmentToSync);
    }
  }

  public ManageDebtPositionDTO mapToManageDebtPositionDTO(DebtPositionDTO debtPositionOnDb, InstallmentDTO installmentToSync, String action) {
    InstallmentDTO installmentOnDb = debtPositionOnDb.getPaymentOptions()
      .stream().flatMap(po -> po.getInstallments().stream())
      .filter(i -> Objects.equals(i.getIud(), installmentToSync.getIud()))
      .findFirst()
      .orElseThrow(() -> {
        log.error("Installment not found on debtPosition[{}] for organizationId[{}] and iud[{}]", debtPositionOnDb.getDebtPositionId(),
          debtPositionOnDb.getOrganizationId(), installmentToSync.getIud());
        return new SilFaultException(SilFaults.PAA_IMPORT_DOVUTO_NON_PRESENTE, "Dovuto non trovato");
      });

    PaymentOptionDTO paymentOptionOnDb = debtPositionOnDb.getPaymentOptions().stream()
      .filter(po -> Objects.equals(po.getPaymentOptionId(), installmentToSync.getPaymentOptionId()))
      .findFirst().get();

    if(action.equals(Constants.LEGACY_IMPORT_ACTION_MODIFY)){
      //this is necessary only for action M (modify), for action A (cancel) the only relevant field is installmentId
      fillInstallmentOnDbWithFieldsToSync(installmentOnDb, installmentToSync);
    }

    ManageInstallmentDTO manageInstallmentDTO = ManageInstallmentDTO.builder()
      .action(Action.fromValue(action))
      .installment(installmentOnDb)
      .build();

    return ManageDebtPositionDTO.builder()
      .debtPositionDescription(debtPositionOnDb.getDescription())
      .paymentOptionId(paymentOptionOnDb.getPaymentOptionId())
      .paymentOptionDescription(paymentOptionOnDb.getDescription())
      .validityDate(debtPositionOnDb.getValidityDate())
      .installments(List.of(manageInstallmentDTO))
      .build();
  }

}
