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
    private static final int CONTAINER_TOOLBAR_Y_OFFSET = 4;
    private static final int PLAYER_TOOLBAR_Y_FROM_BOTTOM = 95;
    private static final int PLAYER_ONLY_TOOLBAR_Y_FROM_BOTTOM = 95;

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
            int containerY = topPos + CONTAINER_TOOLBAR_Y_OFFSET;
            attachToolbar(screen, baseX, containerY, profile, SortToolbarFactory.Target.CONTAINER);

            int playerY = topPos + imageHeight - PLAYER_TOOLBAR_Y_FROM_BOTTOM;
            attachToolbar(screen, baseX, playerY, profile, SortToolbarFactory.Target.PLAYER);
        } else {
            int playerY = topPos + imageHeight - PLAYER_ONLY_TOOLBAR_Y_FROM_BOTTOM;
            attachToolbar(screen, baseX, playerY, profile, SortToolbarFactory.Target.PLAYER);
        }
    }

    private static void attachToolbar(AbstractContainerScreen<?> screen,
                                       int x, int y,
                                       ContainerProfile profile,
                                       SortToolbarFactory.Target target) {
        for (AbstractWidget widget : SortToolbarFactory.create(x, y, profile, target)) {
            Screens.getWidgets(screen).add(widget);
        }
    }
}
