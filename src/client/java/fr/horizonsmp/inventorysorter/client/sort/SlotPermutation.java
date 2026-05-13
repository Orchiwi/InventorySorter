package fr.horizonsmp.inventorysorter.client.sort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SlotPermutation {

    private SlotPermutation() {}

    /**
     * Decompose a list of (source, target) moves into click chains executable with cursor-based
     * PICKUP semantics: pick up the source, then click each subsequent slot, ending either at a
     * target whose original slot was empty (chain) or back at the cycle start (closed cycle).
     */
    public static List<List<Integer>> decompose(List<Sorter.SourceTarget> moves) {
        Map<Integer, Integer> targetOfSource = new HashMap<>();
        Set<Integer> sources = new HashSet<>();
        for (Sorter.SourceTarget move : moves) {
            targetOfSource.put(move.sourceSlot(), move.targetSlot());
            sources.add(move.sourceSlot());
        }

        List<List<Integer>> chains = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        for (Sorter.SourceTarget move : moves) {
            int start = move.sourceSlot();
            if (visited.contains(start)) continue;
            chains.add(buildChain(start, targetOfSource, sources, visited));
        }
        return chains;
    }

    private static List<Integer> buildChain(int start,
                                            Map<Integer, Integer> targetOfSource,
                                            Set<Integer> sources,
                                            Set<Integer> visited) {
        List<Integer> chain = new ArrayList<>();
        chain.add(start);
        visited.add(start);
        int cursor = targetOfSource.get(start);
        while (true) {
            chain.add(cursor);
            if (cursor == start) {
                return chain;
            }
            if (!sources.contains(cursor) || visited.contains(cursor)) {
                return chain;
            }
            visited.add(cursor);
            cursor = targetOfSource.get(cursor);
        }
    }
}
