package zh.qiushui.mod.multiyggdrasil.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;
import zh.qiushui.mod.multiyggdrasil.MultiYggdrasil;
import zh.qiushui.mod.multiyggdrasil.yggdrasil.IYggdrasilSource;
import zh.qiushui.mod.multiyggdrasil.yggdrasil.YggdrasilSourceType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record YggdrasilServersConfig(List<IYggdrasilSource> sources) implements Serializable {
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("multi-yggdrasil.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void save(YggdrasilServersConfig config) {
        tryMkConfigDirs();
        JsonObject root = new JsonObject();
        JsonArray sources = new JsonArray();
        for (IYggdrasilSource source : config.sources) {
            sources.add(s(source));
        }
        root.add("sources", sources);

        try (BufferedWriter writer = Files.newBufferedWriter(PATH)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            MultiYggdrasil.LOGGER.warn("Cannot save config.", e);
        }
    }

    private static void tryMkConfigDirs() {
        if (!FMLPaths.CONFIGDIR.get().toFile().exists() && !FMLPaths.CONFIGDIR.get().toFile().mkdirs()) {
            MultiYggdrasil.LOGGER.warn("Cannot mk config dirs. Path: {}", FMLPaths.CONFIGDIR.get());
        }
    }

    private static @NotNull JsonElement s(IYggdrasilSource source) {
        return source.type().serialize(source);
    }

    public static YggdrasilServersConfig load() {
        JsonObject root;

        try (BufferedReader reader = Files.newBufferedReader(PATH)) {
            root = GSON.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            MultiYggdrasil.LOGGER.warn("Cannot load config. Use default (only mojang.)", e);
            return new YggdrasilServersConfig(List.of());
        }

        JsonElement sourcesRaw = root.get("sources");
        if (sourcesRaw instanceof JsonObject source) {
            return new YggdrasilServersConfig(List.of(des(source)));
        } else if (sourcesRaw instanceof JsonArray sources) {
            List<IYggdrasilSource> sourceList = new ArrayList<>();
            for (JsonElement source : sources) {
                JsonObject parsing = new JsonObject();
                if (source instanceof JsonPrimitive primitive) {
                    parsing.add("type", primitive);
                } else if (source instanceof JsonObject object) {
                    parsing = object;
                }
                sourceList.add(des(parsing));
            }
            return new YggdrasilServersConfig(List.copyOf(sourceList));
        } else {
            MultiYggdrasil.LOGGER.warn("Cannot load config, because the format is wrong. Use default (only mojang.)");
            return new YggdrasilServersConfig(List.of());
        }
    }

    private static IYggdrasilSource des(JsonObject raw) throws IllegalArgumentException, ClassCastException {
        return YggdrasilSourceType.valueOf(raw.get("type").getAsString().toUpperCase(Locale.ROOT)).deserialize(raw);
    }

}
