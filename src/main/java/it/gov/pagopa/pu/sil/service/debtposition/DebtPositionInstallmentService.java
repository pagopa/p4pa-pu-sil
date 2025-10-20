package it.gov.pagopa.pu.sil.service.debtposition;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.querypayments.PaymentStatusRequest;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Service
public class DebtPositionInstallmentService {
  private final DebtPositionService debtPositionService;
  private final SessionIdMapper sessionIdMapper;

  public DebtPositionInstallmentService(DebtPositionService debtPositionService, SessionIdMapper sessionIdMapper) {
    this.debtPositionService = debtPositionService;
    this.sessionIdMapper = sessionIdMapper;
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
      organization.getOrganizationId(), request.id(), InstallmentFacadeService.ALLOWED_ORIGINS, accessToken);
    return findFirstValidPair(debtPositions, inst -> inst.getIud().equals(request.id()), SilFaults.PAA_IUD_NON_VALIDO);
  }

  public List<Pair<DebtPositionDTO, InstallmentDTO>> getDebtPositionsAndInstallmentsByIuv(
    PaymentStatusRequest request,
    Organization organization,
    String accessToken) {
    List<DebtPositionDTO> debtPositions = debtPositionService.getDebtPositionsByOrganizationIdAndIuv(
      organization.getOrganizationId(), request.id(), InstallmentFacadeService.ALLOWED_ORIGINS, accessToken);
    return findFirstValidPair(debtPositions, inst -> inst.getIuv().equals(request.id()), SilFaults.PAA_IUV_NON_VALIDO);
  }

  private Pair<DebtPositionDTO, InstallmentDTO> createPairFromInstallmentId(Long installmentId, String accessToken) {
    DebtPositionDTO debtPosition = debtPositionService.getDebtPositionDTOByInstallmentId(installmentId, accessToken);
    InstallmentDTO installment = findInstallment(debtPosition, inst -> inst.getInstallmentId().equals(installmentId), SilFaults.PAA_ID_SESSION_NON_VALIDO);
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
}
