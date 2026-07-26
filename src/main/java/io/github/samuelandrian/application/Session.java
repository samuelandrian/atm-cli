package io.github.samuelandrian.application;

import io.github.samuelandrian.domain.model.Customer;
import lombok.Data;

@Data
public class Session {
  private Customer currentCustomer;

  public boolean isLoggedIn() {
    return currentCustomer != null;
  }

  public void clear() {
    this.currentCustomer = null;
  }
}
