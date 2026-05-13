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
        List<ItemStack> sortedStacks = sortNonEmpty(currentInRange, criterion);
        List<ItemStack> target = layoutTarget(region, sortedStacks, method);
        return selectionSort(region, currentInRange, target);
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

    private static List<ItemStack> layoutTarget(Region region,
                                                 List<ItemStack> sortedStacks,
                                                 SortMethod method) {
        List<ItemStack> target = new ArrayList<>(region.size());
        for (int i = 0; i < region.size(); i++) target.add(ItemStack.EMPTY);

        if (sortedStacks.isEmpty()) return target;

        return switch (method) {
            case COMPACT -> compactLayout(region, sortedStacks, target);
            case HORIZONTAL -> groupedLayout(region, sortedStacks, target, rowMajorTraversal(region));
            case VERTICAL -> groupedLayout(region, sortedStacks, target, columnMajorTraversal(region));
        };
    }

    private static List<ItemStack> compactLayout(Region region,
                                                  List<ItemStack> sortedStacks,
                                                  List<ItemStack> target) {
        for (int i = 0; i < sortedStacks.size() && i < region.size(); i++) {
            target.set(i, sortedStacks.get(i));
        }
        return target;
    }

    private static List<ItemStack> groupedLayout(Region region,
                                                  List<ItemStack> sortedStacks,
                                                  List<ItemStack> target,
                                                  int[] traversal) {
        List<List<ItemStack>> groups = groupByItem(sortedStacks);
        int totalItems = sortedStacks.size();
        int trailingGaps = 0;
        for (int i = 0; i < groups.size() - 1; i++) {
            if (groups.get(i).size() > 1) trailingGaps++;
        }
        boolean useGaps = (totalItems + trailingGaps) <= region.size();

        int cursor = 0;
        int total = traversal.length;
        for (int g = 0; g < groups.size(); g++) {
            List<ItemStack> group = groups.get(g);
            for (ItemStack stack : group) {
                if (cursor >= total) return target;
                int slotAbs = traversal[cursor];
                target.set(slotAbs - region.slotStart(), stack);
                cursor++;
            }
            boolean hasNext = g < groups.size() - 1;
            if (hasNext && useGaps && group.size() > 1) {
                cursor++;
                if (cursor >= total) return target;
            }
        }
        return target;
    }

    private static List<List<ItemStack>> groupByItem(List<ItemStack> sortedStacks) {
        List<List<ItemStack>> groups = new ArrayList<>();
        for (ItemStack stack : sortedStacks) {
            if (!groups.isEmpty()
                && groups.get(groups.size() - 1).get(0).getItem() == stack.getItem()) {
                groups.get(groups.size() - 1).add(stack);
            } else {
                List<ItemStack> g = new ArrayList<>();
                g.add(stack);
                groups.add(g);
            }
        }
        return groups;
    }

    private static int[] rowMajorTraversal(Region region) {
        int n = region.size();
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            result[i] = region.slotStart() + i;
        }
        return result;
    }

    private static int[] columnMajorTraversal(Region region) {
        int n = region.size();
        int cols = region.cols();
        int rows = region.rows();
        int[] result = new int[n];
        for (int k = 0; k < n; k++) {
            int row = k % rows;
            int col = k / rows;
            result[k] = region.slotStart() + row * cols + col;
        }
        return result;
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
