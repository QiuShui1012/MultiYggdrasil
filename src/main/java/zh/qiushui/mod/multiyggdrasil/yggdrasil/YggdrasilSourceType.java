package zh.qiushui.mod.multiyggdrasil.yggdrasil;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import zh.qiushui.mod.multiyggdrasil.auth.BetterYggdrasilServicesKeyInfo;
import zh.qiushui.mod.multiyggdrasil.util.OptionalUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static zh.qiushui.mod.multiyggdrasil.util.ParseUtil.getPublicKey;

public enum YggdrasilSourceType {
    OFFICIAL(
        simpleSerialize((source, sourceOb) -> {
            if (!Objects.equals(source.sessionRoot(), YggdrasilEnvironment.PROD.getEnvironment().sessionHost())) {
                sourceOb.addProperty("sessionHost", source.sessionRoot());
            }
        }),
        simpleDeserialize(raw -> Optional.ofNullable(raw.getAsJsonPrimitive("sessionHost"))
            .map(JsonPrimitive::getAsString)
            .orElse(YggdrasilEnvironment.PROD.getEnvironment().sessionHost()))
    ),
    BLESSING_SKIN(
        simpleSerialize((source, sourceOb) -> sourceOb.addProperty(
            "apiRoot", source.sessionRoot().replace("sessionserver/", ""))),
        simpleDeserialize(raw -> Optional.of(raw.getAsJsonPrimitive("apiRoot").getAsString())
            .map(v -> v.endsWith("/") ? v : v.concat("/"))
            .map(v -> v.endsWith("api/yggdrasil/") ? v : v.concat("api/yggdrasil/"))
            .map(v -> OptionalUtil.ifPresentDoAndReturn(
                v, v1 -> getPublicKey(v1).ifPresent(BetterYggdrasilServicesKeyInfo.PUBLIC_KEYS::add)))
            .map(v -> v.concat("sessionserver/")).orElseThrow())
    ),
    ;

    private final Function<IYggdrasilSource, JsonObject> serializer;
    private final BiFunction<YggdrasilSourceType, JsonObject, IYggdrasilSource> deserializer;

    YggdrasilSourceType(
        Function<IYggdrasilSource, JsonObject> serializer,
        BiFunction<YggdrasilSourceType, JsonObject, IYggdrasilSource> deserializer
    ) {
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    public JsonElement serialize(IYggdrasilSource src) {
        return this.serializer.apply(src);
    }

    public IYggdrasilSource deserialize(JsonElement json) throws JsonParseException {
        try {
            return this.deserializer.apply(this, json.getAsJsonObject());
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Cannot deserialize this source. Raw json: " + json + ".", e);
        }
    }

    private static Function<IYggdrasilSource, JsonObject> simpleSerialize(BiConsumer<IYggdrasilSource, JsonObject> sessionPart) {
        return source -> {
            JsonObject sourceOb = new JsonObject();
            sourceOb.addProperty("type", source.type().name());
            sourceOb.addProperty("name", source.name());
            sessionPart.accept(source, sourceOb);
            sourceOb.addProperty("ordinal", source.ordinal());
            return sourceOb;
        };
    }

    private static BiFunction<YggdrasilSourceType, JsonObject, IYggdrasilSource> simpleDeserialize(
        Function<JsonObject, String> sessionPart
    ) throws IllegalArgumentException {
        return (type, raw) -> {
            String name = raw.getAsJsonPrimitive("name").getAsString();
            String session = sessionPart.apply(raw);
            int ordinal = raw.getAsJsonPrimitive("ordinal").getAsInt();
            if (ordinal < 0) throw new IllegalArgumentException("Some source's ordinal is less than 0.");
            return IYggdrasilSource.of(type, name, session, ordinal);
        };
    }
}
