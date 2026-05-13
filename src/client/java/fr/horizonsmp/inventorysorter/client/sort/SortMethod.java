package fr.horizonsmp.inventorysorter.client.sort;

public enum SortMethod {
    HORIZONTAL,
    VERTICAL,
    COMPACT;

    public SortMethod next() {
        SortMethod[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String translationKey() {
        return "inventorysorter.method." + name().toLowerCase();
    }
}
