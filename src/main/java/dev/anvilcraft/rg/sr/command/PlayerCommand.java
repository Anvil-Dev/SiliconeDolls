package dev.anvilcraft.rg.sr.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.anvilcraft.rg.sr.SiliconeDollsServerRules;
import dev.anvilcraft.rg.sr.entity.FakePlayer;
import dev.anvilcraft.rg.sr.init.ModCommands;
import dev.anvilcraft.rg.sr.util.CommandRuleValidator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlayerCommand {
    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("player")
                .requires(source -> CommandRuleValidator.hasPermission(() -> SiliconeDollsServerRules.commandPlayer, source))
                .then(
                    Commands.argument("name", StringArgumentType.word())
                        .suggests(PlayerCommand.suggestPlayer())
                        .then(
                            Commands.literal("spawn")
                                .executes(PlayerCommand::spawnPlayer)
                                .then(
                                    Commands.literal("at")
                                        .then(
                                            Commands.argument("pos", Vec3Argument.vec3())
                                                .executes(PlayerCommand::spawnPlayer)
                                                .then(
                                                    Commands.literal("facing")
                                                        .then(
                                                            Commands.argument("facing", Vec2Argument.vec2())
                                                                .executes(PlayerCommand::spawnPlayer)
                                                                .then(
                                                                    Commands.literal("in")
                                                                        .then(
                                                                            Commands.argument("dimension", DimensionArgument.dimension())
                                                                                .executes(PlayerCommand::spawnPlayer)
                                                                                .then(
                                                                                    Commands.literal("in")
                                                                                        .then(
                                                                                            Commands.argument("gamemode", GameModeArgument.gameMode())
                                                                                                .executes(PlayerCommand::spawnPlayer)
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("kill")
                                .executes(PlayerCommand::kill)
                        )
                        .then(
                            Commands.literal("jump")
                        )
                        .then(
                            Commands.literal("use")
                        )
                        .then(
                            Commands.literal("attack")
                        )
                        .then(
                            Commands.literal("sneak")
                        )
                        .then(
                            Commands.literal("unsneak")
                        )
                        .then(
                            Commands.literal("sprint")
                        )
                        .then(
                            Commands.literal("unsprint")
                        )
                        .then(
                            Commands.literal("swapHands")
                        )
                        .then(
                            Commands.literal("look")
                        )
                        .then(
                            Commands.literal("turn")
                        )
                        .then(
                            Commands.literal("move")
                        )
                        .then(
                            Commands.literal("hotbar")
                        )
                        .then(
                            Commands.literal("drop")
                        )
                        .then(
                            Commands.literal("dropStack")
                        )
                        .then(
                            Commands.literal("shadow")
                                .executes(PlayerCommand::shadowPlayer)
                        )
                        .then(
                            Commands.literal("mount")
                        )
                        .then(
                            Commands.literal("dismount")
                        )
                        .then(
                            Commands.literal("stop")
                        )
                )
        );
    }

    public static @NotNull SuggestionProvider<CommandSourceStack> suggestPlayer() {
        return (context, builder) -> {
            List<String> players = new ArrayList<>(context.getSource().getOnlinePlayerNames());
            players.add("Alex");
            players.add("Steve");
            return ModCommands.suggest(players).getSuggestions(context, builder);
        };
    }

    public static int spawnPlayer(@NotNull CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        String name = ModCommands.getArg(context, "name", StringArgumentType::getString);
        if (name == null) {
            context.getSource().sendFailure(Component.literal("Invalid player name").withStyle(ChatFormatting.RED));
            return 0;
        }
        PlayerList playerList = context.getSource().getServer().getPlayerList();
        ServerPlayer playerByName = playerList.getPlayerByName(name);
        if (playerByName != null) {
            context.getSource().sendFailure(Component.literal("Player " + name + " is already online").withStyle(ChatFormatting.RED));
            return 0;
        }
        Vec3 pos = ModCommands.getArg(context, "pos", Vec3Argument::getVec3);
        if (pos == null) {
            pos = context.getSource().getPosition();
        }
        Vec2 facing = ModCommands.getArg(context, "facing", Vec2Argument::getVec2);
        if (facing == null) {
            facing = context.getSource().getRotation();
        }
        ServerLevel level = ModCommands.getArg(context, "dimension", (context1, name1) -> {
            try {
                return DimensionArgument.getDimension(context1, name1);
            } catch (CommandSyntaxException e) {
                return null;
            }
        });
        if (level == null) {
            level = context.getSource().getLevel();
        }
        GameType gameMode = ModCommands.getArg(context, "gamemode", (context1, name1) -> {
            try {
                return GameModeArgument.getGameMode(context1, name1);
            } catch (CommandSyntaxException e) {
                return null;
            }
        });
        if (gameMode == null) {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                gameMode = GameType.CREATIVE;
            } else {
                gameMode = player.gameMode.getGameModeForPlayer();
            }
        }
        return FakePlayer.createFake(server, name, pos, facing, level, gameMode) ? 1 : 0;
    }

    public static int shadowPlayer(@NotNull CommandContext<CommandSourceStack> context) {
        ServerPlayer player = isFakePlayerValid(context);
        if (player == null) return 0;
        FakePlayer.createShadow(player);
        return 1;
    }

    public static int kill(@NotNull CommandContext<CommandSourceStack> context) {
        ServerPlayer player = isFakePlayerValid(context);
        if (player == null) return 0;
        if (player instanceof FakePlayer fakePlayer) {
            fakePlayer.kill();
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Player " + player.getName().getString() + " is not a SiliconeRubber").withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static ServerPlayer isFakePlayerValid(@NotNull CommandContext<CommandSourceStack> context) {
        String name = ModCommands.getArg(context, "name", StringArgumentType::getString);
        if (name == null) {
            context.getSource().sendFailure(Component.literal("Invalid player name").withStyle(ChatFormatting.RED));
            return null;
        }
        PlayerList playerList = context.getSource().getServer().getPlayerList();
        ServerPlayer playerByName = playerList.getPlayerByName(name);
        if (playerByName == null) {
            context.getSource().sendFailure(Component.literal("Player not found").withStyle(ChatFormatting.RED));
            return null;
        }
        return playerByName;
    }
}
