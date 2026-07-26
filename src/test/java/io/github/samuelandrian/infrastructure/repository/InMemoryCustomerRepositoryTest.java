package io.github.samuelandrian.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.*;

import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.repository.CustomerRepository;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryCustomerRepositoryTest {

  private CustomerRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryCustomerRepository();
  }

  @Test
  void testSaveAndFindByName() {
    Customer customer = Customer.builder().name("Alice").build();
    repository.save(customer);

    Optional<Customer> found = repository.findByName("Alice");
    assertTrue(found.isPresent());
    assertEquals("Alice", found.get().getName());

    Optional<Customer> notFound = repository.findByName("Bob");
    assertTrue(notFound.isEmpty());
  }

  @Test
  void testFindAll() {
    Customer c1 = Customer.builder().name("Alice").build();
    Customer c2 = Customer.builder().name("Bob").build();
    repository.save(c1);
    repository.save(c2);

    Collection<Customer> all = repository.findAll();
    assertEquals(2, all.size());
  }
}
