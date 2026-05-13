package fr.horizonsmp.inventorysorter.client.sort;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class Comparators {

    private static final int UNKNOWN_GROUP_INDEX = Integer.MAX_VALUE;

    private static volatile Map<Item, Integer> creativeGroupIndex;

    private Comparators() {}

    public static Comparator<ItemStack> forCriterion(SortCriterion criterion) {
        return switch (criterion) {
            case NAME -> byName().thenComparing(byItemId()).thenComparing(byNbtSubKey());
            case TYPE -> byItemId().thenComparing(byNbtSubKey());
            case CATEGORY -> byCreativeCategory().thenComparing(byItemId()).thenComparing(byNbtSubKey());
            case RARITY_QUANTITY ->
                byRarityDesc().thenComparing(byCountDesc()).thenComparing(byItemId()).thenComparing(byNbtSubKey());
        };
    }

    private static Comparator<ItemStack> byName() {
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.PRIMARY);
        return (a, b) -> collator.compare(a.getHoverName().getString(), b.getHoverName().getString());
    }

    private static Comparator<ItemStack> byItemId() {
        return Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    private static Comparator<ItemStack> byCreativeCategory() {
        return Comparator.comparingInt(stack -> categoryIndex(stack.getItem()));
    }

    private static Comparator<ItemStack> byRarityDesc() {
        return Comparator.<ItemStack>comparingInt(stack -> stack.getRarity().ordinal()).reversed();
    }

    private static Comparator<ItemStack> byCountDesc() {
        return Comparator.<ItemStack>comparingInt(ItemStack::getCount).reversed();
    }

    private static Comparator<ItemStack> byNbtSubKey() {
        return Comparator.comparing(Comparators::nbtSubKey);
    }

    private static int categoryIndex(Item item) {
        Map<Item, Integer> index = creativeGroupIndex;
        if (index == null) {
            synchronized (Comparators.class) {
                index = creativeGroupIndex;
                if (index == null) {
                    index = buildCreativeGroupIndex();
                    creativeGroupIndex = index;
                }
            }
        }
        return index.getOrDefault(item, UNKNOWN_GROUP_INDEX);
    }

    private static Map<Item, Integer> buildCreativeGroupIndex() {
        Map<Item, Integer> result = new HashMap<>();
        List<CreativeModeTab> tabs = CreativeModeTabs.tabs();
        for (int i = 0; i < tabs.size(); i++) {
            CreativeModeTab tab = tabs.get(i);
            for (ItemStack stack : tab.getDisplayItems()) {
                result.putIfAbsent(stack.getItem(), i);
            }
        }
        return result;
    }

    private static String nbtSubKey(ItemStack stack) {
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) {
            return enchantmentSignature(stored);
        }
        ItemEnchantments applied = stack.getEnchantments();
        if (applied != null && !applied.isEmpty()) {
            return enchantmentSignature(applied);
        }
        Object customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            return "name:" + customName.toString().toLowerCase(Locale.ROOT);
        }
        return stack.getComponents().toString();
    }

    private static String enchantmentSignature(ItemEnchantments enchantments) {
        List<String> entries = new ArrayList<>(enchantments.size());
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            String id = entry.getKey().unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("");
            entries.add(id + ':' + entry.getIntValue());
        }
        entries.sort(String::compareTo);
        return "ench:" + String.join("|", entries);
    }
}
