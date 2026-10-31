package dev.anvilcraft.rg.sd.combat.ysm;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.anvilcraft.rg.sd.command.PlayerCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class ModelCommand {
    private ModelCommand() {}

    public static @Nullable LiteralArgumentBuilder<CommandSourceStack> register(
        CommandDispatcher<CommandSourceStack> dispatcher) {
        var binding = YsmCommandBinding.resolve(dispatcher);
        if (binding == null) return null;
        return Commands.literal("model").then(Commands.literal("set")
            .then(Commands.argument(binding.model().getName(), StringArgumentType.string())
                .suggests(binding.model()::listSuggestions)
                .then(Commands.argument(binding.texture().getName(), StringArgumentType.string())
                    .suggests(binding.texture()::listSuggestions)
                    .executes(context -> set(context, dispatcher)))));
    }

    public static int set(CommandContext<CommandSourceStack> context,
        CommandDispatcher<CommandSourceStack> dispatcher) {
        var binding = YsmCommandBinding.resolve(dispatcher);
        if (binding == null) return 0;
        ServerPlayer player = PlayerCommand.getPlayerByPermission(context);
        if (player == null) return 0;
        MinecraftServer server = player.getServer();
        if (server == null) return 0;
        String model = StringArgumentType.getString(context, binding.model().getName());
        String texture = StringArgumentType.getString(context, binding.texture().getName());
        server.getCommands().performPrefixedCommand(context.getSource().withPermission(2),
            YsmCommandBinding.command(player.getGameProfile().getName(), model, texture));
        return 1;
    }
}
