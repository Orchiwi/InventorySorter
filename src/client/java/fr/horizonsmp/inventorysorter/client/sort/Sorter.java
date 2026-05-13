package fr.horizonsmp.inventorysorter.client.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
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
        List<ItemStack> target = switch (method) {
            case COMPACT -> computeCompactTarget(region, currentInRange, criterion);
            case HORIZONTAL -> computeLinedTarget(region, currentInRange, criterion, true);
            case VERTICAL -> computeLinedTarget(region, currentInRange, criterion, false);
        };
        return selectionSort(region, currentInRange, target);
    }

    private static List<ItemStack> emptyTarget(Region region) {
        List<ItemStack> result = new ArrayList<>(region.size());
        for (int i = 0; i < region.size(); i++) result.add(ItemStack.EMPTY);
        return result;
    }

    private static List<ItemStack> computeCompactTarget(Region region,
                                                         List<ItemStack> current,
                                                         SortCriterion criterion) {
        List<ItemStack> nonEmpty = new ArrayList<>();
        for (ItemStack s : current) if (!s.isEmpty()) nonEmpty.add(s);
        nonEmpty.sort(Comparators.forCriterion(criterion));
        List<ItemStack> target = emptyTarget(region);
        for (int i = 0; i < nonEmpty.size() && i < region.size(); i++) {
            target.set(i, nonEmpty.get(i));
        }
        return target;
    }

    /**
     * Lay each item type on its own line (a full row for HORIZONTAL, a full
     * column for VERTICAL). When a type has more stacks than fit on a single
     * line it wraps onto subsequent lines. After a type's last stack the cursor
     * jumps to the start of the next line so the next type begins fresh.
     */
    private static List<ItemStack> computeLinedTarget(Region region,
                                                       List<ItemStack> current,
                                                       SortCriterion criterion,
                                                       boolean horizontal) {
        Map<Item, List<ItemStack>> stacksByItem = new LinkedHashMap<>();
        for (ItemStack stack : current) {
            if (stack.isEmpty()) continue;
            stacksByItem.computeIfAbsent(stack.getItem(), k -> new ArrayList<>()).add(stack);
        }
        List<ItemStack> target = emptyTarget(region);
        if (stacksByItem.isEmpty()) return target;

        Comparator<ItemStack> stackCmp = Comparators.forCriterion(criterion);
        for (List<ItemStack> stacks : stacksByItem.values()) stacks.sort(stackCmp);

        List<Item> orderedTypes = new ArrayList<>(stacksByItem.keySet());
        orderedTypes.sort((a, b) -> stackCmp.compare(
            stacksByItem.get(a).get(0),
            stacksByItem.get(b).get(0)));

        int rowSize = region.cols();
        int columnSize = region.rows();
        int lineSize = horizontal ? rowSize : columnSize;
        int lineCount = horizontal ? columnSize : rowSize;

        int lineIdx = 0;
        int posInLine = 0;

        for (Item item : orderedTypes) {
            if (lineIdx >= lineCount) break;
            List<ItemStack> stacks = stacksByItem.get(item);
            int placed = 0;
            while (placed < stacks.size() && lineIdx < lineCount) {
                int row;
                int col;
                if (horizontal) {
                    row = lineIdx;
                    col = posInLine;
                } else {
                    row = posInLine;
                    col = lineIdx;
                }
                target.set(row * rowSize + col, stacks.get(placed));
                placed++;
                posInLine++;
                if (posInLine >= lineSize) {
                    posInLine = 0;
                    lineIdx++;
                }
            }
            if (posInLine > 0) {
                posInLine = 0;
                lineIdx++;
            }
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
                if (scratchIdx == -1) continue;
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
