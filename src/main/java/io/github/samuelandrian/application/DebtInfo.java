package io.github.samuelandrian.application;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class DebtInfo {
  private final String counterparty;
  private final BigDecimal amount;
}
