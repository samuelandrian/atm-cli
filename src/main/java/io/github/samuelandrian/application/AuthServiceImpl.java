package io.github.samuelandrian.application;

import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.repository.CustomerRepository;
import io.github.samuelandrian.exception.AtmException;
import io.github.samuelandrian.exception.CustomerNotLoggedInException;
import io.github.samuelandrian.exception.InvalidAmountException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private final CustomerRepository repository;
  private final Session session;

  @Override
  public Customer getLoggedInCustomer() {
    if (!session.isLoggedIn()) {
      throw new CustomerNotLoggedInException("No user logged in.");
    }
    return session.getCurrentCustomer();
  }

  @Override
  public Customer login(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new InvalidAmountException("Username cannot be empty.");
    }
    if (session.isLoggedIn()) {
      if (session.getCurrentCustomer().getName().equalsIgnoreCase(name)) {
        return session.getCurrentCustomer();
      } else {
        throw new AtmException(
            "Already logged in as "
                + session.getCurrentCustomer().getName()
                + ". Please logout first.");
      }
    }

    Customer customer =
        repository
            .findByName(name)
            .orElseGet(
                () -> {
                  Customer newCustomer = Customer.builder().name(name).build();
                  repository.save(newCustomer);
                  return newCustomer;
                });

    session.setCurrentCustomer(customer);
    return customer;
  }

  @Override
  public String logout() {
    Customer customer = getLoggedInCustomer();
    String name = customer.getName();
    session.clear();
    return name;
  }

  @Override
  public boolean isLoggedIn() {
    return session.isLoggedIn();
  }
}
