package dev.anvilcraft.rg.sd.combat.ysm;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;

/** Resolves the active dispatcher's schema; no nodes survive a server/world replacement. */
public record YsmCommandBinding<S>(CommandNode<S> model, CommandNode<S> texture) {
    public static <S> YsmCommandBinding<S> resolve(CommandDispatcher<S> dispatcher) {
        var node = dispatcher.getRoot().getChild("ysm");
        node = child(node, "model");
        node = child(node, "set");
        node = child(node, "targets");
        var model = child(node, "model_path");
        if (model == null) model = child(node, "model_id");
        var texture = child(model, "texture");
        if (texture == null) texture = child(model, "texture_id");
        return model == null || texture == null ? null : new YsmCommandBinding<>(model, texture);
    }

    private static <S> CommandNode<S> child(CommandNode<S> node, String name) {
        return node == null ? null : node.getChild(name);
    }

    public static String command(String player, String model, String texture) {
        return "ysm model set " + StringArgumentType.escapeIfRequired(player) + " "
            + StringArgumentType.escapeIfRequired(model) + " " + StringArgumentType.escapeIfRequired(texture);
    }
}
