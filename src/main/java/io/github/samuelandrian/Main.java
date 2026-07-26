package io.github.samuelandrian;

import io.github.samuelandrian.application.*;
import io.github.samuelandrian.cli.*;
import io.github.samuelandrian.domain.repository.CustomerRepository;
import io.github.samuelandrian.domain.service.TransferService;
import io.github.samuelandrian.domain.service.TransferServiceImpl;
import io.github.samuelandrian.infrastructure.repository.InMemoryCustomerRepository;

public class Main {
  public static void main(String[] args) {
    CustomerRepository repository = new InMemoryCustomerRepository();
    TransferService transferService = new TransferServiceImpl();
    Session session = new Session();

    AuthService authService = new AuthServiceImpl(repository, session);
    AtmService atmService = new AtmServiceImpl(repository, transferService, authService);

    AtmCommandParser parser = new AtmCommandParserImpl();
    AtmConsole commandLoop = new AtmConsoleImpl(authService, atmService, parser);
    commandLoop.start();
  }
}
