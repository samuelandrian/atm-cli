package io.github.samuelandrian.exception;

public class CustomerNotLoggedInException extends AtmException {
  public CustomerNotLoggedInException(String message) {
    super(message);
  }
}
