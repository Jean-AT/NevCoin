package com.trading.nevcoin.paper.validation.application.ports;

import com.trading.nevcoin.paper.validation.domain.PaperValidationRequest;
import com.trading.nevcoin.paper.validation.domain.ReplayDataset;

public interface HistoricalReplayPort {
    ReplayDataset replay(PaperValidationRequest request);
}
