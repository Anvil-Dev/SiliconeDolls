package dev.anvilcraft.rg.sd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.anvilcraft.rg.api.RGValidator;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.anvilcraft.rg.api.server.TranslationUtil;
import dev.anvilcraft.rg.sd.SiliconeDollsServerRules;
import dev.anvilcraft.rg.sd.entity.FakePlayer;
import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import dev.anvilcraft.rg.sd.init.ModCommands;
import dev.anvilcraft.rg.sd.util.ServerPlayerInjector;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlayerCommand {
    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("player")
                .requires(source -> RGValidator.CommandRuleValidator.hasPermission(() -> SiliconeDollsServerRules.commandPlayer, source))
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
                                                            Commands.argument("facing", RotationArgument.rotation())
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
                            //use, jump, attack
                            Commands.argument("action", StringArgumentType.word())
                                .suggests(PlayerCommand::suggestAction)
                                .executes(ctx -> actions(ctx, "once"))
                                .then(Commands.literal("once").executes(ctx -> actions(ctx, "once")))
                                .then(Commands.literal("continue").executes(ctx -> actions(ctx, "continues")))
                                .then(
                                    Commands.literal("interval")
                                        .then(
                                            Commands.argument("time", IntegerArgumentType.integer(1))
                                                .executes(ctx -> actions(ctx, "interval"))
                                        )
                                )
                        )
                        .then(
                            Commands.literal("sneak")
                                .executes(ctx -> sneak(ctx, true))
                        )
                        .then(
                            Commands.literal("unsneak")
                                .executes(ctx -> sneak(ctx, false))
                        )
                        .then(
                            Commands.literal("sprint")
                                .executes(ctx -> sprint(ctx, true))
                        )
                        .then(
                            Commands.literal("unsprint")
                                .executes(ctx -> sprint(ctx, false))
                        )
                        .then(
                            Commands.literal("mount")
                                .executes(ctx -> mount(ctx, true))
                        )
                        .then(
                            Commands.literal("dismount")
                                .executes(ctx -> mount(ctx, false))
                        ).then(
                            Commands.literal("look")
                                .then(
                                    Commands.literal("at")
                                        .then(
                                            Commands.argument("pos", Vec3Argument.vec3())
                                                .executes(PlayerCommand::lookAt)
                                        )
                                )
                                .then(
                                    Commands.argument("direction", StringArgumentType.word())
                                        .suggests(PlayerCommand::suggestDirection)
                                        .executes(PlayerCommand::lookDirection)
                                )
                        ).then(
                            Commands.literal("turn")
                                .then(
                                    Commands.argument("x", FloatArgumentType.floatArg())
                                        .then(
                                            Commands.argument("y", FloatArgumentType.floatArg())
                                                .executes(PlayerCommand::turn)
                                        )
                                )
                                .then(
                                    Commands.argument("rotation", StringArgumentType.word())
                                        .suggests(PlayerCommand::suggestRotation)
                                        .executes(PlayerCommand::turnRotation)
                                )
                        ).then(
                            Commands.literal("move")
                        )
                        .then(
                            Commands.literal("swapHands")
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
                            Commands.literal("stop")
                                .executes(PlayerCommand::stopActions)
                        )
                )
        );
    }

    private static @NotNull SuggestionProvider<CommandSourceStack> suggestPlayer() {
        return (context, builder) -> {
            List<String> players = new ArrayList<>(context.getSource().getOnlinePlayerNames());
            players.add("Alex");
            players.add("Steve");
            return ModCommands.suggest(players).getSuggestions(context, builder);
        };
    }

    private static @NotNull CompletableFuture<Suggestions> suggestAction(final CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(new String[]{"jump", "use", "attack"}, builder);
    }

    private static @NotNull CompletableFuture<Suggestions> suggestDirection(final CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(new String[]{"west", "east", "north", "south", "up", "down"}, builder);
    }

    private static @NotNull CompletableFuture<Suggestions> suggestRotation(final CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(new String[]{"left", "forward", "right", "back"}, builder);
    }

    public static int spawnPlayer(@NotNull CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        String name = ModCommands.getArg(context, "name", StringArgumentType::getString);
        if (name == null) {
            source.sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.invalid_name").withStyle(ChatFormatting.RED));
            return 0;
        }
        PlayerList playerList = source.getServer().getPlayerList();
        ServerPlayer playerByName = playerList.getPlayerByName(name);
        if (playerByName != null) {
            source.sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.logged", name).withStyle(ChatFormatting.RED));
            return 0;
        }
        Vec3 pos = ModCommands.getArg(context, "pos", Vec3Argument::getVec3);
        if (pos == null) {
            pos = source.getPosition();
        }
        Coordinates coordinates = ModCommands.getArg(context, "facing", RotationArgument::getRotation);
        Vec2 facing;
        if (coordinates == null) {
            facing = source.getRotation();
        } else {
            facing = coordinates.getRotation(source);
        }
        ServerLevel level = ModCommands.getArg(context, "dimension", (context1, name1) -> {
            try {
                return DimensionArgument.getDimension(context1, name1);
            } catch (CommandSyntaxException e) {
                return null;
            }
        });
        if (level == null) {
            level = source.getLevel();
        }
        GameType gameMode = ModCommands.getArg(context, "gamemode", (context1, name1) -> {
            try {
                return GameModeArgument.getGameMode(context1, name1);
            } catch (CommandSyntaxException e) {
                return null;
            }
        });
        if (gameMode == null) {
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                gameMode = GameType.CREATIVE;
            } else {
                gameMode = player.gameMode.getGameModeForPlayer();
            }
        }
        return FakePlayer.createFake(server, name, pos, facing, level, gameMode) ? 1 : 0;
    }

    public static int shadowPlayer(@NotNull CommandContext<CommandSourceStack> context) {
        ServerPlayer player = isThereFakePlayer(context);
        if (player == null) return 0;
        FakePlayer.createShadow(player);
        return 1;
    }

    public static int kill(@NotNull CommandContext<CommandSourceStack> context) {
        FakePlayer player = isFakePlayerValid(context);
        if (player == null) return 0;
        player.kill();
        return 1;
    }

    public static int actions(@NotNull CommandContext<CommandSourceStack> context, String interval) {
        String action = ModCommands.getArg(context, "action", StringArgumentType::getString);
        if (action == null) {
            return 0;
        }
        if (action.equals("jump") || action.equals("use") || action.equals("attack")) {
            FakePlayer player = isFakePlayerValid(context);
            if (player == null) return 0;
            PlayerActionPack actionPack = ((ServerPlayerInjector) player).getActionPack();
            if (interval.equals("interval")) {
                int time;
                try {
                    time = IntegerArgumentType.getInteger(context, "time");
                } catch (IllegalArgumentException ignored) {
                    context.getSource().sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.invalid_interval").withStyle(ChatFormatting.RED));
                    return 0;
                }
                actionPack.start(getAction(action), PlayerActionPack.Action.interval(time));
            } else if (interval.equals("continues")) {
                actionPack.start(getAction(action), PlayerActionPack.Action.continuous());
            } else {
                actionPack.start(getAction(action), PlayerActionPack.Action.once());
            }
            return 1;
        } else {
            context.getSource().sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.invalid_command").withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    public static int sneak(@NotNull CommandContext<CommandSourceStack> context, boolean doSneak) {
        FakePlayer player = isFakePlayerValid(context);
        if (player == null) return 0;
        PlayerActionPack actionPack = ((ServerPlayerInjector) player).getActionPack();
        actionPack.setSneaking(doSneak);
        return 1;
    }

    public static int sprint(@NotNull CommandContext<CommandSourceStack> context, boolean doSprint) {
        FakePlayer player = isFakePlayerValid(context);
        if (player == null) return 0;
        PlayerActionPack actionPack = ((ServerPlayerInjector) player).getActionPack();
        actionPack.setSprinting(doSprint);
        return 1;
    }

    public static int mount(@NotNull CommandContext<CommandSourceStack> context, boolean doMount) {
        FakePlayer player = isFakePlayerValid(context);
        if (player == null) return 0;
        PlayerActionPack actionPack = ((ServerPlayerInjector) player).getActionPack();
        actionPack.mount(doMount);
        return 1;
    }

    public static int lookDirection(@NotNull CommandContext<CommandSourceStack> context) {
        String direction = ModCommands.getArg(context, "direction", StringArgumentType::getString);
        if (direction == null) {
            return 0;
        }
        Direction d = getDirection(direction);
        if (d == null) {
            context.getSource().sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.invalid_direction").withStyle(ChatFormatting.RED));
            return 0;
        }
        FakePlayer player = isFakePlayerValid(context);
        if (player == null) return 0;
        PlayerActionPack actionPack = ((ServerPlayerInjector) player).getActionPack();
        actionPack.look(d);
        return 1;
    }

    public static int lookAt(@NotNull CommandContext<CommandSourceStack> context) {
        FakePlayer player = isFakePlayerValid(context);
        if (player == null) return 0;
        PlayerActionPack actionPack = ((ServerPlayerInjector) player).getActionPack();
        Vec3 vec3 = ModCommands.getArg(context, "pos", Vec3Argument::getVec3);
        if (vec3 == null) {
            context.getSource().sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.invalid_position").withStyle(ChatFormatting.RED));
            return 0;
        }
        actionPack.lookAt(vec3);
        return 1;
    }

    public static int turnRotation(@NotNull CommandContext<CommandSourceStack> context) {
        String rotation = ModCommands.getArg(context, "rotation", StringArgumentType::getString);
        if (rotation == null) {
            return 0;
        }
        int r = getAngle(rotation);
        if (r == -1) {
            context.getSource().sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.invalid_rotation").withStyle(ChatFormatting.RED));
            return 0;
        }
        FakePlayer player = isFakePlayerValid(context);
        if (player == null) return 0;
        PlayerActionPack actionPack = ((ServerPlayerInjector) player).getActionPack();
        actionPack.turn(r, 0);
        return 1;
    }

    public static int turn(@NotNull CommandContext<CommandSourceStack> context) {
        FakePlayer player = isFakePlayerValid(context);
        if (player == null) return 0;
        PlayerActionPack actionPack = ((ServerPlayerInjector) player).getActionPack();
        float x;
        float y;
        try {
            x = FloatArgumentType.getFloat(context, "x");
            y = FloatArgumentType.getFloat(context, "y");
        } catch (IllegalArgumentException ignored) {
            context.getSource().sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.invalid_rotation").withStyle(ChatFormatting.RED));
            return 0;
        }
        actionPack.turn(x, y);
        return 1;
    }

    public static int stopActions(@NotNull CommandContext<CommandSourceStack> context) {
        FakePlayer player = isFakePlayerValid(context);
        if (player == null) return 0;
        PlayerActionPack actionPack = ((ServerPlayerInjector) player).getActionPack();
        actionPack.stopAll();
        return 1;
    }

    private static @Nullable ServerPlayer isThereFakePlayer(@NotNull CommandContext<CommandSourceStack> context) {
        String name = ModCommands.getArg(context, "name", StringArgumentType::getString);
        if (name == null) {
            context.getSource().sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.invalid_name").withStyle(ChatFormatting.RED));
            return null;
        }
        PlayerList playerList = context.getSource().getServer().getPlayerList();
        ServerPlayer playerByName = playerList.getPlayerByName(name);
        if (playerByName == null) {
            context.getSource().sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.not_found").withStyle(ChatFormatting.RED));
            return null;
        }
        return playerByName;
    }

    private static @Nullable FakePlayer isFakePlayerValid(@NotNull CommandContext<CommandSourceStack> context) {
        ServerPlayer player = isThereFakePlayer(context);
        if (player == null) return null;
        if (player instanceof FakePlayer fakePlayer) return fakePlayer;
        context.getSource().sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.not_silicone_dolls", player.getName().getString()).withStyle(ChatFormatting.RED));
        return null;
    }

    public static @Nullable PlayerActionPack.ActionType getAction(String name) {
        return switch (name) {
            case "use" -> PlayerActionPack.ActionType.USE;
            case "attack" -> PlayerActionPack.ActionType.ATTACK;
            case "jump" -> PlayerActionPack.ActionType.JUMP;
            case "drop_item" -> PlayerActionPack.ActionType.DROP_ITEM;
            case "drop_stack" -> PlayerActionPack.ActionType.DROP_STACK;
            case null, default -> null;
        };
    }

    public static @Nullable Direction getDirection(String direction) {
        return switch (direction) {
            case "west" -> Direction.WEST;
            case "east" -> Direction.EAST;
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "up" -> Direction.UP;
            case "down" -> Direction.DOWN;
            case null, default -> null;
        };
    }

    public static int getAngle(String rotation) {
        return switch (rotation) {
            case "forward" -> 0;
            case "right" -> 90;
            case "back" -> 180;
            case "left" -> 270;
            case null, default -> -1;
        };
    }
}
