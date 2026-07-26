package io.github.samuelandrian.domain.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Repayment {
  private final String to;
  private final BigDecimal amount;
}
