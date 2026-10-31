package dev.anvilcraft.rg.sd.tool;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResidentProfileSnapshotTest {
    private static final UUID ID = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    @Test
    void signedSkinAndIdentitySurviveTheSnapshotWithoutOtherProperties() {
        var original = new GameProfile(ID, "TestPlayer");
        original.getProperties().put("textures", new Property("textures", "skin-value", "skin-signature"));
        original.getProperties().put("other", new Property("other", "not-saved"));
        var json = ResidentProfileSnapshot.save(original);
        var restored = ResidentProfileSnapshot.restore("testplayer", json).orElseThrow();
        assertEquals(ID, restored.getId());
        var texture = restored.getProperties().get("textures").iterator().next();
        assertEquals("skin-value", texture.value());
        assertEquals("skin-signature", texture.signature());
        assertFalse(restored.getProperties().containsKey("other"));
        assertFalse(json.has("other"));
    }

    @Test
    void missingAndMalformedSnapshotsDoNotOverrideIdentity() {
        assertTrue(ResidentProfileSnapshot.restore("TestPlayer", null).isEmpty());
        var json = new JsonObject();
        json.addProperty("name", "DifferentName");
        json.addProperty("id", ID.toString());
        assertTrue(ResidentProfileSnapshot.restore("TestPlayer", json).isEmpty());
        json.addProperty("name", "TestPlayer");
        json.addProperty("id", "1-1-1-1-1");
        assertTrue(ResidentProfileSnapshot.restore("TestPlayer", json).isEmpty());
    }

    @Test
    void oversizedSkinOrInvalidSignatureIsNotRestored() {
        var json = new JsonObject();
        json.addProperty("name", "TestPlayer");
        json.addProperty("id", ID.toString());
        json.addProperty("texture", "x".repeat(32_769));
        assertFalse(ResidentProfileSnapshot.restore("TestPlayer", json).orElseThrow().getProperties().containsKey("textures"));
        json.addProperty("texture", "skin");
        json.addProperty("signature", "x".repeat(4_097));
        assertFalse(ResidentProfileSnapshot.restore("TestPlayer", json).orElseThrow().getProperties().containsKey("textures"));
    }

    @Test
    void unavailableOrMismatchedRefreshKeepsTheSavedSkin() {
        var saved = new GameProfile(ID, "TestPlayer");
        saved.getProperties().put("textures", new Property("textures", "old", "signed"));
        assertSame(saved, ResidentProfileSnapshot.preferAvailableSkin(saved, Optional.empty()));
        var empty = new GameProfile(ID, "TestPlayer");
        assertSame(saved, ResidentProfileSnapshot.preferAvailableSkin(saved, Optional.of(empty)));
        var other = new GameProfile(UUID.randomUUID(), "TestPlayer");
        other.getProperties().put("textures", new Property("textures", "other", "signed"));
        assertSame(saved, ResidentProfileSnapshot.preferAvailableSkin(saved, Optional.of(other)));
    }

    @Test
    void matchingFreshSkinReplacesTheSavedOne() {
        var saved = new GameProfile(ID, "TestPlayer");
        saved.getProperties().put("textures", new Property("textures", "old", "signed"));
        var fresh = new GameProfile(ID, "TestPlayer");
        fresh.getProperties().put("textures", new Property("textures", "fresh", "signed"));
        assertSame(fresh, ResidentProfileSnapshot.preferAvailableSkin(saved, Optional.of(fresh)));
    }
}
