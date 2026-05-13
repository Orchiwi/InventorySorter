package fr.horizonsmp.inventorysorter.client;

import fr.horizonsmp.inventorysorter.client.config.ConfigLoader;
import fr.horizonsmp.inventorysorter.client.keybinding.SortKeybinding;
import fr.horizonsmp.inventorysorter.client.screen.ScreenAttacher;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class InventorysorterClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve("inventorysorter.json");
        ConfigLoader loader = new ConfigLoader(configFile);
        ClientState.init(loader);
        SortKeybinding.register();
        ScreenAttacher.register();
    }
}
