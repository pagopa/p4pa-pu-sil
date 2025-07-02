package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.util.Constants;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class SessionIdMapper {

  public String mapDebtPositionsToSessionId(List<DebtPositionDTO> debtPositions) {
    return debtPositions.stream()
      .flatMap(dp -> dp.getPaymentOptions().stream())
      .flatMap(option -> option.getInstallments().stream())
      .map(InstallmentDTO::getInstallmentId)
      .map(String::valueOf)
      .collect(Collectors.joining(Constants.SESSION_ID_SEPARATOR));
  }

  public List<Long> mapSessionIdToInstallmentIds(String sessionId) {
    try{
      return Stream.of(sessionId.split(Constants.SESSION_ID_SEPARATOR))
        .map(Long::parseLong)
        .collect(Collectors.toList());
    } catch (NullPointerException | NumberFormatException e) {
      log.error("Invalid sessionId: {}", sessionId, e);
      throw new SilFaultException(SilFaults.PAA_ID_SESSION_NON_VALIDO, "ID session non valido");
    }

  }
}
