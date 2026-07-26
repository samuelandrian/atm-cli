package io.github.samuelandrian.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.repository.CustomerRepository;
import io.github.samuelandrian.domain.service.TransferResult;
import io.github.samuelandrian.domain.service.TransferService;
import io.github.samuelandrian.domain.service.TransferServiceImpl;
import io.github.samuelandrian.infrastructure.repository.InMemoryCustomerRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransferServiceTest {

  private CustomerRepository repository;
  private TransferService transferService;

  @BeforeEach
  void setUp() {
    repository = new InMemoryCustomerRepository();
    transferService = new TransferServiceImpl();
  }

  private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
    assertEquals(0, expected.compareTo(actual), "Expected " + expected + " but was " + actual);
  }

  @Test
  void testTransferSufficientBalance() {
    Customer alice = Customer.builder().name("Alice").build();
    alice.setBalance(new BigDecimal("100.00"));
    repository.save(alice);

    Customer bob = Customer.builder().name("Bob").build();
    repository.save(bob);

    TransferResult result =
        transferService.transfer(alice, bob, new BigDecimal("40.00"), repository);

    assertBigDecimalEquals(new BigDecimal("40.00"), result.getCashTransferred());
    assertBigDecimalEquals(BigDecimal.ZERO, result.getDebtReduced());
    assertBigDecimalEquals(BigDecimal.ZERO, result.getDebtCreated());

    assertBigDecimalEquals(new BigDecimal("60.00"), alice.getBalance());
    assertBigDecimalEquals(new BigDecimal("40.00"), bob.getBalance());
  }

  @Test
  void testTransferInsufficientBalanceCreatesDebt() {
    Customer bob = Customer.builder().name("Bob").build();
    bob.setBalance(new BigDecimal("30.00"));
    repository.save(bob);

    Customer alice = Customer.builder().name("Alice").build();
    repository.save(alice);

    TransferResult result =
        transferService.transfer(bob, alice, new BigDecimal("100.00"), repository);

    assertBigDecimalEquals(new BigDecimal("30.00"), result.getCashTransferred());
    assertBigDecimalEquals(BigDecimal.ZERO, result.getDebtReduced());
    assertBigDecimalEquals(new BigDecimal("70.00"), result.getDebtCreated());

    assertBigDecimalEquals(BigDecimal.ZERO, bob.getBalance());
    assertBigDecimalEquals(new BigDecimal("30.00"), alice.getBalance());
    assertBigDecimalEquals(new BigDecimal("70.00"), bob.getDebts().get("Alice"));
  }

  @Test
  void testTransferOffsetsExistingDebt() {
    Customer alice = Customer.builder().name("Alice").build();
    alice.setBalance(new BigDecimal("210.00"));
    repository.save(alice);

    Customer bob = Customer.builder().name("Bob").build();
    // Bob owes Alice 40
    bob.addDebt("Alice", new BigDecimal("40.00"));
    repository.save(bob);

    // Alice transfers 30 to Bob. Since Bob owes Alice 40, this should reduce Bob's debt to 10
    TransferResult result =
        transferService.transfer(alice, bob, new BigDecimal("30.00"), repository);

    assertBigDecimalEquals(BigDecimal.ZERO, result.getCashTransferred());
    assertBigDecimalEquals(new BigDecimal("30.00"), result.getDebtReduced());
    assertBigDecimalEquals(BigDecimal.ZERO, result.getDebtCreated());

    assertBigDecimalEquals(
        new BigDecimal("210.00"), alice.getBalance()); // Alice's balance remains 210
    assertBigDecimalEquals(BigDecimal.ZERO, bob.getBalance()); // Bob's balance remains 0
    assertBigDecimalEquals(
        new BigDecimal("10.00"),
        bob.getDebts().get("Alice")); // Bob's debt to Alice is reduced to 10
  }
}
