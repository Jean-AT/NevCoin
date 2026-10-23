package com.trading.nevcoin.paper.validation.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record EquityPoint(Instant observedAt, BigDecimal cashUsd, BigDecimal equityUsd, BigDecimal drawdownUsd) { }
