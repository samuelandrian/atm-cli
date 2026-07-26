package io.github.samuelandrian.cli;

import io.github.samuelandrian.application.DebtInfo;
import io.github.samuelandrian.cli.enums.CommandType;
import io.github.samuelandrian.domain.model.Customer;
import io.github.samuelandrian.domain.model.Repayment;
import io.github.samuelandrian.domain.service.TransferResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class AtmCommandParserImpl implements AtmCommandParser {

  @Override
  public Command parse(String inputLine) {
    if (inputLine == null || inputLine.trim().isEmpty()) {
      return (authService, atmService) -> CommandResult.success("");
    }

    String[] tokens = inputLine.trim().split("\\s+");
    String firstToken = tokens[0];

    Optional<CommandType> commandTypeOpt = CommandType.fromKeyword(firstToken);
    if (commandTypeOpt.isEmpty()) {
      return (authService, atmService) ->
          CommandResult.error(
              "Error: Invalid command. Supported commands are: login, deposit, withdraw, transfer, logout.");
    }

    CommandType commandType = commandTypeOpt.get();

    return switch (commandType) {
      case LOGIN -> {
        if (tokens.length != 2) {
          yield (authService, atmService) -> CommandResult.error("Error: Usage: login [name]");
        }
        String loginName = tokens[1];
        yield (authService, atmService) -> {
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
      }

      case DEPOSIT -> {
        if (tokens.length != 2) {
          yield (authService, atmService) -> CommandResult.error("Error: Usage: deposit [amount]");
        }
        try {
          BigDecimal depositAmount = new BigDecimal(tokens[1]);
          yield (authService, atmService) -> {
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
          yield (authService, atmService) -> CommandResult.error("Error: Invalid deposit amount.");
        }
      }

      case WITHDRAW -> {
        if (tokens.length != 2) {
          yield (authService, atmService) -> CommandResult.error("Error: Usage: withdraw [amount]");
        }
        try {
          BigDecimal withdrawAmount = new BigDecimal(tokens[1]);
          yield (authService, atmService) -> {
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
          yield (authService, atmService) -> CommandResult.error("Error: Invalid withdraw amount.");
        }
      }

      case TRANSFER -> {
        if (tokens.length != 3) {
          yield (authService, atmService) ->
              CommandResult.error("Error: Usage: transfer [target] [amount]");
        }
        String target = tokens[1];
        try {
          BigDecimal transferAmount = new BigDecimal(tokens[2]);
          yield (authService, atmService) -> {
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
          yield (authService, atmService) -> CommandResult.error("Error: Invalid transfer amount.");
        }
      }

      case LOGOUT -> {
        if (tokens.length != 1) {
          yield (authService, atmService) -> CommandResult.error("Error: Usage: logout");
        }
        yield (authService, atmService) -> {
          String name = authService.logout();
          return CommandResult.success("Goodbye, " + name + "!");
        };
      }
    };
  }

  private static String formatMoney(BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) == 0) {
      return "0";
    }
    return amount.stripTrailingZeros().toPlainString();
  }
}
