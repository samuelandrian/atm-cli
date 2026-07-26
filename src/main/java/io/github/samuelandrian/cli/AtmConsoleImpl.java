package io.github.samuelandrian.cli;

import io.github.samuelandrian.application.AtmService;
import io.github.samuelandrian.application.AuthService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class AtmConsoleImpl implements AtmConsole {
  private final AuthService authService;
  private final AtmService atmService;
  private final AtmCommandParser parser;
  private final BufferedReader reader;
  private final PrintStream out;

  public AtmConsoleImpl(AuthService authService, AtmService atmService, AtmCommandParser parser) {
    this(authService, atmService, parser, System.in, System.out);
  }

  public AtmConsoleImpl(
      AuthService authService,
      AtmService atmService,
      AtmCommandParser parser,
      InputStream in,
      PrintStream out) {
    this.authService = authService;
    this.atmService = atmService;
    this.parser = parser;
    this.reader = new BufferedReader(new InputStreamReader(in));
    this.out = out;
  }

  @Override
  public void start() {
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
          continue;
        }
        if (trimmed.equalsIgnoreCase("exit") || trimmed.equalsIgnoreCase("quit")) {
          break;
        }

        Command command = parser.parse(trimmed);
        try {
          CommandResult result = command.execute(authService, atmService);
          if (result.success()) {
            if (!result.output().isEmpty()) {
              out.println(result.output());
              out.println(); // Print a blank line after output
            }
          } else {
            out.println(result.output());
            out.println(); // Print a blank line even on validation errors
          }
        } catch (Exception e) {
          out.println("Error: " + e.getMessage());
          out.println(); // Print a blank line even on runtime exceptions
        }
      }
    } catch (IOException e) {
      out.println("Error reading input: " + e.getMessage());
    }
  }
}
