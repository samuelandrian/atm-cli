package io.github.samuelandrian.cli;

import io.github.samuelandrian.application.AtmService;
import io.github.samuelandrian.application.AuthService;

@FunctionalInterface
public interface Command {
  CommandResult execute(AuthService authService, AtmService atmService);
}
