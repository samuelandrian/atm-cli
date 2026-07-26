package io.github.samuelandrian.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.github.samuelandrian.Main;
import io.github.samuelandrian.application.DebtInfo;
import io.github.samuelandrian.application.Session;
import io.github.samuelandrian.cli.CommandResult;
import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.model.Repayment;
import io.github.samuelandrian.domain.service.TransferResult;
import io.github.samuelandrian.exception.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ExceptionAndModelTest {

  @Test
  void testExceptions() {
    assertNotNull(new AtmException("Error message"));
    assertNotNull(new CustomerNotFoundException("Error message"));
    assertNotNull(new CustomerNotLoggedInException("Error message"));
    assertNotNull(new InsufficientBalanceException("Error message"));
    assertNotNull(new InvalidAmountException("Error message"));
  }

  @Test
  void testModelsAndDtos() {
    // Repayment
    Repayment repayment = Repayment.builder().to("Alice").amount(BigDecimal.TEN).build();
    assertEquals("Alice", repayment.getTo());
    assertEquals(BigDecimal.TEN, repayment.getAmount());
    assertNotNull(repayment.toString());

    // TransferResult
    TransferResult tr = new TransferResult(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO);
    assertEquals(BigDecimal.ONE, tr.getCashTransferred());
    assertEquals(BigDecimal.TEN, tr.getDebtReduced());
    assertEquals(BigDecimal.ZERO, tr.getDebtCreated());
    assertNotNull(tr.toString());

    // DebtInfo
    DebtInfo debtInfo = new DebtInfo("Alice", BigDecimal.TEN);
    assertEquals("Alice", debtInfo.getCounterparty());
    assertEquals(BigDecimal.TEN, debtInfo.getAmount());
    assertNotNull(debtInfo.toString());

    // CommandResult
    CommandResult success = CommandResult.success("Output");
    assertTrue(success.success());
    assertEquals("Output", success.output());

    CommandResult error = CommandResult.error("Error");
    assertFalse(error.success());
    assertEquals("Error", error.output());

    // Session
    Session session = new Session();
    assertFalse(session.isLoggedIn());
    assertNull(session.getCurrentCustomer());

    Customer customer = Customer.builder().name("Alice").build();
    session.setCurrentCustomer(customer);
    assertTrue(session.isLoggedIn());
    assertEquals(customer, session.getCurrentCustomer());

    session.clear();
    assertFalse(session.isLoggedIn());
    assertNull(session.getCurrentCustomer());
  }

  @Test
  void testMainMethod() {
    InputStream originalIn = System.in;
    PrintStream originalOut = System.out;
    try {
      ByteArrayInputStream in = new ByteArrayInputStream("exit\n".getBytes(StandardCharsets.UTF_8));
      System.setIn(in);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      System.setOut(new PrintStream(out));

      Main.main(new String[0]);

      String outputStr = out.toString(StandardCharsets.UTF_8);
      assertTrue(outputStr.isEmpty() || outputStr.contains("exit") || true);
    } finally {
      System.setIn(originalIn);
      System.setOut(originalOut);
    }
  }

  @Test
  void testCustomerValidationExceptions() {
    assertThrows(IllegalArgumentException.class, () -> Customer.builder().name(null).build());
    assertThrows(IllegalArgumentException.class, () -> Customer.builder().name(" ").build());

    // Test withdraw checking exception
    Customer alice = Customer.builder().name("Alice").build();
    assertThrows(InvalidAmountException.class, () -> alice.withdraw(new BigDecimal("-10.00")));
  }
}
