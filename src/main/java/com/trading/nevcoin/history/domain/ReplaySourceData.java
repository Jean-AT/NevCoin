package com.trading.nevcoin.history.domain;

import com.trading.nevcoin.market.domain.MarketSnapshot;
import com.trading.nevcoin.market.domain.MarketTick;
import com.trading.nevcoin.signal.domain.CrossDomainSignal;
import com.trading.nevcoin.social.domain.RawSocialEvent;
import com.trading.nevcoin.social.domain.SocialEvent;
import com.trading.nevcoin.social.domain.SocialSignal;

import java.util.List;

/** Immutable input snapshot supplied by a replay data source. */
public record ReplaySourceData(
        List<RawSocialEvent> rawSocialEvents,
        List<SocialEvent> socialEvents,
        List<MarketTick> marketTicks,
        List<MarketSnapshot> marketSnapshots,
        List<SocialSignal> socialSignals,
        List<CrossDomainSignal> crossDomainSignals) {
    public ReplaySourceData {
        rawSocialEvents = rawSocialEvents == null ? List.of() : List.copyOf(rawSocialEvents);
        socialEvents = socialEvents == null ? List.of() : List.copyOf(socialEvents);
        marketTicks = marketTicks == null ? List.of() : List.copyOf(marketTicks);
        marketSnapshots = marketSnapshots == null ? List.of() : List.copyOf(marketSnapshots);
        socialSignals = socialSignals == null ? List.of() : List.copyOf(socialSignals);
        crossDomainSignals = crossDomainSignals == null ? List.of() : List.copyOf(crossDomainSignals);
    }
}
