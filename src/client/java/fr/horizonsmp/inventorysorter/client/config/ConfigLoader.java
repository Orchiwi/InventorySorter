package fr.horizonsmp.inventorysorter.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.horizonsmp.inventorysorter.client.sort.SortCriterion;
import fr.horizonsmp.inventorysorter.client.sort.SortMethod;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger("inventorysorter/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path configFile;

    public ConfigLoader(Path configFile) {
        this.configFile = configFile;
    }

    public ModConfig loadOrDefault() {
        if (!Files.exists(configFile)) {
            ModConfig defaults = ModConfig.defaults();
            try {
                save(defaults);
            } catch (IOException ex) {
                LOGGER.warn("Failed to write default config to {}", configFile, ex);
            }
            return defaults;
        }
        try {
            String content = Files.readString(configFile);
            return parse(content);
        } catch (IOException ex) {
            LOGGER.warn("Failed to read config from {}, falling back to defaults", configFile, ex);
            return ModConfig.defaults();
        } catch (RuntimeException ex) {
            LOGGER.warn("Config at {} is malformed, falling back to defaults", configFile, ex);
            return ModConfig.defaults();
        }
    }

    public void save(ModConfig config) throws IOException {
        Files.createDirectories(configFile.getParent());
        Path tmp = configFile.resolveSibling(configFile.getFileName() + ".tmp");
        Files.writeString(tmp, render(config));
        try {
            Files.move(tmp, configFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, configFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private ModConfig parse(String content) {
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();
        ModConfig defaults = ModConfig.defaults();

        SortCriterion criterion = readEnum(root, "current_criterion", SortCriterion.class, defaults.currentCriterion());
        SortMethod method = readEnum(root, "current_method", SortMethod.class, defaults.currentMethod());
        boolean includeHotbar = readBoolean(root, "include_hotbar", defaults.includeHotbar());
        boolean mergePartial = readBoolean(root, "merge_partial_stacks", defaults.mergePartialStacks());

        Map<String, Boolean> screens = new LinkedHashMap<>(defaults.enabledScreens());
        if (root.has("enabled_screens") && root.get("enabled_screens").isJsonObject()) {
            JsonObject obj = root.getAsJsonObject("enabled_screens");
            for (String key : screens.keySet()) {
                if (obj.has(key)) {
                    screens.put(key, obj.get(key).getAsBoolean());
                }
            }
        }
        return new ModConfig(criterion, method, includeHotbar, mergePartial, screens);
    }

    private String render(ModConfig config) {
        JsonObject root = new JsonObject();
        root.addProperty("current_criterion", config.currentCriterion().name());
        root.addProperty("current_method", config.currentMethod().name());
        root.addProperty("include_hotbar", config.includeHotbar());
        root.addProperty("merge_partial_stacks", config.mergePartialStacks());
        JsonObject screens = new JsonObject();
        config.enabledScreens().forEach(screens::addProperty);
        root.add("enabled_screens", screens);
        return GSON.toJson(root);
    }

    private static <E extends Enum<E>> E readEnum(JsonObject root, String key, Class<E> type, E fallback) {
        if (!root.has(key)) return fallback;
        try {
            return Enum.valueOf(type, root.get(key).getAsString());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static boolean readBoolean(JsonObject root, String key, boolean fallback) {
        if (!root.has(key)) return fallback;
        try {
            return root.get(key).getAsBoolean();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }
}
