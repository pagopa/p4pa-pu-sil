
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
      .map(id -> debtPositionService.getDebtPositionDTOByInstallmentId(id, accessToken))
      .map(debtPosition -> Pair.of(debtPosition,
        findInstallment(debtPosition, installment -> installment.getInstallmentId().toString().equals(request.id()), SilFaults.PAA_ID_SESSION_NON_VALIDO)))
      .toList();
  }

  public List<Pair<DebtPositionDTO, InstallmentDTO>> getDebtPositionsAndInstallmentsByIud(
    PaymentStatusRequest request,
    Organization organization,
    String accessToken) {
    return getFirstValidDebtPosition(
      debtPositionService.getDebtPositionsByOrganizationIdAndIud(
        organization.getOrganizationId(), request.id(), InstallmentFacadeService.ALLOWED_ORIGINS, accessToken),
      installment -> installment.getIud().equals(request.id()), SilFaults.PAA_IUD_NON_VALIDO);
  }

  public List<Pair<DebtPositionDTO, InstallmentDTO>> getDebtPositionsAndInstallmentsByIuv(
    PaymentStatusRequest request,
    Organization organization,
    String accessToken) {
    return getFirstValidDebtPosition(
      debtPositionService.getDebtPositionsByOrganizationIdAndIuv(
        organization.getOrganizationId(), request.id(), InstallmentFacadeService.ALLOWED_ORIGINS, accessToken),
      installment -> installment.getIuv().equals(request.id()), SilFaults.PAA_IUV_NON_VALIDO);
  }

  private List<Pair<DebtPositionDTO, InstallmentDTO>> getFirstValidDebtPosition(
    List<DebtPositionDTO> debtPositions,
    Predicate<InstallmentDTO> predicate,
    SilFaults fault) {
    return debtPositions.stream()
      .filter(dp -> !Objects.equals(dp.getStatus(), DebtPositionStatus.CANCELLED))
      .findFirst()
      .map(dp -> Pair.of(dp, findInstallment(dp, predicate, fault)))
      .map(List::of)
      .orElse(List.of());
  }

  private InstallmentDTO findInstallment(DebtPositionDTO debtPosition, Predicate<InstallmentDTO> predicate, SilFaults fault) {
    return debtPosition.getPaymentOptions().stream()
      .flatMap(po -> po.getInstallments().stream())
      .filter(predicate)
      .findFirst()
      .orElseThrow(() -> new SilFaultException(fault, "Avviso non trovato"));
  }
}
