package com.trading.nevcoin.paper.validation.application.ports;

import com.trading.nevcoin.paper.validation.domain.PaperValidationResult;
import java.util.UUID;

public interface PaperValidationResultStore {
    UUID save(PaperValidationResult result);
}
