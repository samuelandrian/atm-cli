package io.github.samuelandrian.exception;

public class CustomerNotFoundException extends AtmException {
  public CustomerNotFoundException(String message) {
    super(message);
  }
}
