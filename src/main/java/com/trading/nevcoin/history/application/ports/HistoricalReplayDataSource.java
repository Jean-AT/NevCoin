package com.trading.nevcoin.history.application.ports;

import com.trading.nevcoin.history.domain.ReplayRequest;
import com.trading.nevcoin.history.domain.ReplaySourceData;

public interface HistoricalReplayDataSource {
    ReplaySourceData load(ReplayRequest request);
}
