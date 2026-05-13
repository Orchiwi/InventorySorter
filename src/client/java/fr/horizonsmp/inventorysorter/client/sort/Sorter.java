package fr.horizonsmp.inventorysorter.client.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class Sorter {

    public record Region(int slotStart, int slotEndInclusive, int cols, int rows) {
        public int size() {
            return slotEndInclusive - slotStart + 1;
        }
    }

    public record SourceTarget(int sourceSlot, int targetSlot) {}

    private record Entry(int slot, ItemStack stack) {}

    private Sorter() {}

    public static List<SourceTarget> plan(Region region,
                                          List<ItemStack> currentInRange,
                                          SortCriterion criterion,
                                          SortMethod method) {
        if (currentInRange.size() != region.size()) {
            throw new IllegalArgumentException(
                "currentInRange size " + currentInRange.size() + " != region size " + region.size());
        }

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < currentInRange.size(); i++) {
            ItemStack stack = currentInRange.get(i);
            if (!stack.isEmpty()) {
                entries.add(new Entry(region.slotStart() + i, stack));
            }
        }

        Comparator<ItemStack> comparator = Comparators.forCriterion(criterion);
        entries.sort((a, b) -> comparator.compare(a.stack(), b.stack()));

        List<ItemStack> sortedStacks = entries.stream().map(Entry::stack).toList();
        List<Integer> targetSlots = computeTargetSlots(region, sortedStacks, method);

        List<SourceTarget> assignments = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            int source = entries.get(i).slot();
            int target = targetSlots.get(i);
            if (source != target) {
                assignments.add(new SourceTarget(source, target));
            }
        }
        return assignments;
    }

    private static List<Integer> computeTargetSlots(Region region, List<ItemStack> sortedStacks, SortMethod method) {
        int n = sortedStacks.size();
        return switch (method) {
            case HORIZONTAL -> horizontalLayout(region, n);
            case VERTICAL -> verticalLayout(region, n);
            case GROUPED -> groupedLayout(region, sortedStacks);
        };
    }

    private static List<Integer> horizontalLayout(Region region, int n) {
        List<Integer> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(region.slotStart() + i);
        }
        return result;
    }

    private static List<Integer> verticalLayout(Region region, int n) {
        List<Integer> result = new ArrayList<>(n);
        int cols = region.cols();
        int rows = region.rows();
        for (int k = 0; k < n; k++) {
            int row = k % rows;
            int col = k / rows;
            result.add(region.slotStart() + row * cols + col);
        }
        return result;
    }

    private static List<Integer> groupedLayout(Region region, List<ItemStack> sortedStacks) {
        int n = sortedStacks.size();
        if (n == 0) return List.of();
        List<Integer> groupBoundaries = new ArrayList<>();
        groupBoundaries.add(0);
        for (int i = 1; i < n; i++) {
            if (sortedStacks.get(i).getItem() != sortedStacks.get(i - 1).getItem()) {
                groupBoundaries.add(i);
            }
        }
        int totalGroups = groupBoundaries.size();
        int requiredSlots = n + Math.max(0, totalGroups - 1);
        if (requiredSlots > region.size()) {
            return horizontalLayout(region, n);
        }
        List<Integer> result = new ArrayList<>(n);
        int cursor = region.slotStart();
        for (int g = 0; g < totalGroups; g++) {
            int groupStart = groupBoundaries.get(g);
            int groupEnd = (g + 1 < totalGroups) ? groupBoundaries.get(g + 1) : n;
            for (int i = groupStart; i < groupEnd; i++) {
                result.add(cursor++);
            }
            if (g + 1 < totalGroups) {
                cursor++;
            }
        }
        return result;
    }
}
