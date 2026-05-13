package fr.horizonsmp.inventorysorter.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int inventorysorter$getLeftPos();

    @Accessor("topPos")
    int inventorysorter$getTopPos();

    @Accessor("imageWidth")
    int inventorysorter$getImageWidth();

    @Accessor("imageHeight")
    int inventorysorter$getImageHeight();
}
