package fr.horizonsmp.inventorysorter.client.sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Ported from invtweaks.InvTweaksHandlerSorting (MC 1.12, MIT, by Jimeo Wan).
 *
 * <p>HORIZONTAL and VERTICAL synthesize rectangle rules via the same formula
 * the original mod uses, then apply each rule by filling its preferred slots
 * with stacks of the matching item type. COMPACT (= the original DEFAULT
 * method) sorts then packs without any rectangle. The final permutation is
 * produced by a selection sort that handles same-item collisions via a
 * scratch slot to avoid vanilla PICKUP's merge-instead-of-swap behaviour.
 */
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
            case HORIZONTAL -> computeRectangleTarget(region, currentInRange, criterion, true);
            case VERTICAL -> computeRectangleTarget(region, currentInRange, criterion, false);
        };
        return selectionSort(region, currentInRange, target);
    }

    private static List<ItemStack> computeCompactTarget(Region region,
                                                         List<ItemStack> current,
                                                         SortCriterion criterion) {
        List<ItemStack> nonEmpty = new ArrayList<>();
        for (ItemStack s : current) if (!s.isEmpty()) nonEmpty.add(s);
        nonEmpty.sort(Comparators.forCriterion(criterion));
        List<ItemStack> target = new ArrayList<>(region.size());
        for (int i = 0; i < region.size(); i++) {
            target.add(i < nonEmpty.size() ? nonEmpty.get(i) : ItemStack.EMPTY);
        }
        return target;
    }

    private static List<ItemStack> computeRectangleTarget(Region region,
                                                           List<ItemStack> current,
                                                           SortCriterion criterion,
                                                           boolean horizontal) {
        Map<Item, List<ItemStack>> stacksByItem = new LinkedHashMap<>();
        for (ItemStack s : current) {
            if (s.isEmpty()) continue;
            stacksByItem.computeIfAbsent(s.getItem(), k -> new ArrayList<>()).add(s);
        }
        List<ItemStack> empty = new ArrayList<>(region.size());
        for (int i = 0; i < region.size(); i++) empty.add(ItemStack.EMPTY);
        if (stacksByItem.isEmpty()) return empty;

        Comparator<ItemStack> stackCmp = Comparators.forCriterion(criterion);
        for (List<ItemStack> stacks : stacksByItem.values()) stacks.sort(stackCmp);

        int rowSize = region.cols();
        int columnSize = region.rows();
        int lineSize = horizontal ? rowSize : columnSize;

        List<Item> itemOrder = orderItemTypes(stacksByItem, lineSize, stackCmp);

        int distinctItems = stacksByItem.size();
        int spaceWidth;
        int spaceHeight;
        if (horizontal) {
            spaceHeight = 1;
            spaceWidth = Math.max(1, rowSize / divCeil(distinctItems, columnSize));
        } else {
            spaceWidth = 1;
            spaceHeight = Math.max(1, columnSize / divCeil(distinctItems, rowSize));
        }

        int row = 0;
        int col = 0;
        int maxRow = columnSize - 1;
        int maxCol = rowSize - 1;
        int availableSlots = region.size();
        int remainingStacks = 0;
        for (List<ItemStack> s : stacksByItem.values()) remainingStacks += s.size();

        List<ItemStack> target = empty;

        for (Item item : itemOrder) {
            List<ItemStack> stacks = stacksByItem.get(item);
            int count = stacks.size();
            int thisW = spaceWidth;
            int thisH = spaceHeight;

            while (count > thisH * thisW) {
                if (horizontal) {
                    if (col + thisW < maxCol) {
                        thisW = maxCol - col + 1;
                    } else if (row + thisH < maxRow) {
                        thisH++;
                    } else {
                        break;
                    }
                } else {
                    if (row + thisH < maxRow) {
                        thisH = maxRow - row + 1;
                    } else if (col + thisW < maxCol) {
                        thisW++;
                    } else {
                        break;
                    }
                }
            }

            if (horizontal && col + thisW == maxCol) {
                thisW++;
            } else if (!horizontal && row + thisH == maxRow) {
                thisH++;
            }

            placeRectangle(target, region, row, col, thisH, thisW, stacks, horizontal);

            availableSlots -= thisH * thisW;
            remainingStacks -= count;
            if (availableSlots >= remainingStacks) {
                if (horizontal) {
                    if (col + thisW + spaceWidth <= maxCol + 1) {
                        col += thisW;
                    } else {
                        col = 0;
                        row += thisH;
                    }
                } else {
                    if (row + thisH + spaceHeight <= maxRow + 1) {
                        row += thisH;
                    } else {
                        row = 0;
                        col += thisW;
                    }
                }
                if (row > maxRow || col > maxCol) break;
            } else {
                break;
            }
        }

        return target;
    }

    private static List<Item> orderItemTypes(Map<Item, List<ItemStack>> stacksByItem,
                                              int lineSize,
                                              Comparator<ItemStack> stackCmp) {
        List<Item> itemOrder = new ArrayList<>();
        List<Item> unordered = new ArrayList<>(stacksByItem.keySet());

        boolean foundOversized = true;
        while (foundOversized) {
            foundOversized = false;
            for (Item item : new ArrayList<>(unordered)) {
                if (stacksByItem.get(item).size() > lineSize) {
                    itemOrder.add(item);
                    unordered.remove(item);
                    foundOversized = true;
                    break;
                }
            }
        }
        unordered.sort((a, b) -> stackCmp.compare(
            stacksByItem.get(a).get(0),
            stacksByItem.get(b).get(0)));
        itemOrder.addAll(unordered);
        return itemOrder;
    }

    private static void placeRectangle(List<ItemStack> target,
                                        Region region,
                                        int startRow,
                                        int startCol,
                                        int rectH,
                                        int rectW,
                                        List<ItemStack> stacks,
                                        boolean horizontal) {
        int cols = region.cols();
        int placed = 0;
        int maxRow = region.rows() - 1;
        int maxCol = cols - 1;
        if (horizontal) {
            for (int r = startRow; r <= maxRow && r < startRow + rectH; r++) {
                for (int c = startCol; c <= maxCol && c < startCol + rectW; c++) {
                    if (placed >= stacks.size()) return;
                    target.set(r * cols + c, stacks.get(placed++));
                }
            }
        } else {
            for (int c = startCol; c <= maxCol && c < startCol + rectW; c++) {
                for (int r = startRow; r <= maxRow && r < startRow + rectH; r++) {
                    if (placed >= stacks.size()) return;
                    target.set(r * cols + c, stacks.get(placed++));
                }
            }
        }
    }

    private static int divCeil(int a, int b) {
        return (a + b - 1) / b;
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
