package dev.anvilcraft.rg.sd.combat.ysm;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import dev.anvilcraft.rg.sd.SiliconeDolls;
import dev.anvilcraft.rg.sd.command.PlayerCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class ModelCommand {
    public static CommandNode<CommandSourceStack> ysm = null;
    public static CommandNode<CommandSourceStack> model = null;
    public static CommandNode<CommandSourceStack> set = null;
    public static CommandNode<CommandSourceStack> targets = null;
    public static CommandNode<CommandSourceStack> modelId = null;
    public static CommandNode<CommandSourceStack> textureId = null;

    @SuppressWarnings("unused")
    public static @NotNull LiteralArgumentBuilder<CommandSourceStack> register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        ModelCommand.init(dispatcher);
        return Commands.literal("model")
            .then(
                Commands.literal("set")
                    .then(
                        Commands.argument("model_id", StringArgumentType.string())
                            .suggests(modelId::listSuggestions)
                            .then(
                                Commands.argument("texture_id", StringArgumentType.string())
                                    .suggests(textureId::listSuggestions)
                                    .executes(context -> set(context, dispatcher))
                            )
                    )
            );
    }

    public static boolean init(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        RootCommandNode<CommandSourceStack> root = dispatcher.getRoot();
        if (ModelCommand.textureId != null) return true;
        try {
            ModelCommand.ysm = root.getChild("ysm");
            ModelCommand.model = ModelCommand.ysm.getChild("model");
            ModelCommand.set = ModelCommand.model.getChild("set");
            ModelCommand.targets = ModelCommand.set.getChild("targets");
            ModelCommand.modelId = ModelCommand.targets.getChild("model_id");
            ModelCommand.textureId = ModelCommand.modelId.getChild("texture_id");
        } catch (Exception e) {
            SiliconeDolls.LOGGER.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    public static int set(@NotNull CommandContext<CommandSourceStack> context, @NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        if (ModelCommand.textureId == null && !ModelCommand.init(dispatcher)) return 0;
        ServerPlayer player = PlayerCommand.getPlayerByPermission(context);
        if (player == null) return 0;
        MinecraftServer server = player.getServer();
        if (server == null) return 0;
        String modelId = StringArgumentType.getString(context, "model_id");
        String textureId = StringArgumentType.getString(context, "texture_id");
        server.getCommands().performPrefixedCommand(
            context.getSource().withPermission(2),
            "ysm model set %s %s %s".formatted(player.getGameProfile().getName(), modelId, textureId)
        );
        return 1;
    }
}
