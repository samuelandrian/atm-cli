package io.github.samuelandrian.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.samuelandrian.application.AtmService;
import io.github.samuelandrian.application.AtmServiceImpl;
import io.github.samuelandrian.application.AuthService;
import io.github.samuelandrian.application.AuthServiceImpl;
import io.github.samuelandrian.application.Session;
import io.github.samuelandrian.cli.AtmCommandParser;
import io.github.samuelandrian.cli.AtmCommandParserImpl;
import io.github.samuelandrian.cli.AtmConsole;
import io.github.samuelandrian.cli.AtmConsoleImpl;
import io.github.samuelandrian.domain.repository.CustomerRepository;
import io.github.samuelandrian.domain.service.TransferService;
import io.github.samuelandrian.domain.service.TransferServiceImpl;
import io.github.samuelandrian.infrastructure.repository.InMemoryCustomerRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CLIIntegrationTest {

  @Test
  void testExampleSession() {
    // Setup dependencies
    CustomerRepository repository = new InMemoryCustomerRepository();
    TransferService transferService = new TransferServiceImpl();
    Session session = new Session();

    AuthService authService = new AuthServiceImpl(repository, session);
    AtmService atmService = new AtmServiceImpl(repository, transferService, authService);

    // Prepare the simulation input commands
    String input =
        """
        login Alice
        deposit 100
        logout
        login Bob
        deposit 80
        transfer Alice 50
        transfer Alice 100
        deposit 30
        logout
        login Alice
        transfer Bob 30
        logout
        login Bob
        deposit 100
        logout
        """;

    ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(out, true, StandardCharsets.UTF_8);

    // Start REPL loop
    AtmCommandParser parser = new AtmCommandParserImpl();
    AtmConsole loop = new AtmConsoleImpl(authService, atmService, parser, in, printStream);
    loop.start();

    // Retrieve actual output
    String actualOutput = out.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");

    // The expected output block matching the requirements
    String expectedOutput =
        """
        Hello, Alice!
        Your balance is $0

        Your balance is $100

        Goodbye, Alice!

        Hello, Bob!
        Your balance is $0

        Your balance is $80

        Transferred $50 to Alice
        Your balance is $30

        Transferred $30 to Alice
        Your balance is $0
        Owed $70 to Alice

        Transferred $30 to Alice
        Your balance is $0
        Owed $40 to Alice

        Goodbye, Bob!

        Hello, Alice!
        Your balance is $210
        Owed $40 from Bob

        Your balance is $210
        Owed $10 from Bob

        Goodbye, Alice!

        Hello, Bob!
        Your balance is $0
        Owed $10 to Alice

        Transferred $10 to Alice
        Your balance is $90

        Goodbye, Bob!
        """
            .replace("\r\n", "\n");

    assertEquals(expectedOutput.trim(), actualOutput.trim());
  }
}
