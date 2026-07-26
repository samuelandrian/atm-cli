package io.github.samuelandrian.cli;

import static org.junit.jupiter.api.Assertions.*;

import io.github.samuelandrian.application.AtmService;
import io.github.samuelandrian.application.AuthService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AtmConsoleTest {

  @Test
  void testConsoleLoopNormalAndExit() {
    AuthService authService = null; // Mocks not strictly needed as parser is mocked
    AtmService atmService = null;

    AtmCommandParser parser = input -> (auth, atm) -> CommandResult.success("Parsed: " + input);

    String simulatedInput = "login Alice\nexit\n";
    ByteArrayInputStream in =
        new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(out, true, StandardCharsets.UTF_8);

    AtmConsole console = new AtmConsoleImpl(authService, atmService, parser, in, printStream);
    console.start();

    String result = out.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    assertTrue(result.contains("Parsed: login Alice"));
    assertFalse(result.contains("Parsed: exit")); // Exit should break before parsing/executing
  }

  @Test
  void testConsoleLoopQuit() {
    AtmCommandParser parser = input -> (auth, atm) -> CommandResult.success("Parsed: " + input);

    String simulatedInput = "quit\n";
    ByteArrayInputStream in =
        new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(out, true, StandardCharsets.UTF_8);

    AtmConsole console = new AtmConsoleImpl(null, null, parser, in, printStream);
    console.start();

    String result = out.toString(StandardCharsets.UTF_8);
    assertTrue(result.isEmpty());
  }

  @Test
  void testConsoleLoopEmptyLine() {
    AtmCommandParser parser = input -> (auth, atm) -> CommandResult.success("Parsed: " + input);

    String simulatedInput = "\n\nquit\n";
    ByteArrayInputStream in =
        new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(out, true, StandardCharsets.UTF_8);

    AtmConsole console = new AtmConsoleImpl(null, null, parser, in, printStream);
    console.start();

    String result = out.toString(StandardCharsets.UTF_8);
    assertTrue(result.isEmpty());
  }

  @Test
  void testConsoleLoopCommandError() {
    AtmCommandParser parser = input -> (auth, atm) -> CommandResult.error("Error output");

    String simulatedInput = "invalidcmd\nexit\n";
    ByteArrayInputStream in =
        new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(out, true, StandardCharsets.UTF_8);

    AtmConsole console = new AtmConsoleImpl(null, null, parser, in, printStream);
    console.start();

    String result = out.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    assertTrue(result.contains("Error output"));
  }

  @Test
  void testConsoleLoopCommandThrowsException() {
    AtmCommandParser parser =
        input ->
            (auth, atm) -> {
              throw new RuntimeException("Unexpected error");
            };

    String simulatedInput = "throwcmd\nexit\n";
    ByteArrayInputStream in =
        new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(out, true, StandardCharsets.UTF_8);

    AtmConsole console = new AtmConsoleImpl(null, null, parser, in, printStream);
    console.start();

    String result = out.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    assertTrue(result.contains("Error: Unexpected error"));
  }

  @Test
  void testConsoleLoopReaderThrowsIOException() {
    AtmCommandParser parser = input -> (auth, atm) -> CommandResult.success("");

    InputStream in =
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("Simulated read error");
          }
        };
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(out, true, StandardCharsets.UTF_8);

    AtmConsole console = new AtmConsoleImpl(null, null, parser, in, printStream);
    console.start();

    String result = out.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    assertTrue(result.contains("Error reading input: Simulated read error"));
  }
}
