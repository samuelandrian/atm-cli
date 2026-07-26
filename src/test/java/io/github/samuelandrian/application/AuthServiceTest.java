package io.github.samuelandrian.application;

import static org.junit.jupiter.api.Assertions.*;

import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.repository.CustomerRepository;
import io.github.samuelandrian.exception.CustomerNotLoggedInException;
import io.github.samuelandrian.infrastructure.repository.InMemoryCustomerRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

  private CustomerRepository repository;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    repository = new InMemoryCustomerRepository();
    Session session = new Session();
    authService = new AuthServiceImpl(repository, session);
  }

  @Test
  void testLoginCreatesNewUser() {
    Customer customer = authService.login("Alice");
    assertNotNull(customer);
    assertEquals("Alice", customer.getName());
    assertEquals(BigDecimal.ZERO, customer.getBalance());

    assertTrue(repository.findByName("Alice").isPresent());
  }

  @Test
  void testLoginExistingUser() {
    Customer existing = Customer.builder().name("Bob").build();
    existing.setBalance(new BigDecimal("50.00"));
    repository.save(existing);

    Customer customer = authService.login("Bob");
    assertEquals("Bob", customer.getName());
    assertEquals(new BigDecimal("50.00"), customer.getBalance());
  }

  @Test
  void testActionsRequireLogin() {
    assertThrows(CustomerNotLoggedInException.class, () -> authService.logout());
  }

  @Test
  void testLoginLogoutCycle() {
    authService.login("Alice");
    assertTrue(authService.isLoggedIn());
    assertEquals("Alice", authService.getLoggedInCustomer().getName());

    String loggedOut = authService.logout();
    assertEquals("Alice", loggedOut);
    assertFalse(authService.isLoggedIn());
    assertThrows(CustomerNotLoggedInException.class, authService::getLoggedInCustomer);
  }

  @Test
  void testLoginValidationAndDuplicates() {
    // Empty / null login
    assertThrows(RuntimeException.class, () -> authService.login(null));
    assertThrows(RuntimeException.class, () -> authService.login("   "));

    // Login Alice
    Customer c1 = authService.login("Alice");

    // Login Alice again (should succeed and return same customer)
    Customer c2 = authService.login("Alice");
    assertEquals(c1, c2);

    // Login Bob while Alice is logged in (should fail)
    assertThrows(RuntimeException.class, () -> authService.login("Bob"));
  }
}
