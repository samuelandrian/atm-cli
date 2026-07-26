package io.github.samuelandrian.application;

import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.model.Repayment;
import io.github.samuelandrian.domain.service.TransferResult;
import java.math.BigDecimal;
import java.util.List;

public interface AtmService {
  List<Repayment> deposit(BigDecimal amount);

  void withdraw(BigDecimal amount);

  TransferResult transfer(String targetName, BigDecimal amount);

  List<DebtInfo> getDebtsOwedToOthers(Customer customer);

  List<DebtInfo> getDebtsOwedFromOthers(Customer customer);
}
