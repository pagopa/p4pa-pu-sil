package it.gov.pagopa.pu.sil.service.singleimport;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.ManageDebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.ManageDebtPositionMapper;
import it.gov.pagopa.pu.sil.mapper.PaaSILImportaDovutoMapper;
import it.gov.pagopa.pu.sil.service.debtposition.ManageDebtPositionService;
import it.gov.pagopa.pu.sil.service.notice.NoticeService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovuto;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovutoRisposta;
import jakarta.activation.DataHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

@Service
@Slf4j
public class PaaSILImportaDovutoService extends BaseDebtPositionHandler<PaaSILImportaDovuto, PaaSILImportaDovutoRisposta> {
  private final PaaSILImportaDovutoMapper paaSILImportaDovutoMapper;
  private final ManageDebtPositionMapper manageDebtPositionMapper;
  private final String puSilBaseUrl;

  public PaaSILImportaDovutoService(OrganizationService organizationService,
                                    DebtPositionService debtPositionService,
                                    ManageDebtPositionService manageDebtPositionService,
                                    NoticeService noticeService,
                                    PaaSILImportaDovutoMapper paaSILImportaDovutoMapper,
                                    ManageDebtPositionMapper manageDebtPositionMapper,
                                    @Value("${public-base-url.pu-sil}") String puSilBaseUrl) {
    super(organizationService, debtPositionService, manageDebtPositionService, noticeService);
    this.paaSILImportaDovutoMapper = paaSILImportaDovutoMapper;
    this.manageDebtPositionMapper = manageDebtPositionMapper;
    this.puSilBaseUrl = puSilBaseUrl;
  }

  @Override
  protected Pair<DebtPositionDTO, String> mapRequestToDebtPosition(PaaSILImportaDovuto request, Organization organization, String accessToken) {
    return paaSILImportaDovutoMapper.mapRequestToDebtPosition(request, organization, accessToken);
  }

  @Override
  protected PaaSILImportaDovutoRisposta mapToRespone(DebtPositionDTO debtPosition, String orgFiscalCode, DataHandler notice, String action) {
    String iuv = debtPosition.getPaymentOptions().getFirst().getInstallments().getFirst().getIuv();
    PaaSILImportaDovutoRisposta response = new PaaSILImportaDovutoRisposta();
    response.setEsito(RegistryOutcome.OK.getValue());
    response.setIdentificativoUnivocoVersamento(iuv);
    response.setBase64ZipAvviso(notice);
    if (action.equals(Constants.LEGACY_IMPORT_ACTION_MODIFY) || action.equals(Constants.LEGACY_IMPORT_ACTION_INSERT)) {
      response.setUrlFileAvviso(composeDownloadPdfNoticeUrl(orgFiscalCode, iuv));
    }
    return response;
  }

  @Override
  protected ManageDebtPositionDTO mapToManageDebtPositionDTO(DebtPositionDTO debtPositionOnDb, DebtPositionDTO debtPositionToSync, String action) {
    //validate data consistency between installment to sync and the one on db
    if (!Objects.equals(debtPositionToSync.getDebtPositionTypeOrgId(), debtPositionOnDb.getDebtPositionTypeOrgId())) {
      throw new SilFaultException(SilFaults.PAA_CAMPO_NON_MODIFICABILE, "Il campo tipo dovuto non può essere modificato");
    }

    InstallmentDTO installmentToSync = debtPositionToSync.getPaymentOptions().getFirst().getInstallments().getFirst();
    return manageDebtPositionMapper.mapToManageDebtPositionDTO(debtPositionOnDb, installmentToSync, action, true);
  }

  private String composeDownloadPdfNoticeUrl(String orgFiscalCode, String iuv) {
    return UriComponentsBuilder.fromUriString(puSilBaseUrl)
      .path("/sil/organization/{orgFiscalCode}/printpaymentnotice/{iuv}")
      .buildAndExpand(orgFiscalCode, iuv)
      .toUriString();
  }
}
