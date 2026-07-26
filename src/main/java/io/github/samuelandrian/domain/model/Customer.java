package io.github.samuelandrian.domain.model;

import io.github.samuelandrian.domain.repository.CustomerRepository;
import io.github.samuelandrian.exception.InsufficientBalanceException;
import io.github.samuelandrian.exception.InvalidAmountException;
import java.math.BigDecimal;
import java.util.*;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Customer {
  private final String name;
  private BigDecimal balance;
  private final Map<String, BigDecimal> debts;

  @Builder
  public Customer(String name, BigDecimal balance, Map<String, BigDecimal> debts) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Customer name cannot be empty.");
    }
    this.name = name;
    this.balance = balance != null ? balance : BigDecimal.ZERO;
    this.debts = debts != null ? debts : new LinkedHashMap<>();
  }

  public Map<String, BigDecimal> getDebts() {
    return debts != null ? new LinkedHashMap<>(debts) : new LinkedHashMap<>();
  }

  public void addDebt(String creditorName, BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }
    BigDecimal currentDebt = debts.getOrDefault(creditorName, BigDecimal.ZERO);
    debts.put(creditorName, currentDebt.add(amount));
  }

  public void reduceDebt(String creditorName, BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }
    BigDecimal currentDebt = debts.get(creditorName);
    if (currentDebt != null) {
      BigDecimal newDebt = currentDebt.subtract(amount);
      if (newDebt.compareTo(BigDecimal.ZERO) <= 0) {
        debts.remove(creditorName);
      } else {
        debts.put(creditorName, newDebt);
      }
    }
  }

  public List<Repayment> receiveMoney(BigDecimal amount, CustomerRepository repository) {
    return receiveMoney(amount, repository, new HashSet<>());
  }

  private List<Repayment> receiveMoney(
      BigDecimal amount, CustomerRepository repository, Set<String> visited) {
    List<Repayment> repayments = new ArrayList<>();
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return repayments;
    }

    if (visited.contains(this.name)) {
      this.balance = this.balance.add(amount);
      return repayments;
    }
    visited.add(this.name);

    BigDecimal remaining = amount;
    List<String> creditorNames = new ArrayList<>(debts.keySet());

    for (String creditorName : creditorNames) {
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }
      BigDecimal debtAmount = debts.get(creditorName);
      if (debtAmount == null || debtAmount.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }

      BigDecimal payment = remaining.min(debtAmount);
      BigDecimal newDebt = debtAmount.subtract(payment);

      if (newDebt.compareTo(BigDecimal.ZERO) == 0) {
        debts.remove(creditorName);
      } else {
        debts.put(creditorName, newDebt);
      }

      remaining = remaining.subtract(payment);

      Customer creditor =
          repository
              .findByName(creditorName)
              .orElseGet(
                  () -> {
                    Customer c = Customer.builder().name(creditorName).build();
                    repository.save(c);
                    return c;
                  });

      Set<String> subVisited = new HashSet<>(visited);
      creditor.receiveMoney(payment, repository, subVisited);
      repository.save(creditor);

      repayments.add(Repayment.builder().to(creditorName).amount(payment).build());
    }

    if (remaining.compareTo(BigDecimal.ZERO) > 0) {
      this.balance = this.balance.add(remaining);
    }

    return repayments;
  }

  public void withdraw(BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidAmountException("Amount must be greater than zero.");
    }
    if (this.balance.compareTo(amount) < 0) {
      throw new InsufficientBalanceException("Insufficient funds.");
    }
    this.balance = this.balance.subtract(amount);
  }
}
