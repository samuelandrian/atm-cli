package io.github.samuelandrian.domain.service;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class TransferResult {
  private final BigDecimal cashTransferred;
  private final BigDecimal debtReduced;
  private final BigDecimal debtCreated;
}
