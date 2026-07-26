package io.github.samuelandrian.application;

import io.github.samuelandrian.domain.model.Customer;

public interface AuthService {
  Customer getLoggedInCustomer();

  Customer login(String name);

  String logout();

  boolean isLoggedIn();
}
