package com.smartfreezer.entity;

public enum ZoneType {
    UPPER("上层酸奶区"),
    MIDDLE("中层熟食区"),
    LOWER("下层鲜肉区");

    private final String displayName;

    ZoneType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
