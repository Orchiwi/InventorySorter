package fr.horizonsmp.inventorysorter.client.keybinding;

import com.mojang.blaze3d.platform.InputConstants;
import fr.horizonsmp.inventorysorter.client.ClientState;
import fr.horizonsmp.inventorysorter.client.SortAction;
import fr.horizonsmp.inventorysorter.client.container.ContainerProfile;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.lwjgl.glfw.GLFW;

public final class SortKeybinding {

    private SortKeybinding() {}

    public static void register() {
        KeyMapping mapping = new KeyMapping(
            "key.inventorysorter.sort",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KeyMapping.Category.INVENTORY
        );
        KeyMappingHelper.registerKeyMapping(mapping);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (mapping.consumeClick()) {
                handleSort();
            }
        });
    }

    private static void handleSort() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) return;
        ContainerProfile profile = ContainerProfile.detect(menu);
        if (profile.screenId().equals(ContainerProfile.SCREEN_UNKNOWN)) return;
        if (!ClientState.get().config().isScreenEnabled(profile.screenId())) return;
        if (profile.hasContainer()) {
            SortAction.runOnContainer(profile);
        } else {
            boolean includeHotbar = ClientState.get().config().includeHotbar();
            SortAction.runOnPlayerMain(profile, includeHotbar);
        }
    }
}
