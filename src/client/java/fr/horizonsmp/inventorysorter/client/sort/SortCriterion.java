package fr.horizonsmp.inventorysorter.client.sort;

public enum SortCriterion {
    NAME,
    TYPE,
    CATEGORY,
    RARITY_QUANTITY;

    public SortCriterion next() {
        SortCriterion[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String translationKey() {
        return "inventorysorter.criterion." + name().toLowerCase();
    }
}
