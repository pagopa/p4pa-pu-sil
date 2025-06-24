package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.sil.exception.InvalidValueException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BalanceService {

  private final BalanceUnmashallerService balanceUnmashallerService;

  public BalanceService(BalanceUnmashallerService balanceUnmashallerService) {
    this.balanceUnmashallerService = balanceUnmashallerService;
  }

  public Boolean isBalanceValid(String balance) {
    try {
      balanceUnmashallerService.unmarshal(balance);
      log.info("The balance value is formally valid");
      return Boolean.TRUE;
    } catch (InvalidValueException invalidValueException){
      log.info("The balance value is not valid: {}", invalidValueException.getMessage());
      return Boolean.FALSE;
    }
  }
}
