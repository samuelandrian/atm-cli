package io.github.samuelandrian.domain;

import static org.junit.jupiter.api.Assertions.*;

import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.model.Repayment;
import io.github.samuelandrian.domain.repository.CustomerRepository;
import io.github.samuelandrian.infrastructure.repository.InMemoryCustomerRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerTest {

  private CustomerRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryCustomerRepository();
  }

  @Test
  void testCustomerCreation() {
    Customer customer = Customer.builder().name("Alice").build();
    assertEquals("Alice", customer.getName());
    assertEquals(BigDecimal.ZERO, customer.getBalance());
    assertTrue(customer.getDebts().isEmpty());
  }

  @Test
  void testWithdrawal() {
    Customer customer = Customer.builder().name("Alice").build();
    customer.setBalance(new BigDecimal("100.00"));

    customer.withdraw(new BigDecimal("30.00"));
    assertEquals(new BigDecimal("70.00"), customer.getBalance());

    assertThrows(RuntimeException.class, () -> customer.withdraw(new BigDecimal("80.00")));
  }

  @Test
  void testAddAndReduceDebt() {
    Customer customer = Customer.builder().name("Bob").build();
    customer.addDebt("Alice", new BigDecimal("50.00"));
    assertEquals(new BigDecimal("50.00"), customer.getDebts().get("Alice"));

    customer.addDebt("Alice", new BigDecimal("20.00"));
    assertEquals(new BigDecimal("70.00"), customer.getDebts().get("Alice"));

    customer.reduceDebt("Alice", new BigDecimal("30.00"));
    assertEquals(new BigDecimal("40.00"), customer.getDebts().get("Alice"));

    customer.reduceDebt("Alice", new BigDecimal("50.00"));
    assertNull(customer.getDebts().get("Alice"));
  }

  @Test
  void testReceiveMoneyWithDebtsFIFO() {
    Customer bob = Customer.builder().name("Bob").build();
    repository.save(bob);

    Customer alice = Customer.builder().name("Alice").build();
    repository.save(alice);

    Customer charlie = Customer.builder().name("Charlie").build();
    repository.save(charlie);

    // Bob owes Alice 40 and Charlie 30
    bob.addDebt("Alice", new BigDecimal("40.00"));
    bob.addDebt("Charlie", new BigDecimal("30.00"));

    // Bob receives 50
    List<Repayment> repayments = bob.receiveMoney(new BigDecimal("50.00"), repository);

    // Bob should repay Alice 40 and Charlie 10 (FIFO order)
    assertEquals(2, repayments.size());
    assertEquals("Alice", repayments.get(0).getTo());
    assertEquals(new BigDecimal("40.00"), repayments.get(0).getAmount());
    assertEquals("Charlie", repayments.get(1).getTo());
    assertEquals(new BigDecimal("10.00"), repayments.get(1).getAmount());

    // Alice should have received 40
    Customer updatedAlice = repository.findByName("Alice").orElseThrow();
    assertEquals(new BigDecimal("40.00"), updatedAlice.getBalance());

    // Charlie should have received 10
    Customer updatedCharlie = repository.findByName("Charlie").orElseThrow();
    assertEquals(new BigDecimal("10.00"), updatedCharlie.getBalance());

    // Bob's remaining debt to Charlie should be 20, and to Alice should be 0 (removed)
    assertEquals(new BigDecimal("20.00"), bob.getDebts().get("Charlie"));
    assertNull(bob.getDebts().get("Alice"));
    assertEquals(BigDecimal.ZERO, bob.getBalance());
  }
}
