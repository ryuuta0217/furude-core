package com.ryuuta0217.furude.feature.tool;

public enum DiggerToolMode {
    OFF("オフ"),
    CHAIN_DESTRUCTION("一括破壊"),
    RANGED_MINING("範囲破壊");

    private final String displayName;

    DiggerToolMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
