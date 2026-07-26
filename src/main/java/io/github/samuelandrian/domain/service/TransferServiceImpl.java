package io.github.samuelandrian.domain.service;

import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.repository.CustomerRepository;
import io.github.samuelandrian.exception.InvalidAmountException;
import java.math.BigDecimal;

public class TransferServiceImpl implements TransferService {

  @Override
  public TransferResult transfer(
      Customer sender, Customer receiver, BigDecimal amount, CustomerRepository repository) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidAmountException("Amount must be greater than zero.");
    }
    if (sender.getName().equalsIgnoreCase(receiver.getName())) {
      throw new InvalidAmountException("Cannot transfer to self.");
    }

    BigDecimal remainingAmount = amount;
    BigDecimal debtReduced = BigDecimal.ZERO;

    // 1. Check if receiver owes sender. If yes, offset the transfer against that debt.
    BigDecimal receiverDebtToSender =
        receiver.getDebts().getOrDefault(sender.getName(), BigDecimal.ZERO);
    if (receiverDebtToSender.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal offset = remainingAmount.min(receiverDebtToSender);
      receiver.reduceDebt(sender.getName(), offset);
      repository.save(receiver);

      debtReduced = offset;
      remainingAmount = remainingAmount.subtract(offset);
    }

    BigDecimal cashTransferred = BigDecimal.ZERO;
    BigDecimal debtCreated = BigDecimal.ZERO;

    // 2. If there is still a remaining transfer amount, pay it using sender's balance or create
    // debt.
    if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal senderBalance = sender.getBalance();
      cashTransferred = remainingAmount.min(senderBalance);

      if (cashTransferred.compareTo(BigDecimal.ZERO) > 0) {
        sender.setBalance(senderBalance.subtract(cashTransferred));
        receiver.receiveMoney(cashTransferred, repository);
        repository.save(receiver);
      }

      debtCreated = remainingAmount.subtract(cashTransferred);
      if (debtCreated.compareTo(BigDecimal.ZERO) > 0) {
        sender.addDebt(receiver.getName(), debtCreated);
      }
    }

    repository.save(sender);
    return new TransferResult(cashTransferred, debtReduced, debtCreated);
  }
}
