package fr.horizonsmp.inventorysorter.client.config;

import fr.horizonsmp.inventorysorter.client.container.ContainerProfile;
import fr.horizonsmp.inventorysorter.client.sort.SortCriterion;
import fr.horizonsmp.inventorysorter.client.sort.SortMethod;
import java.util.LinkedHashMap;
import java.util.Map;

public record ModConfig(
    SortCriterion currentCriterion,
    SortMethod currentMethod,
    boolean includeHotbar,
    boolean mergePartialStacks,
    Map<String, Boolean> enabledScreens
) {

    public static ModConfig defaults() {
        Map<String, Boolean> screens = new LinkedHashMap<>();
        screens.put(ContainerProfile.SCREEN_PLAYER, true);
        screens.put(ContainerProfile.SCREEN_CHEST, true);
        screens.put(ContainerProfile.SCREEN_SHULKER, true);
        screens.put(ContainerProfile.SCREEN_HOPPER, true);
        screens.put(ContainerProfile.SCREEN_DISPENSER, true);
        return new ModConfig(SortCriterion.NAME, SortMethod.HORIZONTAL, false, true, screens);
    }

    public boolean isScreenEnabled(String screenId) {
        return enabledScreens.getOrDefault(screenId, true);
    }

    public ModConfig withCriterion(SortCriterion criterion) {
        return new ModConfig(criterion, currentMethod, includeHotbar, mergePartialStacks, enabledScreens);
    }

    public ModConfig withMethod(SortMethod method) {
        return new ModConfig(currentCriterion, method, includeHotbar, mergePartialStacks, enabledScreens);
    }

    public ModConfig withIncludeHotbar(boolean includeHotbar) {
        return new ModConfig(currentCriterion, currentMethod, includeHotbar, mergePartialStacks, enabledScreens);
    }

    public ModConfig withMergePartialStacks(boolean mergePartialStacks) {
        return new ModConfig(currentCriterion, currentMethod, includeHotbar, mergePartialStacks, enabledScreens);
    }

    public ModConfig withScreenEnabled(String screenId, boolean enabled) {
        Map<String, Boolean> updated = new LinkedHashMap<>(enabledScreens);
        updated.put(screenId, enabled);
        return new ModConfig(currentCriterion, currentMethod, includeHotbar, mergePartialStacks, updated);
    }
}
