package fr.horizonsmp.inventorysorter.client;

import fr.horizonsmp.inventorysorter.client.container.ContainerProfile;
import fr.horizonsmp.inventorysorter.client.container.SlotMover;
import fr.horizonsmp.inventorysorter.client.sort.Sorter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class SortAction {

    private SortAction() {}

    public static boolean runOnContainer(ContainerProfile profile) {
        if (!profile.hasContainer()) return false;
        return run(profile.containerRegion());
    }

    public static boolean runOnPlayerMain(ContainerProfile profile, boolean includeHotbar) {
        Sorter.Region playerMain = profile.playerMainRegion();
        if (playerMain == null) return false;
        if (!includeHotbar || profile.hotbarRegion() == null) {
            return run(playerMain);
        }
        Sorter.Region hotbar = profile.hotbarRegion();
        Sorter.Region combined = new Sorter.Region(
            playerMain.slotStart(),
            hotbar.slotEndInclusive(),
            playerMain.cols(),
            playerMain.rows() + hotbar.rows()
        );
        return run(combined);
    }

    private static boolean run(Sorter.Region region) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || !SlotMover.canSort(player)) return false;

        List<ItemStack> current = readStacks(menu, region);
        ModConfigSnapshot snapshot = ModConfigSnapshot.current();
        List<Sorter.SourceTarget> moves = Sorter.plan(region, current, snapshot.criterion(), snapshot.method());
        SlotMover.execute(player, moves);
        return !moves.isEmpty();
    }

    private static List<ItemStack> readStacks(AbstractContainerMenu menu, Sorter.Region region) {
        List<ItemStack> result = new ArrayList<>(region.size());
        for (int i = region.slotStart(); i <= region.slotEndInclusive(); i++) {
            result.add(menu.slots.get(i).getItem());
        }
        return result;
    }

    private record ModConfigSnapshot(
        fr.horizonsmp.inventorysorter.client.sort.SortCriterion criterion,
        fr.horizonsmp.inventorysorter.client.sort.SortMethod method
    ) {
        static ModConfigSnapshot current() {
            var cfg = ClientState.get().config();
            return new ModConfigSnapshot(cfg.currentCriterion(), cfg.currentMethod());
        }
    }
}
