package com.trading.nevcoin.discovery.domain;

import java.util.regex.Pattern;

public final class SolanaMintAddress {

    private static final Pattern BASE58_MINT = Pattern.compile("[1-9A-HJ-NP-Za-km-z]{32,44}");

    private SolanaMintAddress() {
    }

    public static boolean isValid(String value) {
        return value != null && BASE58_MINT.matcher(value.trim()).matches();
    }

    public static String requireValid(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException("A valid Solana Base58 mint address is required");
        }
        return value.trim();
    }
}
