package it.gov.pagopa.pu.sil.service.debtposition;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.querypayments.PaymentStatusRequest;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static it.gov.pagopa.pu.sil.util.ValidationUtils.getTransferCategoryFromLegacyPaymentMetadataSecondary;

@Service
public class DebtPositionInstallmentService {
  private final DebtPositionService debtPositionService;
  private final SessionIdMapper sessionIdMapper;
  private final DebtPositionTypeService debtPositionTypeService;

  public DebtPositionInstallmentService(DebtPositionService debtPositionService, SessionIdMapper sessionIdMapper, DebtPositionTypeService debtPositionTypeService) {
    this.debtPositionService = debtPositionService;
    this.sessionIdMapper = sessionIdMapper;
    this.debtPositionTypeService = debtPositionTypeService;
  }

  public List<Pair<DebtPositionDTO, InstallmentDTO>> getDebtPositionsAndInstallmentsByInstallmentId(
    PaymentStatusRequest request,
    String accessToken) {
    return sessionIdMapper.mapSessionIdToInstallmentIds(request.id()).stream()
      .map(id -> createPairFromInstallmentId(id, accessToken))
      .toList();
  }

  public List<Pair<DebtPositionDTO, InstallmentDTO>> getDebtPositionsAndInstallmentsByIud(
    PaymentStatusRequest request,
    Organization organization,
    String accessToken) {
    List<DebtPositionDTO> debtPositions = debtPositionService.getDebtPositionsByOrganizationIdAndIud(
      organization.getOrganizationId(), request.id(), Constants.ORDINARY_DEBT_POSITION_ORIGINS, accessToken);
    return findFirstValidPair(debtPositions, inst -> Objects.equals(inst.getIud(), request.id()), SilFaults.PAA_IUD_NON_VALIDO);
  }

  public List<Pair<DebtPositionDTO, InstallmentDTO>> getDebtPositionsAndInstallmentsByIuv(
    PaymentStatusRequest request,
    Organization organization,
    String accessToken) {
    List<DebtPositionDTO> debtPositions = debtPositionService.getDebtPositionsByOrganizationIdAndIuv(
      organization.getOrganizationId(), request.id(), Constants.ORDINARY_DEBT_POSITION_ORIGINS, accessToken);
    return findFirstValidPair(debtPositions, inst -> Objects.equals(inst.getIuv(), request.id()), SilFaults.PAA_IUV_NON_VALIDO);
  }

  private Pair<DebtPositionDTO, InstallmentDTO> createPairFromInstallmentId(Long installmentId, String accessToken) {
    DebtPositionDTO debtPosition = debtPositionService.getDebtPositionDTOByInstallmentId(installmentId, accessToken);

    if (debtPosition == null) {
      throw new SilFaultException(SilFaults.PAA_ID_SESSION_NON_VALIDO, "id session non valido");
    }

    InstallmentDTO installment = findInstallment(debtPosition, inst -> Objects.equals(inst.getInstallmentId(), installmentId), SilFaults.PAA_ID_SESSION_NON_VALIDO);
    return Pair.of(debtPosition, installment);
  }

  private List<Pair<DebtPositionDTO, InstallmentDTO>> findFirstValidPair(
    List<DebtPositionDTO> debtPositions,
    Predicate<InstallmentDTO> predicate,
    SilFaults fault) {
    return debtPositions.stream()
      .filter(this::isNotCancelled)
      .findFirst()
      .map(dp -> Pair.of(dp, findInstallment(dp, predicate, fault)))
      .map(List::of)
      .orElse(List.of());
  }

  private boolean isNotCancelled(DebtPositionDTO debtPosition) {
    return !Objects.equals(debtPosition.getStatus(), DebtPositionStatus.CANCELLED);
  }

  private InstallmentDTO findInstallment(DebtPositionDTO debtPosition, Predicate<InstallmentDTO> predicate, SilFaults fault) {
    return debtPosition.getPaymentOptions().stream()
      .flatMap(po -> po.getInstallments().stream())
      .filter(predicate)
      .findFirst()
      .orElseThrow(() -> new SilFaultException(fault, "Avviso non trovato"));
  }

  public String getCategory(String legacyPaymentMetadata, String debtPositionTypeOrgCode, Long organizationId, String accessToken) {
    String category = getTransferCategoryFromLegacyPaymentMetadataSecondary(legacyPaymentMetadata);
    if(category == null) {
      DebtPositionTypeOrg debtPositionTypeOrg = debtPositionTypeService.getDebtPositionTypeOrgByOrgIdAndType(
        organizationId, debtPositionTypeOrgCode, accessToken);
      if (debtPositionTypeOrg == null) {
        throw new SilFaultException(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, "Tipo dovuto non valido: " + debtPositionTypeOrgCode);
      }
      DebtPositionType debtPositionType = debtPositionTypeService.getDebtPositionTypeById(debtPositionTypeOrg.getDebtPositionTypeId(), accessToken);
      category = debtPositionType.getTaxonomyCode();
    }
    Optional<String> prefix = ValidationUtils.CATEGORY_ALLOWED_PREFIXES.stream().filter(category::startsWith).findAny();
    if (prefix.isPresent()) {
      category = category.replace(prefix.get(), "");
    }
    return category.replace("/", "");
  }
}
