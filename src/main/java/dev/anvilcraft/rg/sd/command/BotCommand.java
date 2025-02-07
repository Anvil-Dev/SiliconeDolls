package dev.anvilcraft.rg.sd.command;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.anvilcraft.rg.api.RGValidator;
import dev.anvilcraft.rg.sd.SiliconeDollsServerRules;
import dev.anvilcraft.rg.sd.tools.FilesUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BotCommand {
    public static final FilesUtil.MapFile<String, BotInfo> BOT_INFO = new FilesUtil.MapFile<>("bot", Object::toString, BotInfo.class);
    public static final FilesUtil.MapFile<String, BotGroupInfo> BOT_GROUP_INFO = new FilesUtil.MapFile<>("botGroup", Object::toString, BotGroupInfo.class);

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(
            Commands.literal("bot")
                .requires(source -> RGValidator.CommandRuleValidator.hasPermission(() -> SiliconeDollsServerRules.commandBotList, source))
                .then(
                    Commands.literal("list")
                        .then(
                            Commands.argument("page", IntegerArgumentType.integer(1))
                        )
                )
                .then(
                    Commands.literal("add")
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .then(
                                    Commands.argument("desc", StringArgumentType.greedyString())
                                )
                        )
                )
                .then(
                    Commands.literal("load")
                        .then(
                            Commands.argument("player", StringArgumentType.string())
                        )
                )
                .then(
                    Commands.literal("remove")
                        .then(
                            Commands.argument("player", StringArgumentType.string())
                        )
                )
        );
        dispatcher.register(
            Commands.literal("botGroup")
                .requires(source -> RGValidator.CommandRuleValidator.hasPermission(() -> SiliconeDollsServerRules.commandBotList, source))
                .then(
                    Commands.literal("create")
                        .then(
                            Commands.argument("name", StringArgumentType.greedyString())
                        )
                )
                .then(
                    Commands.literal("list")
                        .then(
                            Commands.argument("page", IntegerArgumentType.integer(1))
                        )
                )
                .then(
                    Commands.literal("remove")
                        .then(
                            Commands.argument("name", StringArgumentType.greedyString())
                        )
                )
                .then(
                    Commands.literal("add")
                        .then(
                            Commands.argument("bot", StringArgumentType.string())
                                .then(
                                    Commands.argument("group", StringArgumentType.greedyString())
                                )
                        )
                )
                .then(
                    Commands.literal("remove")
                        .then(
                            Commands.argument("bot", StringArgumentType.string())
                                .then(
                                    Commands.argument("group", StringArgumentType.greedyString())
                                )
                        )
                )
                .then(
                    Commands.literal("load")
                        .then(
                            Commands.argument("group", StringArgumentType.greedyString())
                        )
                )
                .then(
                    Commands.literal("unload")
                        .then(
                            Commands.argument("group", StringArgumentType.greedyString())
                        )
                )
                .then(
                    Commands.literal("info")
                        .then(
                            Commands.argument("group", StringArgumentType.greedyString())
                        )
                )
        );
    }



    public record BotInfo(
        String name,
        String desc,
        Vec3 pos,
        Vec2 facing,
        @SerializedName("dim_type") ResourceKey<Level> dimType,
        GameType mode,
        boolean flying,
        JsonObject actions
    ) {
    }

    public record BotGroupInfo(
        String name,
        List<String> bots
    ) {
    }
}
