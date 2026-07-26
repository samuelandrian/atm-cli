package io.github.samuelandrian.cli;

public record CommandResult(boolean success, String output) {
  public static CommandResult success(String output) {
    return new CommandResult(true, output);
  }

  public static CommandResult error(String errorMsg) {
    return new CommandResult(false, errorMsg);
  }
}
