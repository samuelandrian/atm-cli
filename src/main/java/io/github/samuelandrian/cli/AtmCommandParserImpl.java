package io.github.samuelandrian.cli;

import io.github.samuelandrian.application.DebtInfo;
import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.model.Repayment;
import io.github.samuelandrian.domain.service.TransferResult;
import java.math.BigDecimal;
import java.util.List;

public class AtmCommandParserImpl implements AtmCommandParser {

  @Override
  public Command parse(String inputLine) {
    if (inputLine == null || inputLine.trim().isEmpty()) {
      return (authService, atmService) -> CommandResult.success("");
    }

    String[] tokens = inputLine.trim().split("\\s+");
    String commandName = tokens[0].toLowerCase();

    switch (commandName) {
      case "login":
        if (tokens.length != 2) {
          return (authService, atmService) -> CommandResult.error("Error: Usage: login [name]");
        }
        String loginName = tokens[1];
        return (authService, atmService) -> {
          Customer customer = authService.login(loginName);
          StringBuilder sb = new StringBuilder();
          sb.append("Hello, ").append(customer.getName()).append("!\n");
          sb.append("Your balance is $").append(formatMoney(customer.getBalance()));

          List<DebtInfo> owedTo = atmService.getDebtsOwedToOthers(customer);
          for (DebtInfo debt : owedTo) {
            sb.append("\nOwed $")
                .append(formatMoney(debt.getAmount()))
                .append(" to ")
                .append(debt.getCounterparty());
          }

          List<DebtInfo> owedFrom = atmService.getDebtsOwedFromOthers(customer);
          for (DebtInfo debt : owedFrom) {
            sb.append("\nOwed $")
                .append(formatMoney(debt.getAmount()))
                .append(" from ")
                .append(debt.getCounterparty());
          }

          return CommandResult.success(sb.toString());
        };

      case "deposit":
        if (tokens.length != 2) {
          return (authService, atmService) -> CommandResult.error("Error: Usage: deposit [amount]");
        }
        try {
          BigDecimal depositAmount = new BigDecimal(tokens[1]);
          return (authService, atmService) -> {
            List<Repayment> repayments = atmService.deposit(depositAmount);
            Customer customer = authService.getLoggedInCustomer();
            StringBuilder sb = new StringBuilder();
            for (Repayment r : repayments) {
              sb.append("Transferred $")
                  .append(formatMoney(r.getAmount()))
                  .append(" to ")
                  .append(r.getTo())
                  .append("\n");
            }
            sb.append("Your balance is $").append(formatMoney(customer.getBalance()));

            List<DebtInfo> owedTo = atmService.getDebtsOwedToOthers(customer);
            for (DebtInfo debt : owedTo) {
              sb.append("\nOwed $")
                  .append(formatMoney(debt.getAmount()))
                  .append(" to ")
                  .append(debt.getCounterparty());
            }
            return CommandResult.success(sb.toString());
          };
        } catch (NumberFormatException e) {
          return (authService, atmService) -> CommandResult.error("Error: Invalid deposit amount.");
        }

      case "withdraw":
        if (tokens.length != 2) {
          return (authService, atmService) ->
              CommandResult.error("Error: Usage: withdraw [amount]");
        }
        try {
          BigDecimal withdrawAmount = new BigDecimal(tokens[1]);
          return (authService, atmService) -> {
            atmService.withdraw(withdrawAmount);
            Customer customer = authService.getLoggedInCustomer();
            StringBuilder sb = new StringBuilder();
            sb.append("Your balance is $").append(formatMoney(customer.getBalance()));

            List<DebtInfo> owedTo = atmService.getDebtsOwedToOthers(customer);
            for (DebtInfo debt : owedTo) {
              sb.append("\nOwed $")
                  .append(formatMoney(debt.getAmount()))
                  .append(" to ")
                  .append(debt.getCounterparty());
            }
            return CommandResult.success(sb.toString());
          };
        } catch (NumberFormatException e) {
          return (authService, atmService) ->
              CommandResult.error("Error: Invalid withdraw amount.");
        }

      case "transfer":
        if (tokens.length != 3) {
          return (authService, atmService) ->
              CommandResult.error("Error: Usage: transfer [target] [amount]");
        }
        String target = tokens[1];
        try {
          BigDecimal transferAmount = new BigDecimal(tokens[2]);
          return (authService, atmService) -> {
            TransferResult res = atmService.transfer(target, transferAmount);
            Customer customer = authService.getLoggedInCustomer();
            StringBuilder sb = new StringBuilder();

            if (res.getCashTransferred().compareTo(BigDecimal.ZERO) > 0) {
              sb.append("Transferred $")
                  .append(formatMoney(res.getCashTransferred()))
                  .append(" to ")
                  .append(target)
                  .append("\n");
            }

            sb.append("Your balance is $").append(formatMoney(customer.getBalance()));

            if (res.getDebtCreated().compareTo(BigDecimal.ZERO) > 0) {
              sb.append("\nOwed $")
                  .append(formatMoney(res.getDebtCreated()))
                  .append(" to ")
                  .append(target);
            }

            List<DebtInfo> owedFrom = atmService.getDebtsOwedFromOthers(customer);
            for (DebtInfo debt : owedFrom) {
              if (debt.getCounterparty().equalsIgnoreCase(target)) {
                sb.append("\nOwed $")
                    .append(formatMoney(debt.getAmount()))
                    .append(" from ")
                    .append(debt.getCounterparty());
              }
            }

            return CommandResult.success(sb.toString());
          };
        } catch (NumberFormatException e) {
          return (authService, atmService) ->
              CommandResult.error("Error: Invalid transfer amount.");
        }

      case "logout":
        if (tokens.length != 1) {
          return (authService, atmService) -> CommandResult.error("Error: Usage: logout");
        }
        return (authService, atmService) -> {
          String name = authService.logout();
          return CommandResult.success("Goodbye, " + name + "!");
        };

      default:
        return (authService, atmService) ->
            CommandResult.error(
                "Error: Invalid command. Supported commands are: login, deposit, withdraw, transfer, logout.");
    }
  }

  private static String formatMoney(BigDecimal amount) {
    if (amount == null) {
      return "0";
    }
    if (amount.compareTo(BigDecimal.ZERO) == 0) {
      return "0";
    }
    BigDecimal stripped = amount.stripTrailingZeros();
    if (stripped.scale() <= 0) {
      return stripped.toPlainString();
    }
    return stripped.toPlainString();
  }
}
