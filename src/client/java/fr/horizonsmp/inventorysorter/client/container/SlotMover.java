package fr.horizonsmp.inventorysorter.client.container;

import fr.horizonsmp.inventorysorter.client.sort.SlotPermutation;
import fr.horizonsmp.inventorysorter.client.sort.Sorter;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

public final class SlotMover {

    private SlotMover() {}

    public static boolean canSort(LocalPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;
        return menu != null && menu.getCarried().isEmpty();
    }

    public static void execute(LocalPlayer player, List<Sorter.SourceTarget> moves) {
        if (moves.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null) return;

        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || !menu.getCarried().isEmpty()) return;

        int syncId = menu.containerId;

        for (List<Integer> chain : SlotPermutation.decompose(moves)) {
            for (int slot : chain) {
                gameMode.handleContainerInput(syncId, slot, 0, ContainerInput.PICKUP, player);
            }
        }
    }
}
