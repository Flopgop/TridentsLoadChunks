package net.flamgop.tlc;

import com.google.gson.FormattingStyle;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.JsonOps;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;

public class TridentsLoadChunks implements ModInitializer {

    private static TridentsLoadChunks INSTANCE;
    public static TridentsLoadChunks instance() {
        return INSTANCE;
    }

    private static final Path CONFIG_FILE_PATH = Path.of("tridents_load_chunks.json");
    public static final Logger LOGGER = LoggerFactory.getLogger("TridentsLoadChunks");

    private TLCConfig config;

    @Override
    public void onInitialize() {
        INSTANCE = this;
        TicketTypes.initialize();
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resourceManager) -> {
            try {
                config = this.loadConfig();
            } catch (IOException | IllegalStateException e) {
                LOGGER.error("Failed to load TridentsLoadChunks config: ", e);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
            try {
                this.saveConfig(config == null ? new TLCConfig() : config);
            } catch (IOException | IllegalStateException e) {
                LOGGER.error("Failed to save TridentsLoadChunks config: ", e);
            }
        });
    }

    public @NotNull TLCConfig config() {
        if (config == null) {
            try {
                config = loadConfig();
            } catch (IOException | IllegalStateException e) {
                LOGGER.error("Failed to load TridentsLoadChunks config: ", e);
                config = new TLCConfig();
            }
        }
        return config;
    }

    private TLCConfig loadConfig() throws IOException, IllegalStateException {
        File configFile = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_PATH).toFile();
        if (!configFile.exists()) {
            return new TLCConfig();
        }

        try (FileReader reader = new FileReader(configFile)) {
            return TLCConfig.CODEC.parse(JsonOps.INSTANCE, GsonHelper.parse(reader)).getOrThrow();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void saveConfig(TLCConfig config) throws IOException, IllegalStateException {
        JsonElement elem = TLCConfig.CODEC.encode(config, JsonOps.INSTANCE, JsonOps.INSTANCE.empty()).getOrThrow();

        File configFile = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_PATH).toFile();
        if (!configFile.exists()) {
            if (!configFile.getParentFile().exists()) configFile.getParentFile().mkdirs();
            configFile.createNewFile();
        }

        try (JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(configFile)))) {
            writer.setFormattingStyle(FormattingStyle.PRETTY);
            GsonHelper.writeValue(writer, elem, String::compareToIgnoreCase);
        }
    }
}
