package io.github.samuelandrian.cli.enums;

import java.util.Arrays;
import java.util.Optional;

public enum CommandType {
  LOGIN("login"),
  DEPOSIT("deposit"),
  WITHDRAW("withdraw"),
  TRANSFER("transfer"),
  LOGOUT("logout");

  private final String keyword;

  CommandType(String keyword) {
    this.keyword = keyword;
  }

  public String getKeyword() {
    return keyword;
  }

  public static Optional<CommandType> fromKeyword(String keyword) {
    if (keyword == null) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(c -> c.keyword.equalsIgnoreCase(keyword.trim()))
        .findFirst();
  }
}
