package io.github.samuelandrian.exception;

public class InsufficientBalanceException extends AtmException {
  public InsufficientBalanceException(String message) {
    super(message);
  }
}
