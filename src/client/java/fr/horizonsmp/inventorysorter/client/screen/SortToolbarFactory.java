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

    public static final int BUTTON_WIDTH = 18;
    public static final int BUTTON_HEIGHT = 18;
    public static final int BUTTON_GAP = 1;
    public static final int TOOLBAR_WIDTH = BUTTON_WIDTH * 3 + BUTTON_GAP * 2;

    public enum Target { CONTAINER, PLAYER }

    private SortToolbarFactory() {}

    public static List<AbstractWidget> create(int x, int y, ContainerProfile profile, Target target) {
        List<AbstractWidget> widgets = new ArrayList<>(3);
        widgets.add(buildCriterionButton(x, y));
        widgets.add(buildMethodButton(x + BUTTON_WIDTH + BUTTON_GAP, y));
        widgets.add(buildSortButton(x + 2 * (BUTTON_WIDTH + BUTTON_GAP), y, profile, target));
        return widgets;
    }

    private static Button buildCriterionButton(int x, int y) {
        SortCriterion current = ClientState.get().config().currentCriterion();
        Button button = Button.builder(criterionGlyph(current), b -> cycleCriterion(b))
            .pos(x, y)
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .tooltip(criterionTooltip(current))
            .build();
        return button;
    }

    private static Button buildMethodButton(int x, int y) {
        SortMethod current = ClientState.get().config().currentMethod();
        Button button = Button.builder(methodGlyph(current), b -> cycleMethod(b))
            .pos(x, y)
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .tooltip(methodTooltip(current))
            .build();
        return button;
    }

    private static Button buildSortButton(int x, int y, ContainerProfile profile, Target target) {
        return Button.builder(Component.literal("✓"), b -> {
                if (target == Target.CONTAINER) {
                    SortAction.runOnContainer(profile);
                } else {
                    boolean includeHotbar = ClientState.get().config().includeHotbar();
                    SortAction.runOnPlayerMain(profile, includeHotbar);
                }
            })
            .pos(x, y)
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(
                Component.translatable("inventorysorter.button.sort.title"),
                Component.translatable("inventorysorter.button.sort.hint")))
            .build();
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

    private static void cycleMethod(Button button) {
        ClientState state = ClientState.get();
        SortMethod[] holder = new SortMethod[1];
        state.update(cfg -> {
            SortMethod next = cfg.currentMethod().next();
            holder[0] = next;
            return cfg.withMethod(next);
        });
        button.setMessage(methodGlyph(holder[0]));
        button.setTooltip(methodTooltip(holder[0]));
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
            case HORIZONTAL -> "H";
            case VERTICAL -> "V";
            case GROUPED -> "G";
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
