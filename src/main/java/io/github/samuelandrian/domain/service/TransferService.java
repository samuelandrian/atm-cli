package io.github.samuelandrian.domain.service;

import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.repository.CustomerRepository;
import java.math.BigDecimal;

public interface TransferService {
  TransferResult transfer(
      Customer sender, Customer receiver, BigDecimal amount, CustomerRepository repository);
}
