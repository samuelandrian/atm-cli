package io.github.samuelandrian.infrastructure.repository;

import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.repository.CustomerRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryCustomerRepository implements CustomerRepository {
  private final Map<String, Customer> customers = new HashMap<>();

  @Override
  public Optional<Customer> findByName(String name) {
    if (name == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(customers.get(name.toLowerCase()));
  }

  @Override
  public void save(Customer customer) {
    if (customer != null && customer.getName() != null) {
      customers.put(customer.getName().toLowerCase(), customer);
    }
  }

  @Override
  public Collection<Customer> findAll() {
    return customers.values();
  }
}
