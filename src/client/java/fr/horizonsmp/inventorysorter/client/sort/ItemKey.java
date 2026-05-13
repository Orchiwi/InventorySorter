package fr.horizonsmp.inventorysorter.client.sort;

import java.util.Objects;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ItemKey {

    private final Item item;
    private final DataComponentMap components;
    private final int hash;

    private ItemKey(Item item, DataComponentMap components) {
        this.item = item;
        this.components = components;
        this.hash = Objects.hash(BuiltInRegistries.ITEM.getId(item), components);
    }

    public static ItemKey of(ItemStack stack) {
        return new ItemKey(stack.getItem(), stack.getComponents());
    }

    public Identifier itemId() {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public Item item() {
        return item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemKey other)) return false;
        return this.item == other.item && Objects.equals(this.components, other.components);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
