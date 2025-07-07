package zh.qiushui.mod.multiyggdrasil.auth;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.logging.LogUtils;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.StringUtil;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BetterGameProfileCache extends GameProfileCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int GAMEPROFILES_MRU_LIMIT = 1000;
    private static final int GAMEPROFILES_EXPIRATION_MONTHS = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    @Setter
    private static boolean usesAuthentication;
    /**
     * A map between player usernames and info
     */
    private final Map<String, BetterGameProfileInfo> profilesByName = Maps.newConcurrentMap();
    /**
     * A map between uuid and info
     */
    private final Map<UUID, BetterGameProfileInfo> profilesByUUID = Maps.newConcurrentMap();
    private final Map<String, CompletableFuture<Optional<GameProfile>>> requests = Maps.newConcurrentMap();
    private final GameProfileRepository profileRepository;
    private final AtomicLong operationCount = new AtomicLong();
    @Nullable
    private Executor executor;

    public BetterGameProfileCache(GameProfileRepository profileRepository, File file) {
        super(profileRepository, file);
        this.profileRepository = profileRepository;
        Lists.reverse(this.loadBetter()).forEach(this::safeAdd);
    }

    private void safeAdd(BetterGameProfileInfo profile) {
        GameProfile gameprofile = profile.getProfile();
        profile.setLastAccess(this.getNextOperation());
        this.profilesByName.put(gameprofile.getName().toLowerCase(Locale.ROOT), profile);
        this.profilesByUUID.put(gameprofile.getId(), profile);
    }

    private static Optional<GameProfile> lookupGameProfile(GameProfileRepository profileRepo, String name) {
        if (!StringUtil.isValidPlayerName(name)) return createUnknownProfile(name);
        final AtomicReference<GameProfile> atomicreference = new AtomicReference<>();
        ProfileLookupCallback profilelookupcallback = new ProfileLookupCallback() {
            @Override
            public void onProfileLookupSucceeded(GameProfile profile) {
                atomicreference.set(profile);
            }

            @Override
            public void onProfileLookupFailed(String profileName, Exception exception) {
                atomicreference.set(null);
            }
        };
        profileRepo.findProfilesByNames(new String[] {name}, profilelookupcallback);
        GameProfile gameprofile = atomicreference.get();
        return gameprofile != null ? Optional.of(gameprofile) : createUnknownProfile(name);
    }

    private static Optional<GameProfile> createUnknownProfile(String profileName) {
        return usesAuthentication() ? Optional.empty() : Optional.of(UUIDUtil.createOfflineProfile(profileName));
    }

    private static boolean usesAuthentication() {
        return usesAuthentication;
    }

    /**
     * Add an entry to this cache
     */
    @Override
    public void add(GameProfile gameProfile) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, GAMEPROFILES_EXPIRATION_MONTHS);
        Date date = calendar.getTime();
        BetterGameProfileInfo info = new BetterGameProfileInfo(gameProfile, date);
        this.safeAdd(info);
        this.save();
    }

    private long getNextOperation() {
        return this.operationCount.incrementAndGet();
    }

    /**
     * Get a player's GameProfile given their username. Yggdrasil's source sources will be contacted if the entry is not cached locally.
     */
    @Override
    public Optional<GameProfile> get(String name) {
        String id = name.toLowerCase(Locale.ROOT);
        BetterGameProfileInfo info = this.profilesByName.get(id);
        boolean isModified = false;
        if (info != null && new Date().getTime() >= info.getExpirationDate().getTime()) {
            this.profilesByUUID.remove(info.getProfile().getId());
            this.profilesByName.remove(info.getProfile().getName().toLowerCase(Locale.ROOT));
            isModified = true;
            info = null;
        }

        Optional<GameProfile> optional;
        if (info != null) {
            info.setLastAccess(this.getNextOperation());
            optional = Optional.of(info.getProfile());
        } else {
            optional = lookupGameProfile(this.profileRepository, id);
            if (optional.isPresent()) {
                this.add(optional.get());
                isModified = false;
            }
        }

        if (isModified) {
            this.save();
        }

        return optional;
    }

    @Override
    public CompletableFuture<Optional<GameProfile>> getAsync(String name) {
        if (this.executor == null) throw new IllegalStateException("No executor");
        CompletableFuture<Optional<GameProfile>> future = this.requests.get(name);
        if (future != null) return future;
        future = CompletableFuture.supplyAsync(() -> this.get(name), Util.backgroundExecutor())
            .whenCompleteAsync((profileOp, ex) -> this.requests.remove(name), this.executor);
        this.requests.put(name, future);
        return future;
    }

    /**
     * @param uuid Get a player's {@link GameProfile} given their UUID
     */
    @Override
    public Optional<GameProfile> get(UUID uuid) {
        BetterGameProfileInfo info = this.profilesByUUID.get(uuid);
        if (info == null) return Optional.empty();
        info.setLastAccess(this.getNextOperation());
        return Optional.of(info.getProfile());
    }

    @Override
    public void setExecutor(@Nullable Executor executor) {
        this.executor = executor;
    }

    @Override
    public void clearExecutor() {
        this.executor = null;
    }

    private static DateFormat createDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
    }

    @Override
    public List<GameProfileInfo> load() {
        return Lists.transform(this.loadBetter(), o -> o);
    }

    public List<BetterGameProfileInfo> loadBetter() {
        List<BetterGameProfileInfo> cache = Lists.newArrayList();

        try {
            try (Reader reader = Files.newReader(this.file, StandardCharsets.UTF_8)) {
                JsonArray cacheJson = GSON.fromJson(reader, JsonArray.class);
                if (cacheJson != null) {
                    DateFormat format = createDateFormat();
                    cacheJson.forEach(p_143973_ -> readGameProfile(p_143973_, format).ifPresent(cache::add));
                    return cache;
                }
            } catch (FileNotFoundException ignored) {
            }

            return cache;
        } catch (JsonParseException | IOException ioexception) {
            LOGGER.warn("Failed to load profile cache {}", this.file, ioexception);
        }

        return cache;
    }

    @Override
    public void save() {
        JsonArray cache = new JsonArray();
        DateFormat dateformat = createDateFormat();
        this.getTopMRUProfiles(GAMEPROFILES_MRU_LIMIT).forEach(info -> cache.add(writeGameProfile(info, dateformat)));
        String s = GSON.toJson(cache);

        try (Writer writer = Files.newWriter(this.file, StandardCharsets.UTF_8)) {
            writer.write(s);
        } catch (IOException ignored) {
        }
    }

    @SuppressWarnings("SameParameterValue")
    private Stream<BetterGameProfileInfo> getTopMRUProfiles(int limit) {
        return ImmutableList.copyOf(this.profilesByUUID.values())
            .stream()
            .sorted(Comparator.comparing(BetterGameProfileInfo::getLastAccess).reversed())
            .limit(limit);
    }

    private static JsonElement writeGameProfile(BetterGameProfileInfo profileInfo, DateFormat dateFormat) {
        JsonObject cacheSingle = new JsonObject();
        cacheSingle.addProperty("name", profileInfo.getProfile().getName());
        cacheSingle.addProperty("uuid", profileInfo.getProfile().getId().toString());
        cacheSingle.addProperty("source", profileInfo.getSource());
        cacheSingle.addProperty("expiresOn", dateFormat.format(profileInfo.getExpirationDate()));
        return cacheSingle;
    }

    private static Optional<BetterGameProfileInfo> readGameProfile(JsonElement json, DateFormat dateFormat) {
        if (!json.isJsonObject()) return Optional.empty();
        JsonObject root = json.getAsJsonObject();
        JsonElement name = root.get("name");
        JsonElement uuidJson = root.get("uuid");
        if (name == null || uuidJson == null) return Optional.empty();
        String nameStr = name.getAsString();
        String uuidStr = uuidJson.getAsString();

        Date date = null;
        JsonElement expiresOn = root.get("expiresOn");
        if (expiresOn != null) {
            try {
                date = dateFormat.parse(expiresOn.getAsString());
            } catch (ParseException ignored) {
            }
        }

        if (nameStr == null || uuidStr == null || date == null) return Optional.empty();
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (Throwable throwable) {
            return Optional.empty();
        }

        return Optional.of(new BetterGameProfileInfo(new GameProfile(uuid, nameStr), date));
    }
}
