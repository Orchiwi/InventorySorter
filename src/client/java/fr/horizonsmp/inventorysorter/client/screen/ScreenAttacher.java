package fr.horizonsmp.inventorysorter.client.screen;

import fr.horizonsmp.inventorysorter.client.ClientState;
import fr.horizonsmp.inventorysorter.client.container.ContainerProfile;
import fr.horizonsmp.inventorysorter.mixin.client.AbstractContainerScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class ScreenAttacher {

    private static final int RIGHT_MARGIN = 4;
    private static final int Y_OFFSET_TOP = 4;
    private static final int Y_OFFSET_PLAYER = 64;

    private ScreenAttacher() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) return;
            attach(container);
        });
    }

    private static void attach(AbstractContainerScreen<?> screen) {
        ContainerProfile profile = ContainerProfile.detect(screen.getMenu());
        if (profile.screenId().equals(ContainerProfile.SCREEN_UNKNOWN)) return;
        if (!ClientState.get().config().isScreenEnabled(profile.screenId())) return;

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int leftPos = accessor.inventorysorter$getLeftPos();
        int topPos = accessor.inventorysorter$getTopPos();
        int imageWidth = accessor.inventorysorter$getImageWidth();
        int imageHeight = accessor.inventorysorter$getImageHeight();

        int baseX = leftPos + imageWidth - SortToolbarFactory.TOOLBAR_WIDTH - RIGHT_MARGIN;

        if (profile.hasContainer()) {
            int containerY = topPos + Y_OFFSET_TOP;
            for (AbstractWidget widget : SortToolbarFactory.create(baseX, containerY, profile, SortToolbarFactory.Target.CONTAINER)) {
                Screens.getWidgets(screen).add(widget);
            }
            int playerY = topPos + imageHeight - Y_OFFSET_PLAYER;
            for (AbstractWidget widget : SortToolbarFactory.create(baseX, playerY, profile, SortToolbarFactory.Target.PLAYER)) {
                Screens.getWidgets(screen).add(widget);
            }
        } else {
            int playerY = topPos + Y_OFFSET_TOP;
            for (AbstractWidget widget : SortToolbarFactory.create(baseX, playerY, profile, SortToolbarFactory.Target.PLAYER)) {
                Screens.getWidgets(screen).add(widget);
            }
        }
    }
}
