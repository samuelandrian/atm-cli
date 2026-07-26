package io.github.samuelandrian.cli;

import static org.junit.jupiter.api.Assertions.*;

import io.github.samuelandrian.application.*;
import io.github.samuelandrian.cli.enums.CommandType;
import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.repository.CustomerRepository;
import io.github.samuelandrian.domain.service.TransferService;
import io.github.samuelandrian.domain.service.TransferServiceImpl;
import io.github.samuelandrian.infrastructure.repository.InMemoryCustomerRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AtmCommandParserTest {

  private AuthService authService;
  private AtmService atmService;
  private AtmCommandParser parser;
  private CustomerRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryCustomerRepository();
    TransferService transferService = new TransferServiceImpl();
    Session session = new Session();
    authService = new AuthServiceImpl(repository, session);
    atmService = new AtmServiceImpl(repository, transferService, authService);
    parser = new AtmCommandParserImpl();
  }

  @Test
  void testCommandTypeEnum() {
    assertEquals(CommandType.LOGIN, CommandType.fromKeyword("login").orElseThrow());
    assertEquals(CommandType.DEPOSIT, CommandType.fromKeyword("deposit").orElseThrow());
    assertEquals(CommandType.WITHDRAW, CommandType.fromKeyword("withdraw").orElseThrow());
    assertEquals(CommandType.TRANSFER, CommandType.fromKeyword("transfer").orElseThrow());
    assertEquals(CommandType.LOGOUT, CommandType.fromKeyword("logout").orElseThrow());
    assertTrue(CommandType.fromKeyword("invalid").isEmpty());
    assertTrue(CommandType.fromKeyword(null).isEmpty());
  }

  @Test
  void testParseEmptyOrNull() {
    Command cmdNull = parser.parse(null);
    assertTrue(cmdNull.execute(authService, atmService).success());
    assertEquals("", cmdNull.execute(authService, atmService).output());

    Command cmdEmpty = parser.parse("   ");
    assertTrue(cmdEmpty.execute(authService, atmService).success());
    assertEquals("", cmdEmpty.execute(authService, atmService).output());
  }

  @Test
  void testInvalidCommand() {
    Command cmd = parser.parse("unknown command");
    CommandResult result = cmd.execute(authService, atmService);
    assertFalse(result.success());
    assertTrue(result.output().contains("Invalid command"));
  }

  @Test
  void testLoginParsing() {
    // Valid
    Command cmd = parser.parse("login Alice");
    CommandResult res = cmd.execute(authService, atmService);
    assertTrue(res.success());
    assertTrue(res.output().contains("Hello, Alice!"));

    // Logged in user with debts owed to others
    Customer bob = Customer.builder().name("Bob").build();
    bob.addDebt("Alice", BigDecimal.TEN);
    repository.save(bob);

    // Logged in user with debts owed from others
    Customer alice = repository.findByName("Alice").orElseThrow();
    alice.addDebt("Bob", BigDecimal.ONE); // Just an entry, but Bob owes Alice 10
    repository.save(alice);

    // Logout alice
    authService.logout();

    // Login Bob
    Command cmd2 = parser.parse("login Bob");
    CommandResult res2 = cmd2.execute(authService, atmService);
    assertTrue(res2.success());
    assertTrue(res2.output().contains("Hello, Bob!"));
    assertTrue(res2.output().contains("Owed $10 to Alice"));

    // Invalid args
    Command cmdErr1 = parser.parse("login");
    CommandResult resErr1 = cmdErr1.execute(authService, atmService);
    assertFalse(resErr1.success());
    assertTrue(resErr1.output().contains("Usage: login"));

    Command cmdErr2 = parser.parse("login Alice Bob");
    CommandResult resErr2 = cmdErr2.execute(authService, atmService);
    assertFalse(resErr2.success());
    assertTrue(resErr2.output().contains("Usage: login"));
  }

  @Test
  void testDepositParsing() {
    authService.login("Alice");

    // Valid
    Command cmd = parser.parse("deposit 100");
    CommandResult res = cmd.execute(authService, atmService);
    assertTrue(res.success());
    assertTrue(res.output().contains("Your balance is $100"));

    // Valid deposit paying debt
    Customer alice = authService.getLoggedInCustomer();
    alice.addDebt("Bob", new BigDecimal("40.00"));
    repository.save(alice);

    Command cmd2 = parser.parse("deposit 50");
    CommandResult res2 = cmd2.execute(authService, atmService);
    assertTrue(res2.success());
    assertTrue(res2.output().contains("Transferred $40 to Bob"));
    assertTrue(res2.output().contains("Your balance is $110"));

    // Invalid args
    Command cmdErr1 = parser.parse("deposit");
    CommandResult resErr1 = cmdErr1.execute(authService, atmService);
    assertFalse(resErr1.success());
    assertTrue(resErr1.output().contains("Usage: deposit"));

    Command cmdErr2 = parser.parse("deposit 100 200");
    CommandResult resErr2 = cmdErr2.execute(authService, atmService);
    assertFalse(resErr2.success());
    assertTrue(resErr2.output().contains("Usage: deposit"));

    // Invalid format
    Command cmdErr3 = parser.parse("deposit abc");
    CommandResult resErr3 = cmdErr3.execute(authService, atmService);
    assertFalse(resErr3.success());
    assertTrue(resErr3.output().contains("Invalid deposit amount"));
  }

  @Test
  void testWithdrawParsing() {
    authService.login("Alice");
    atmService.deposit(new BigDecimal("100.00"));

    // Valid
    Command cmd = parser.parse("withdraw 30");
    CommandResult res = cmd.execute(authService, atmService);
    assertTrue(res.success());
    assertTrue(res.output().contains("Your balance is $70"));

    // Valid withdraw with active debt (should display debt list)
    Customer alice = authService.getLoggedInCustomer();
    alice.addDebt("Bob", BigDecimal.TEN);
    repository.save(alice);

    Command cmd2 = parser.parse("withdraw 10");
    CommandResult res2 = cmd2.execute(authService, atmService);
    assertTrue(res2.success());
    assertTrue(res2.output().contains("Your balance is $60"));
    assertTrue(res2.output().contains("Owed $10 to Bob"));

    // Invalid args
    Command cmdErr1 = parser.parse("withdraw");
    CommandResult resErr1 = cmdErr1.execute(authService, atmService);
    assertFalse(resErr1.success());
    assertTrue(resErr1.output().contains("Usage: withdraw"));

    Command cmdErr2 = parser.parse("withdraw 50 100");
    CommandResult resErr2 = cmdErr2.execute(authService, atmService);
    assertFalse(resErr2.success());
    assertTrue(resErr2.output().contains("Usage: withdraw"));

    // Invalid format
    Command cmdErr3 = parser.parse("withdraw abc");
    CommandResult resErr3 = cmdErr3.execute(authService, atmService);
    assertFalse(resErr3.success());
    assertTrue(resErr3.output().contains("Invalid withdraw amount"));
  }

  @Test
  void testTransferParsing() {
    authService.login("Alice");
    atmService.deposit(new BigDecimal("100.00"));

    // Valid
    Command cmd = parser.parse("transfer Bob 40");
    CommandResult res = cmd.execute(authService, atmService);
    assertTrue(res.success());
    assertTrue(res.output().contains("Transferred $40 to Bob"));
    assertTrue(res.output().contains("Your balance is $60"));

    // Valid creating debt
    Command cmd2 = parser.parse("transfer Bob 80");
    CommandResult res2 = cmd2.execute(authService, atmService);
    assertTrue(res2.success());
    assertTrue(res2.output().contains("Transferred $60 to Bob"));
    assertTrue(res2.output().contains("Your balance is $0"));
    assertTrue(res2.output().contains("Owed $20 to Bob"));

    // Invalid args
    Command cmdErr1 = parser.parse("transfer");
    CommandResult resErr1 = cmdErr1.execute(authService, atmService);
    assertFalse(resErr1.success());
    assertTrue(resErr1.output().contains("Usage: transfer"));

    Command cmdErr2 = parser.parse("transfer Bob");
    CommandResult resErr2 = cmdErr2.execute(authService, atmService);
    assertFalse(resErr2.success());
    assertTrue(resErr2.output().contains("Usage: transfer"));

    Command cmdErr3 = parser.parse("transfer Bob 40 50");
    CommandResult resErr3 = cmdErr3.execute(authService, atmService);
    assertFalse(resErr3.success());
    assertTrue(resErr3.output().contains("Usage: transfer"));

    // Invalid format
    Command cmdErr4 = parser.parse("transfer Bob abc");
    CommandResult resErr4 = cmdErr4.execute(authService, atmService);
    assertFalse(resErr4.success());
    assertTrue(resErr4.output().contains("Invalid transfer amount"));

    // Offset transfer target owes money to sender
    Customer bobModel = repository.findByName("Bob").orElseThrow();
    bobModel.addDebt("Alice", new BigDecimal("40.00"));
    repository.save(bobModel);

    Command cmdOffset = parser.parse("transfer Bob 30");
    CommandResult resOffset = cmdOffset.execute(authService, atmService);
    assertTrue(resOffset.success());
    assertTrue(resOffset.output().contains("Owed $10 from Bob"));

    // False branch: Charlie owes Alice 10, Alice transfers to Bob
    Customer charlieModel = Customer.builder().name("Charlie").build();
    charlieModel.addDebt("Alice", new BigDecimal("10.00"));
    repository.save(charlieModel);

    Command cmdFalseBranch = parser.parse("transfer Bob 5");
    CommandResult resFalseBranch = cmdFalseBranch.execute(authService, atmService);
    assertTrue(resFalseBranch.success());
    assertFalse(resFalseBranch.output().contains("from Charlie"));
  }

  @Test
  void testLogoutParsing() {
    authService.login("Alice");

    // Valid
    Command cmd = parser.parse("logout");
    CommandResult res = cmd.execute(authService, atmService);
    assertTrue(res.success());
    assertTrue(res.output().contains("Goodbye, Alice!"));

    // Invalid args
    Command cmdErr = parser.parse("logout now");
    CommandResult resErr = cmdErr.execute(authService, atmService);
    assertFalse(resErr.success());
    assertTrue(resErr.output().contains("Usage: logout"));
  }
}
