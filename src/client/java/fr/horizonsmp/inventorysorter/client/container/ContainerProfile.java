package fr.horizonsmp.inventorysorter.client.container;

import fr.horizonsmp.inventorysorter.client.sort.Sorter.Region;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;

public record ContainerProfile(
    String screenId,
    Region containerRegion,
    Region playerMainRegion,
    Region hotbarRegion
) {

    public static final String SCREEN_PLAYER = "player";
    public static final String SCREEN_CHEST = "chest";
    public static final String SCREEN_SHULKER = "shulker";
    public static final String SCREEN_HOPPER = "hopper";
    public static final String SCREEN_DISPENSER = "dispenser";
    public static final String SCREEN_UNKNOWN = "unknown";

    public boolean hasContainer() {
        return containerRegion != null;
    }

    public static ContainerProfile detect(AbstractContainerMenu menu) {
        if (menu instanceof InventoryMenu) {
            return new ContainerProfile(
                SCREEN_PLAYER,
                null,
                new Region(InventoryMenu.INV_SLOT_START, InventoryMenu.INV_SLOT_END, 9, 3),
                new Region(InventoryMenu.USE_ROW_SLOT_START, InventoryMenu.USE_ROW_SLOT_END, 9, 1)
            );
        }
        if (menu instanceof ChestMenu chest) {
            int rows = chest.getRowCount();
            int containerSize = rows * 9;
            return new ContainerProfile(
                SCREEN_CHEST,
                new Region(0, containerSize - 1, 9, rows),
                new Region(containerSize, containerSize + 26, 9, 3),
                new Region(containerSize + 27, containerSize + 35, 9, 1)
            );
        }
        if (menu instanceof ShulkerBoxMenu) {
            return new ContainerProfile(
                SCREEN_SHULKER,
                new Region(0, 26, 9, 3),
                new Region(27, 53, 9, 3),
                new Region(54, 62, 9, 1)
            );
        }
        if (menu instanceof HopperMenu) {
            return new ContainerProfile(
                SCREEN_HOPPER,
                new Region(0, 4, 5, 1),
                new Region(5, 31, 9, 3),
                new Region(32, 40, 9, 1)
            );
        }
        if (menu instanceof DispenserMenu) {
            return new ContainerProfile(
                SCREEN_DISPENSER,
                new Region(0, 8, 3, 3),
                new Region(9, 35, 9, 3),
                new Region(36, 44, 9, 1)
            );
        }
        return new ContainerProfile(SCREEN_UNKNOWN, null, null, null);
    }
}
