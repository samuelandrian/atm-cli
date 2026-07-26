package io.github.samuelandrian.domain.repository;

import io.github.samuelandrian.domain.model.Customer;
import java.util.Collection;
import java.util.Optional;

public interface CustomerRepository {
  Optional<Customer> findByName(String name);

  void save(Customer customer);

  Collection<Customer> findAll();
}
