package com.bng.marketcoordination.economy;

public enum CommodityCategory {
    FOOD("food"),
    FUEL("fuel"),
    MATERIALS("materials"),
    TEXTILES("textiles"),
    TOOLS("tools"),
    MISC("misc");

    private final String id;

    CommodityCategory(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static CommodityCategory fromId(String id) {
        if (id == null) {
            return MISC;
        }
        for (CommodityCategory category : values()) {
            if (category.id.equalsIgnoreCase(id)) {
                return category;
            }
        }
        return MISC;
    }
}
