package io.github.samuelandrian.application;

import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.model.Repayment;
import io.github.samuelandrian.domain.repository.CustomerRepository;
import io.github.samuelandrian.domain.service.TransferResult;
import io.github.samuelandrian.domain.service.TransferService;
import io.github.samuelandrian.exception.InvalidAmountException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AtmServiceImpl implements AtmService {
  private final CustomerRepository repository;
  private final TransferService transferService;
  private final AuthService authService;

  @Override
  public List<Repayment> deposit(BigDecimal amount) {
    Customer customer = authService.getLoggedInCustomer();
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidAmountException("Amount must be greater than zero.");
    }
    List<Repayment> repayments = customer.receiveMoney(amount, repository);
    repository.save(customer);
    return repayments;
  }

  @Override
  public void withdraw(BigDecimal amount) {
    Customer customer = authService.getLoggedInCustomer();
    customer.withdraw(amount);
    repository.save(customer);
  }

  @Override
  public TransferResult transfer(String targetName, BigDecimal amount) {
    Customer sender = authService.getLoggedInCustomer();
    if (targetName == null || targetName.trim().isEmpty()) {
      throw new InvalidAmountException("Target username cannot be empty.");
    }
    if (sender.getName().equalsIgnoreCase(targetName)) {
      throw new InvalidAmountException("Cannot transfer to self.");
    }

    Customer receiver =
        repository
            .findByName(targetName)
            .orElseGet(
                () -> {
                  Customer newCustomer = Customer.builder().name(targetName).build();
                  repository.save(newCustomer);
                  return newCustomer;
                });

    return transferService.transfer(sender, receiver, amount, repository);
  }

  @Override
  public List<DebtInfo> getDebtsOwedToOthers(Customer customer) {
    return customer.getDebts().entrySet().stream()
        .map(e -> new DebtInfo(e.getKey(), e.getValue()))
        .sorted(Comparator.comparing(DebtInfo::getCounterparty, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  @Override
  public List<DebtInfo> getDebtsOwedFromOthers(Customer customer) {
    return repository.findAll().stream()
        .filter(c -> c.getDebts().containsKey(customer.getName()))
        .map(c -> new DebtInfo(c.getName(), c.getDebts().get(customer.getName())))
        .sorted(Comparator.comparing(DebtInfo::getCounterparty, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }
}
