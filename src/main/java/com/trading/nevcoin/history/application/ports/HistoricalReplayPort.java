package com.trading.nevcoin.history.application.ports;

import com.trading.nevcoin.history.domain.ReplayDataset;
import com.trading.nevcoin.history.domain.ReplayRequest;

public interface HistoricalReplayPort {
    ReplayDataset replay(ReplayRequest request);
}
