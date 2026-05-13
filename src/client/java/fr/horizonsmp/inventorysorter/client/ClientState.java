package fr.horizonsmp.inventorysorter.client;

import fr.horizonsmp.inventorysorter.client.config.ConfigLoader;
import fr.horizonsmp.inventorysorter.client.config.ModConfig;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientState {

    private static final Logger LOGGER = LoggerFactory.getLogger("inventorysorter/state");

    private static volatile ClientState instance;

    private final ConfigLoader loader;
    private final AtomicReference<ModConfig> config;

    private ClientState(ConfigLoader loader, ModConfig initial) {
        this.loader = loader;
        this.config = new AtomicReference<>(initial);
    }

    public static ClientState init(ConfigLoader loader) {
        ModConfig initial = loader.loadOrDefault();
        ClientState state = new ClientState(loader, initial);
        instance = state;
        return state;
    }

    public static ClientState get() {
        ClientState s = instance;
        if (s == null) {
            throw new IllegalStateException("ClientState not initialized");
        }
        return s;
    }

    public ModConfig config() {
        return config.get();
    }

    public void update(UnaryOperator<ModConfig> mutator) {
        ModConfig next = mutator.apply(config.get());
        config.set(next);
        try {
            loader.save(next);
        } catch (IOException ex) {
            LOGGER.warn("Failed to save inventorysorter config", ex);
        }
    }
}
