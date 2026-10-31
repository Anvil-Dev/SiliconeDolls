package dev.anvilcraft.rg.sd.combat.ysm;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YsmCommandBindingTest {
    @Test
    void resolvesBothPublishedCommandSchemas() {
        var modern = YsmCommandBinding.resolve(dispatcher("model_path", "texture"));
        assertEquals("model_path", modern.model().getName());
        assertEquals("texture", modern.texture().getName());
        var legacy = YsmCommandBinding.resolve(dispatcher("model_id", "texture_id"));
        assertEquals("model_id", legacy.model().getName());
        assertEquals("texture_id", legacy.texture().getName());
    }

    @Test
    void unavailableOrReplacedDispatcherDoesNotReuseOldNodes() {
        assertNotNull(YsmCommandBinding.resolve(dispatcher("model_path", "texture")));
        assertNull(YsmCommandBinding.resolve(new CommandDispatcher<Object>()));
        var second = dispatcher("model_path", "texture");
        assertNotSame(YsmCommandBinding.resolve(dispatcher("model_path", "texture")).model(),
            YsmCommandBinding.resolve(second).model());
    }

    @Test
    void forwardingEscapesPathsAndTextureArguments() throws Exception {
        var reader = new StringReader(YsmCommandBinding.command("TestPlayer", "global:custom/a \"quoted\".ysm", "a\\b c"));
        assertEquals("ysm", reader.readUnquotedString()); reader.skipWhitespace();
        assertEquals("model", reader.readUnquotedString()); reader.skipWhitespace();
        assertEquals("set", reader.readUnquotedString()); reader.skipWhitespace();
        assertEquals("TestPlayer", reader.readString()); reader.skipWhitespace();
        assertEquals("global:custom/a \"quoted\".ysm", reader.readString()); reader.skipWhitespace();
        assertEquals("a\\b c", reader.readString());
        assertFalse(reader.canRead());
    }

    private static CommandDispatcher<Object> dispatcher(String model, String texture) {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("ysm")
            .then(LiteralArgumentBuilder.<Object>literal("model")
                .then(LiteralArgumentBuilder.<Object>literal("set")
                    .then(RequiredArgumentBuilder.<Object, String>argument("targets", StringArgumentType.word())
                        .then(RequiredArgumentBuilder.<Object, String>argument(model, StringArgumentType.string())
                            .then(RequiredArgumentBuilder.<Object, String>argument(texture, StringArgumentType.string())))))));
        return dispatcher;
    }
}
