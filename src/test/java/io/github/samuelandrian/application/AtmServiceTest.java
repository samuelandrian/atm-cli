package io.github.samuelandrian.application;

import static org.junit.jupiter.api.Assertions.*;

import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.repository.CustomerRepository;
import io.github.samuelandrian.domain.service.TransferService;
import io.github.samuelandrian.domain.service.TransferServiceImpl;
import io.github.samuelandrian.exception.CustomerNotLoggedInException;
import io.github.samuelandrian.infrastructure.repository.InMemoryCustomerRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AtmServiceTest {

  private CustomerRepository repository;
  private AuthService authService;
  private AtmService atmService;

  @BeforeEach
  void setUp() {
    repository = new InMemoryCustomerRepository();
    TransferService transferService = new TransferServiceImpl();
    Session session = new Session();
    authService = new AuthServiceImpl(repository, session);
    atmService = new AtmServiceImpl(repository, transferService, authService);
  }

  @Test
  void testActionsRequireLogin() {
    assertThrows(
        CustomerNotLoggedInException.class, () -> atmService.deposit(new BigDecimal("10.00")));
    assertThrows(
        CustomerNotLoggedInException.class, () -> atmService.withdraw(new BigDecimal("10.00")));
    assertThrows(
        CustomerNotLoggedInException.class,
        () -> atmService.transfer("Bob", new BigDecimal("10.00")));
  }

  @Test
  void testDepositAndWithdrawal() {
    authService.login("Alice");

    atmService.deposit(new BigDecimal("100.00"));
    Customer alice = authService.getLoggedInCustomer();
    assertEquals(0, new BigDecimal("100.00").compareTo(alice.getBalance()));

    atmService.withdraw(new BigDecimal("30.00"));
    assertEquals(0, new BigDecimal("70.00").compareTo(alice.getBalance()));
  }

  @Test
  void testInvalidDeposit() {
    authService.login("Alice");
    assertThrows(RuntimeException.class, () -> atmService.deposit(new BigDecimal("-10.00")));
    assertThrows(RuntimeException.class, () -> atmService.deposit(BigDecimal.ZERO));
  }

  @Test
  void testTransferValidations() {
    authService.login("Alice");

    // Self transfer error
    assertThrows(RuntimeException.class, () -> atmService.transfer("Alice", BigDecimal.TEN));

    // Empty target error
    assertThrows(RuntimeException.class, () -> atmService.transfer(null, BigDecimal.TEN));
    assertThrows(RuntimeException.class, () -> atmService.transfer("   ", BigDecimal.TEN));

    // Valid transfer to a non-existent user Bob (should auto-create Bob)
    atmService.deposit(new BigDecimal("50.00"));
    assertNotNull(atmService.transfer("Bob", new BigDecimal("30.00")));
    assertTrue(repository.findByName("Bob").isPresent());
  }
}
