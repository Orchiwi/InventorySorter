package fr.horizonsmp.inventorysorter.client.screen;

import fr.horizonsmp.inventorysorter.client.ClientState;
import fr.horizonsmp.inventorysorter.client.SortAction;
import fr.horizonsmp.inventorysorter.client.container.ContainerProfile;
import fr.horizonsmp.inventorysorter.client.sort.SortCriterion;
import fr.horizonsmp.inventorysorter.client.sort.SortMethod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class SortToolbarFactory {

    public static final int BUTTON_SIZE = 12;
    public static final int BUTTON_GAP = 1;
    public static final int TOOLBAR_WIDTH = BUTTON_SIZE * 4 + BUTTON_GAP * 3;

    public enum Target { CONTAINER, PLAYER }

    private SortToolbarFactory() {}

    public static List<AbstractWidget> create(int x, int y, ContainerProfile profile, Target target) {
        List<AbstractWidget> widgets = new ArrayList<>(4);
        int step = BUTTON_SIZE + BUTTON_GAP;
        widgets.add(buildCriterionButton(x, y));
        widgets.add(buildSortButton(x + step, y, profile, target, SortMethod.VERTICAL));
        widgets.add(buildSortButton(x + 2 * step, y, profile, target, SortMethod.COMPACT));
        widgets.add(buildSortButton(x + 3 * step, y, profile, target, SortMethod.HORIZONTAL));
        return widgets;
    }

    private static Button buildCriterionButton(int x, int y) {
        SortCriterion current = ClientState.get().config().currentCriterion();
        Button button = Button.builder(criterionGlyph(current), b -> cycleCriterion(b))
            .pos(x, y)
            .size(BUTTON_SIZE, BUTTON_SIZE)
            .tooltip(criterionTooltip(current))
            .build();
        return button;
    }

    private static Button buildSortButton(int x, int y, ContainerProfile profile, Target target, SortMethod method) {
        return Button.builder(methodGlyph(method), b -> applySort(profile, target, method))
            .pos(x, y)
            .size(BUTTON_SIZE, BUTTON_SIZE)
            .tooltip(methodTooltip(method))
            .build();
    }

    private static void applySort(ContainerProfile profile, Target target, SortMethod method) {
        ClientState state = ClientState.get();
        state.update(cfg -> cfg.withMethod(method));
        if (target == Target.CONTAINER) {
            SortAction.runOnContainer(profile);
        } else {
            boolean includeHotbar = state.config().includeHotbar();
            SortAction.runOnPlayerMain(profile, includeHotbar);
        }
    }

    private static void cycleCriterion(Button button) {
        ClientState state = ClientState.get();
        SortCriterion[] holder = new SortCriterion[1];
        state.update(cfg -> {
            SortCriterion next = cfg.currentCriterion().next();
            holder[0] = next;
            return cfg.withCriterion(next);
        });
        button.setMessage(criterionGlyph(holder[0]));
        button.setTooltip(criterionTooltip(holder[0]));
    }

    private static MutableComponent criterionGlyph(SortCriterion criterion) {
        return Component.literal(switch (criterion) {
            case NAME -> "N";
            case TYPE -> "T";
            case CATEGORY -> "C";
            case RARITY_QUANTITY -> "R";
        });
    }

    private static MutableComponent methodGlyph(SortMethod method) {
        return Component.literal(switch (method) {
            case VERTICAL -> "↕";
            case COMPACT -> "≡";
            case HORIZONTAL -> "↔";
        });
    }

    private static Tooltip criterionTooltip(SortCriterion criterion) {
        return Tooltip.create(
            Component.translatable("inventorysorter.button.criterion.title",
                Component.translatable(criterion.translationKey())),
            Component.translatable("inventorysorter.button.criterion.hint"));
    }

    private static Tooltip methodTooltip(SortMethod method) {
        return Tooltip.create(
            Component.translatable("inventorysorter.button.method.title",
                Component.translatable(method.translationKey())),
            Component.translatable("inventorysorter.button.method.hint"));
    }
}
