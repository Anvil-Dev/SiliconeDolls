package dev.anvilcraft.rg.sd.tool;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.Optional;
import java.util.UUID;

/** Persists only the public profile identity and an already obtained skin property. */
final class ResidentProfileSnapshot {
    private static final int MAX_TEXTURE_LENGTH = 32_768;
    private static final int MAX_SIGNATURE_LENGTH = 4_096;

    private ResidentProfileSnapshot() {}

    static JsonObject save(GameProfile profile) {
        var result = new JsonObject();
        if (profile == null || profile.getId() == null || profile.getName() == null) return result;
        result.addProperty("id", profile.getId().toString());
        result.addProperty("name", profile.getName());
        for (Property property : profile.getProperties().get("textures")) {
            if (property.value() == null || property.value().isEmpty() || property.value().length() > MAX_TEXTURE_LENGTH
                    || (property.signature() != null && property.signature().length() > MAX_SIGNATURE_LENGTH)) continue;
            result.addProperty("texture", property.value());
            if (property.signature() != null) result.addProperty("signature", property.signature());
            break;
        }
        return result;
    }

    static Optional<GameProfile> restore(String username, JsonElement element) {
        if (username == null || element == null || !element.isJsonObject()) return Optional.empty();
        var object = element.getAsJsonObject();
        var name = string(object, "name", 16);
        var id = string(object, "id", 36);
        if (name == null || id == null || id.length() != 36 || !name.equalsIgnoreCase(username)
                || !name.matches("[A-Za-z0-9_]{1,16}")) return Optional.empty();
        try {
            var profile = new GameProfile(UUID.fromString(id), name);
            var texture = string(object, "texture", MAX_TEXTURE_LENGTH);
            var signature = string(object, "signature", MAX_SIGNATURE_LENGTH);
            if (texture != null && !texture.isEmpty() && (!object.has("signature") || signature != null)) {
                profile.getProperties().put("textures", new Property("textures", texture, signature));
            }
            return Optional.of(profile);
        } catch (IllegalArgumentException invalidId) {
            return Optional.empty();
        }
    }

    static GameProfile preferAvailableSkin(GameProfile fallback, Optional<GameProfile> refreshed) {
        if (refreshed.isEmpty() || !fallback.getId().equals(refreshed.get().getId())) return fallback;
        var fresh = refreshed.get();
        return fresh.getProperties().containsKey("textures") || !fallback.getProperties().containsKey("textures")
                ? fresh : fallback;
    }

    private static String string(JsonObject object, String name, int maximum) {
        JsonElement element = object.get(name);
        if (!(element instanceof JsonPrimitive value) || !value.isString()) return null;
        var text = value.getAsString();
        return text.length() <= maximum ? text : null;
    }
}
