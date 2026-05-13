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

    /**
     * A single click sequence (chain of slot indices to click in order). For our PICKUP-based
     * swap protocol a sequence is either 2 slots (pickup source, deposit into empty target) or
     * 3 slots (pickup target, pickup source — which swaps because items differ, pickup target
     * again to deposit). Same-content swaps are filtered out before they reach this list.
     */
    public record ClickChain(int[] slots) {}

    private Sorter() {}

    public static List<ClickChain> plan(Region region,
                                        List<ItemStack> currentInRange,
                                        SortCriterion criterion,
                                        SortMethod method) {
        if (currentInRange.size() != region.size()) {
            throw new IllegalArgumentException(
                "currentInRange size " + currentInRange.size() + " != region size " + region.size());
        }
        List<ItemStack> target = computeTarget(region, currentInRange, criterion, method);
        return selectionSort(region, currentInRange, target);
    }

    private static List<ItemStack> computeTarget(Region region,
                                                  List<ItemStack> current,
                                                  SortCriterion criterion,
                                                  SortMethod method) {
        if (method == SortMethod.COMPACT) {
            return compactTarget(region, current);
        }
        List<ItemStack> sortedStacks = sortNonEmpty(current, criterion);
        List<Integer> targetSlots = layoutSlots(region, sortedStacks.size(), method);
        return buildTargetArray(region, sortedStacks, targetSlots);
    }

    private static List<ItemStack> sortNonEmpty(List<ItemStack> current, SortCriterion criterion) {
        List<ItemStack> sorted = new ArrayList<>();
        for (ItemStack stack : current) {
            if (!stack.isEmpty()) sorted.add(stack);
        }
        Comparator<ItemStack> comparator = Comparators.forCriterion(criterion);
        sorted.sort(comparator);
        return sorted;
    }

    private static List<ItemStack> compactTarget(Region region, List<ItemStack> current) {
        List<ItemStack> target = new ArrayList<>(region.size());
        for (ItemStack stack : current) {
            if (!stack.isEmpty()) target.add(stack);
        }
        while (target.size() < region.size()) {
            target.add(ItemStack.EMPTY);
        }
        return target;
    }

    private static List<Integer> layoutSlots(Region region, int n, SortMethod method) {
        List<Integer> result = new ArrayList<>(n);
        return switch (method) {
            case HORIZONTAL -> horizontalLayout(region, n);
            case VERTICAL -> verticalLayout(region, n);
            case COMPACT -> horizontalLayout(region, n);
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

    private static List<ItemStack> buildTargetArray(Region region,
                                                     List<ItemStack> sortedStacks,
                                                     List<Integer> targetSlots) {
        List<ItemStack> target = new ArrayList<>(region.size());
        for (int i = 0; i < region.size(); i++) {
            target.add(ItemStack.EMPTY);
        }
        for (int i = 0; i < sortedStacks.size() && i < targetSlots.size(); i++) {
            int slot = targetSlots.get(i);
            target.set(slot - region.slotStart(), sortedStacks.get(i));
        }
        return target;
    }

    private static List<ClickChain> selectionSort(Region region,
                                                   List<ItemStack> current,
                                                   List<ItemStack> target) {
        List<ItemStack> working = new ArrayList<>(current);
        List<ClickChain> chains = new ArrayList<>();
        int n = working.size();
        for (int i = 0; i < n; i++) {
            ItemStack expected = target.get(i);
            ItemStack actual = working.get(i);
            if (sameContent(actual, expected)) continue;

            int sourceJ = findFirstMatch(working, expected, i + 1);
            if (sourceJ == -1) continue;

            int slotI = region.slotStart() + i;
            int slotJ = region.slotStart() + sourceJ;
            ItemStack stackJ = working.get(sourceJ);

            if (actual.isEmpty()) {
                chains.add(new ClickChain(new int[]{slotJ, slotI}));
                working.set(i, stackJ);
                working.set(sourceJ, ItemStack.EMPTY);
            } else if (sameItemType(actual, stackJ)) {
                int scratchIdx = findEmptyScratch(working, i, sourceJ);
                if (scratchIdx == -1) {
                    continue;
                }
                int slotScratch = region.slotStart() + scratchIdx;
                chains.add(new ClickChain(new int[]{
                    slotI, slotScratch, slotJ, slotI, slotScratch, slotJ
                }));
                working.set(i, stackJ);
                working.set(sourceJ, actual);
            } else {
                chains.add(new ClickChain(new int[]{slotI, slotJ, slotI}));
                working.set(i, stackJ);
                working.set(sourceJ, actual);
            }
        }
        return chains;
    }

    private static boolean sameContent(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        return ItemStack.isSameItemSameComponents(a, b) && a.getCount() == b.getCount();
    }

    private static boolean sameItemType(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return false;
        return ItemStack.isSameItemSameComponents(a, b);
    }

    private static int findFirstMatch(List<ItemStack> working, ItemStack expected, int startIdx) {
        if (expected.isEmpty()) return -1;
        for (int j = startIdx; j < working.size(); j++) {
            if (sameContent(working.get(j), expected)) return j;
        }
        return -1;
    }

    private static int findEmptyScratch(List<ItemStack> working, int slotI, int slotJ) {
        for (int k = 0; k < working.size(); k++) {
            if (k == slotI || k == slotJ) continue;
            if (working.get(k).isEmpty()) return k;
        }
        return -1;
    }
}
