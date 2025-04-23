package dev.anvilcraft.rg.sd.command;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.anvilcraft.rg.api.RGValidator;
import dev.anvilcraft.rg.api.server.TranslationUtil;
import dev.anvilcraft.rg.sd.SiliconeDolls;
import dev.anvilcraft.rg.sd.SiliconeDollsServerRules;
import dev.anvilcraft.rg.sd.entity.FakeClientConnection;
import dev.anvilcraft.rg.sd.entity.FakePlayer;
import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import dev.anvilcraft.rg.sd.init.ModCommands;
import dev.anvilcraft.rg.sd.mixin.EntityInvoker;
import dev.anvilcraft.rg.sd.mixin.PlayerAccessor;
import dev.anvilcraft.rg.sd.util.IServerPlayerInjector;
import dev.anvilcraft.rg.tools.FilesUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings({"SameParameterValue", "unused"})
public class BotCommand {
    public static final FilesUtil.MapFile<String, BotInfo> BOT_INFO = new FilesUtil.MapFile<>("bot", Object::toString, BotInfo.class);
    public static final FilesUtil.MapFile<String, BotGroupInfo> BOT_GROUP_INFO = new FilesUtil.MapFile<>("botGroup", Object::toString, BotGroupInfo.class);

    static {
        BOT_INFO.setGson(
            builder -> builder
                .registerTypeHierarchyAdapter(PlayerActionPack.class, new PlayerActionPack.Serializer())
                .registerTypeHierarchyAdapter(PlayerActionPack.Action.class, new PlayerActionPack.Action.Serializer())
        );
        BOT_GROUP_INFO.setGson(
            builder -> builder
                .registerTypeHierarchyAdapter(PlayerActionPack.class, new PlayerActionPack.Serializer())
                .registerTypeHierarchyAdapter(PlayerActionPack.Action.class, new PlayerActionPack.Action.Serializer())
        );
    }

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("bot")
                .executes(BotCommand::list)
                .requires(source -> RGValidator.CommandRuleValidator.hasPermission(() -> SiliconeDollsServerRules.commandBot, source))
                .then(
                    Commands.literal("list")
                        .executes(BotCommand::list)
                        .then(
                            Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(BotCommand::list)
                        )
                )
                .then(
                    Commands.literal("add")
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .then(
                                    Commands.argument("desc", StringArgumentType.greedyString())
                                        .executes(BotCommand::add)
                                )
                        )
                )
                .then(
                    Commands.literal("load")
                        .then(
                            Commands.argument("player", StringArgumentType.string())
                                .suggests(BotCommand.suggestBot())
                                .executes(BotCommand::load)
                        )
                )
                .then(
                    Commands.literal("remove")
                        .then(
                            Commands.argument("player", StringArgumentType.string())
                                .suggests(BotCommand.suggestBot())
                                .executes(BotCommand::remove)
                        )
                )
                .then(
                    Commands.literal("group")
                        .requires(source -> RGValidator.CommandRuleValidator.hasPermission(() -> SiliconeDollsServerRules.commandBot, source))
                        .executes(BotCommand::groupList)
                        .then(
                            Commands.literal("create")
                                .then(
                                    Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(BotCommand::groupCreate)
                                )
                        )
                        .then(
                            Commands.literal("list")
                                .executes(BotCommand::groupList)
                                .then(
                                    Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(BotCommand::groupList)
                                )
                        )
                        .then(
                            Commands.literal("remove")
                                .then(
                                    Commands.argument("name", StringArgumentType.greedyString())
                                        .suggests(BotCommand.suggestBotGroup())
                                        .executes(BotCommand::groupRemove)
                                )
                        )
                        .then(
                            Commands.literal("add")
                                .then(
                                    Commands.argument("bot", StringArgumentType.string())
                                        .suggests(BotCommand.suggestBot())
                                        .then(
                                            Commands.argument("group", StringArgumentType.greedyString())
                                                .suggests(BotCommand.suggestBotGroup())
                                                .executes(BotCommand::groupAddBot)
                                        )
                                )
                        )
                        .then(
                            Commands.literal("remove")
                                .then(
                                    Commands.argument("bot", StringArgumentType.string())
                                        .suggests(BotCommand.suggestBot())
                                        .then(
                                            Commands.argument("group", StringArgumentType.greedyString())
                                                .suggests(BotCommand.suggestBotGroup())
                                                .executes(BotCommand::groupRemoveBot)
                                        )
                                )
                        )
                        .then(
                            Commands.literal("load")
                                .then(
                                    Commands.argument("group", StringArgumentType.greedyString())
                                        .suggests(BotCommand.suggestBotGroup())
                                        .executes(BotCommand::groupLoadBot)
                                )
                        )
                        .then(
                            Commands.literal("unload")
                                .then(
                                    Commands.argument("group", StringArgumentType.greedyString())
                                        .suggests(BotCommand.suggestBotGroup())
                                        .executes(BotCommand::groupUnloadBot)
                                )
                        )
                        .then(
                            Commands.literal("info")
                                .then(
                                    Commands.argument("group", StringArgumentType.greedyString())
                                        .suggests(BotCommand.suggestBotGroup())
                                        .executes(BotCommand::groupInfo)
                                )
                        )
                )
        );
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        BOT_INFO.init(context);
        int page;
        try {
            page = IntegerArgumentType.getInteger(context, "page");
        } catch (IllegalArgumentException ignored) {
            page = 1;
        }
        final int pageSize = 8;
        int size = BOT_INFO.map.size();
        int maxPage = size / pageSize + 1;
        if (page > maxPage) {
            context.getSource().sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.no_such_page", page));
            return 0;
        }
        BotInfo[] botInfos = BOT_INFO.map.values().toArray(new BotInfo[0]);

        context.getSource().sendSystemMessage(
            TranslationUtil.trans("silicone_dolls.commands.title.bot_list", page, maxPage)
                .withStyle(ChatFormatting.YELLOW)
        );
        for (int i = (page - 1) * pageSize; i < size && i < page * pageSize; i++) {
            context.getSource().sendSystemMessage(botToComponent(botInfos[i]));
        }
        listComponent(context, page, maxPage, "/bot list");
        return 1;
    }

    private static @NotNull MutableComponent botToComponent(@NotNull BotInfo botInfo) {
        MutableComponent desc = Component.literal(botInfo.desc).withStyle(
            Style.EMPTY
                .applyFormat(ChatFormatting.GRAY)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(botInfo.name)))
        );
        boolean notOnline = BOT_INFO.server.getPlayerList().getPlayerByName(botInfo.name) == null;
        MutableComponent load = Component.literal("[↑]").withStyle(
            Style.EMPTY
                .applyFormat(notOnline ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TranslationUtil.trans("silicone_dolls.commands.button.bot.load")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bot load %s".formatted(botInfo.name)))
        );
        MutableComponent remove = Component.literal("[↓]").withStyle(
            Style.EMPTY
                .applyFormat(notOnline ? ChatFormatting.GRAY : ChatFormatting.RED)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TranslationUtil.trans("silicone_dolls.commands.button.bot.unload")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/player %s kill".formatted(botInfo.name)))
        );
        MutableComponent delete = Component.literal("[\uD83D\uDDD1]").withStyle(
            Style.EMPTY
                .applyFormat(ChatFormatting.RED)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TranslationUtil.trans("silicone_dolls.commands.button.bot.remove")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bot remove %s".formatted(botInfo.name)))
        );
        MutableComponent component = Component.literal("▶ ")
            .withStyle(notOnline ? ChatFormatting.RED : ChatFormatting.GREEN)
            .append(desc);
        component.append(" ").append(load);
        component.append(" ").append(remove);
        return component.append(" ").append(delete);
    }

    private static void listComponent(@NotNull CommandContext<CommandSourceStack> context, int page, int maxPage, String command) {
        Component prevPage = page <= 1 ?
            Component.literal("<<<").withStyle(ChatFormatting.GRAY) :
            Component.literal("<<<").withStyle(
                Style.EMPTY
                    .applyFormat(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command + " " + (page - 1)))
            );
        Component nextPage = page >= maxPage ?
            Component.literal(">>>").withStyle(ChatFormatting.GRAY) :
            Component.literal(">>>").withStyle(
                Style.EMPTY
                    .applyFormat(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command + " " + (page + 1)))
            );
        context.getSource().sendSystemMessage(
            Component.literal("=======")
                .withStyle(ChatFormatting.YELLOW)
                .append(" ")
                .append(prevPage)
                .append(" ")
                .append(TranslationUtil.trans("silicone_dolls.commands.title.page", page, maxPage).withStyle(ChatFormatting.YELLOW))
                .append(" ")
                .append(nextPage)
                .append(" ")
                .append(Component.literal("=======").withStyle(ChatFormatting.YELLOW))
        );
    }

    private static @NotNull SuggestionProvider<CommandSourceStack> suggestBot() {
        return ModCommands.suggest(BOT_INFO.map.keySet());
    }

    private static @NotNull SuggestionProvider<CommandSourceStack> suggestBotGroup() {
        return ModCommands.suggest(BOT_GROUP_INFO.map.keySet());
    }

    private static boolean load(String name, Consumer<Component> failure) {
        if (BOT_INFO.server.getPlayerList().getPlayerByName(name) != null) {
            failure.accept(TranslationUtil.trans("silicone_dolls.commands.tips.logged", name));
            return false;
        }
        BotInfo botInfo = BOT_INFO.map.getOrDefault(name, null);
        if (botInfo == null) {
            failure.accept(TranslationUtil.trans("silicone_dolls.commands.tips.not_exist", name));
            return false;
        }
        boolean success = false;
        try {
            ServerLevel worldIn = BOT_INFO.server.getLevel(botInfo.dimType);
            GameProfileCache.setUsesAuthentication(false);
            GameProfile gameprofile;
            try {
                GameProfileCache profileCache = BOT_INFO.server.getProfileCache();
                if (profileCache == null) gameprofile = null;
                else gameprofile = profileCache.get(name).orElse(null);
                if (gameprofile == null) {
                    if (!SiliconeDollsServerRules.allowSpawningOfflinePlayers) return false;
                    gameprofile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);
                }
                GameProfile finalGP = gameprofile;
                SkullBlockEntity.fetchGameProfile(gameprofile.getName()).thenAcceptAsync((p) -> {
                    GameProfile current = finalGP;
                    if (p.isPresent()) current = p.get();
                    if (worldIn == null) return;
                    FakePlayer instance = FakePlayer.create(BOT_INFO.server, worldIn, current, ClientInformation.createDefault(), false);
                    instance.fixStartingPosition = () -> instance.moveTo(botInfo.pos.x, botInfo.pos.y, botInfo.pos.z, botInfo.facing.y, botInfo.facing.x);
                    BOT_INFO.server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), instance, new CommonListenerCookie(current, 0, instance.clientInformation(), false, ConnectionType.OTHER));
                    instance.teleportTo(worldIn, botInfo.pos.x, botInfo.pos.y, botInfo.pos.z, botInfo.facing.y, botInfo.facing.x);
                    instance.setHealth(20.0F);
                    ((EntityInvoker) instance).invokerUnsetRemoved();
                    AttributeInstance attribute = instance.getAttribute(Attributes.STEP_HEIGHT);
                    if (attribute != null) attribute.setBaseValue(0.6000000238418579);
                    instance.gameMode.changeGameModeForPlayer(botInfo.mode);
                    BOT_INFO.server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(instance, (byte) ((int) (instance.yHeadRot * 256.0F / 360.0F))), botInfo.dimType);
                    BOT_INFO.server.getPlayerList().broadcastAll(new ClientboundTeleportEntityPacket(instance), botInfo.dimType);
                    instance.getEntityData().set(PlayerAccessor.getCustomisationData(), (byte) 127);
                    instance.getAbilities().flying = botInfo.flying;
                    PlayerActionPack actionPack = botInfo.actions;
                    ((IServerPlayerInjector) instance).getActionPack().copyFrom(actionPack);
                }, BOT_INFO.server);
                success = true;
            } finally {
                GameProfileCache.setUsesAuthentication(BOT_INFO.server.isDedicatedServer() && BOT_INFO.server.usesAuthentication());
            }
        } catch (Exception e) {
            SiliconeDolls.LOGGER.error(e.getMessage(), e);
        }
        if (!success) failure.accept(TranslationUtil.trans("silicone_dolls.commands.tips.not_load", name));
        return success;
    }

    private static int load(CommandContext<CommandSourceStack> context) {
        BOT_INFO.init(context);
        CommandSourceStack source = context.getSource();
        String name = ModCommands.getArg(context, "player", StringArgumentType::getString);
        boolean success = load(name, source::sendFailure);
        return success ? 1 : 0;
    }

    @SuppressWarnings("resource")
    private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BOT_INFO.init(context);
        CommandSourceStack source = context.getSource();
        ServerPlayer p;
        if (!((p = EntityArgument.getPlayer(context, "player")) instanceof FakePlayer player)) {
            source.sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.not_fake", p.getGameProfile().getName()));
            return 0;
        }
        String name = player.getGameProfile().getName();
        if (BOT_INFO.map.containsKey(name)) {
            source.sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.already_save", name));
            return 0;
        }
        BotCommand.BOT_INFO.map.put(
            name,
            new BotInfo(
                name,
                StringArgumentType.getString(context, "desc"),
                player.position(),
                player.getRotationVector(),
                player.level().dimension(),
                player.gameMode.getGameModeForPlayer(),
                player.getAbilities().flying,
                ((IServerPlayerInjector) player).getActionPack()
            )
        );
        BOT_INFO.save();
        source.sendSuccess(() -> TranslationUtil.trans("silicone_dolls.commands.tips.already_save", name), false);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        BOT_INFO.init(context);
        String name = StringArgumentType.getString(context, "player");
        BotInfo remove = BotCommand.BOT_INFO.map.remove(name);
        if (remove == null) {
            context.getSource().sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.not_exist", name));
            return 0;
        }
        context.getSource().sendSuccess(() -> TranslationUtil.trans("silicone_dolls.commands.tips.saved", name), false);
        BOT_INFO.save();
        return 1;
    }

    private static boolean groupInit(CommandContext<CommandSourceStack> context) {
        BOT_GROUP_INFO.init(context);
        BOT_INFO.init(context);
        CommandSourceStack source = context.getSource();
        String groupName = StringArgumentType.getString(context, "group");
        if (!BOT_GROUP_INFO.map.containsKey(groupName)) {
            source.sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.group_not_found", groupName));
            return true;
        }
        List<String> botNames = BOT_GROUP_INFO.map.get(groupName).bots;
        List<String> failedBots = new ArrayList<>();
        for (String botName : botNames) {
            if (!BOT_INFO.map.containsKey(botName)) {
                failedBots.add(botName);
            }
        }
        botNames.removeAll(failedBots);
        BOT_GROUP_INFO.map.put(
            groupName,
            new BotGroupInfo(groupName, botNames)
        );
        BOT_GROUP_INFO.save();
        return false;
    }

    private static int groupInfo(CommandContext<CommandSourceStack> context) {
        if (groupInit(context)) return 0;
        String groupName = StringArgumentType.getString(context, "group");
        int page;
        try {
            page = IntegerArgumentType.getInteger(context, "page");
        } catch (IllegalArgumentException ignored) {
            page = 1;
        }
        final int pageSize = 8;
        int size = BOT_GROUP_INFO.map.get(groupName).bots.size();
        int maxPage = size / pageSize + 1;
        if (page > maxPage) {
            context.getSource().sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.no_such_page", page));
            return 0;
        }
        ArrayList<BotInfo> botInfos = new ArrayList<>();
        for (String botName : BOT_GROUP_INFO.map.get(groupName).bots) {
            botInfos.add(BOT_INFO.map.get(botName));
        }
        context.getSource().sendSystemMessage(
            TranslationUtil.trans("silicone_dolls.commands.title.bot_group_info", groupName, page, maxPage)
                .withStyle(ChatFormatting.YELLOW)
        );
        for (int i = (page - 1) * pageSize; i < size && i < page * pageSize; i++) {
            context.getSource().sendSystemMessage(botToComponent(botInfos.get(i)));
        }
        listComponent(context, page, maxPage, "/bot group show");
        return 1;
    }

    private static int groupUnloadBot(CommandContext<CommandSourceStack> context) {
        if (groupInit(context)) return 0;
        CommandSourceStack source = context.getSource();
        String groupName = StringArgumentType.getString(context, "group");
        List<String> botNames = BOT_GROUP_INFO.map.get(groupName).bots;
        for (String botName : botNames) {
            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(botName);
            if (player == null) continue;
            if (!(player instanceof FakePlayer fake)) continue;
            fake.kill();
        }
        return 1;
    }

    private static int groupLoadBot(CommandContext<CommandSourceStack> context) {
        BOT_GROUP_INFO.init(context);
        BOT_INFO.init(context);
        CommandSourceStack source = context.getSource();
        String groupName = StringArgumentType.getString(context, "group");
        if (!BOT_GROUP_INFO.map.containsKey(groupName)) {
            source.sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.group_not_found", groupName));
            return 0;
        }
        List<String> botNames = BOT_GROUP_INFO.map.get(groupName).bots;
        List<String> failedBots = new ArrayList<>();
        for (String botName : new ArrayList<>(botNames)) {
            if (!BOT_INFO.map.containsKey(botName)) {
                failedBots.add(botName);
                continue;
            }
            load(botName, source::sendFailure);
        }
        botNames.removeAll(failedBots);
        BOT_GROUP_INFO.map.put(
            groupName,
            new BotGroupInfo(groupName, botNames)
        );
        BOT_GROUP_INFO.save();
        return 1;
    }

    private static int groupRemoveBot(CommandContext<CommandSourceStack> context) {
        BOT_GROUP_INFO.init(context);
        CommandSourceStack source = context.getSource();
        String groupName = StringArgumentType.getString(context, "group");
        String botName = StringArgumentType.getString(context, "bot");
        if (!BOT_GROUP_INFO.map.containsKey(groupName)) {
            source.sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.group_not_found", groupName));
            return 0;
        }
        List<String> botNames = BOT_GROUP_INFO.map.get(groupName).bots;
        if (!botNames.contains(botName)) {
            source.sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.bot_not_in", botName, groupName));
            return 0;
        }
        botNames.remove(botName);
        BotCommand.BOT_GROUP_INFO.map.put(
            groupName,
            new BotGroupInfo(
                groupName,
                botNames
            )
        );
        BOT_GROUP_INFO.save();
        source.sendSuccess(() -> TranslationUtil.trans("silicone_dolls.commands.tips.bot_removed", botName, groupName), false);
        return 1;
    }

    private static int groupAddBot(CommandContext<CommandSourceStack> context) {
        BOT_GROUP_INFO.init(context);
        BOT_INFO.init(context);
        CommandSourceStack source = context.getSource();
        String groupName = StringArgumentType.getString(context, "group");
        String botName = StringArgumentType.getString(context, "bot");
        if (!BOT_INFO.map.containsKey(botName)) {
            source.sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.bot_not_found", botName));

            return 0;
        }
        if (!BOT_GROUP_INFO.map.containsKey(groupName)) {
            source.sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.group_not_found", groupName));
            return 0;
        }
        List<String> botNames = BOT_GROUP_INFO.map.get(groupName).bots;
        if (botNames.contains(botName)) {
            source.sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.bot_already_added", botName));
            return 0;
        }
        botNames.add(botName);
        BotCommand.BOT_GROUP_INFO.map.put(
            groupName,
            new BotGroupInfo(
                groupName,
                botNames
            )
        );
        BOT_GROUP_INFO.save();
        source.sendSuccess(() -> TranslationUtil.trans("silicone_dolls.commands.tips.bot_added", botName, groupName), false);
        return 1;
    }

    private static int groupCreate(CommandContext<CommandSourceStack> context) {
        BOT_GROUP_INFO.init(context);
        CommandSourceStack source = context.getSource();
        String groupName = StringArgumentType.getString(context, "name");
        if (BOT_GROUP_INFO.map.containsKey(groupName)) {
            source.sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.group_already_exists", groupName));
            return 0;
        }
        BOT_GROUP_INFO.map.put(
            groupName,
            new BotGroupInfo(groupName, new ArrayList<>())
        );
        BOT_GROUP_INFO.save();
        source.sendSuccess(() -> TranslationUtil.trans("silicone_dolls.commands.tips.group_created_successfully", groupName), false);
        return 1;
    }

    private static int groupRemove(CommandContext<CommandSourceStack> context) {
        BOT_GROUP_INFO.init(context);
        String name = StringArgumentType.getString(context, "name");
        BotGroupInfo remove = BotCommand.BOT_GROUP_INFO.map.remove(name);
        if (remove == null) {
            context.getSource().sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.group_not_exists", name));
            return 0;
        }
        context.getSource().sendSuccess(() -> TranslationUtil.trans("silicone_dolls.commands.tips.group_removed_player", name), false);
        BOT_GROUP_INFO.save();
        return 1;
    }

    private static int groupList(CommandContext<CommandSourceStack> context) {
        BOT_GROUP_INFO.init(context);
        int page;
        try {
            page = IntegerArgumentType.getInteger(context, "page");
        } catch (IllegalArgumentException ignored) {
            page = 1;
        }
        final int pageSize = 8;
        int size = BOT_GROUP_INFO.map.size();
        int maxPage = size / pageSize + 1;
        if (page > maxPage) {
            context.getSource().sendFailure(TranslationUtil.trans("silicone_dolls.commands.tips.no_such_page", page));
            return 0;
        }
        BotGroupInfo[] botGroupInfos = BOT_GROUP_INFO.map.values().toArray(new BotGroupInfo[0]);
        context.getSource().sendSystemMessage(
            TranslationUtil.trans("silicone_dolls.commands.title.bot_group_list", page, maxPage)
                .withStyle(ChatFormatting.YELLOW)
        );
        for (int i = (page - 1) * pageSize; i < size && i < page * pageSize; i++) {
            context.getSource().sendSystemMessage(botGroupToComponent(botGroupInfos[i]));
        }
        listComponent(context, page, maxPage, "/bot group list");
        return 1;
    }

    private static @NotNull MutableComponent botGroupToComponent(@NotNull BotGroupInfo botGroupInfo) {
        MutableComponent name = Component.literal(botGroupInfo.name).withStyle(
            Style.EMPTY
                .applyFormat(ChatFormatting.GRAY)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(botGroupInfo.name)))
        );
        MutableComponent load = Component.literal("[↑]").withStyle(
            Style.EMPTY
                .applyFormat(ChatFormatting.GREEN)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TranslationUtil.trans("silicone_dolls.commands.button.bot_group.load")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bot group load %s".formatted(botGroupInfo.name)))
        );
        MutableComponent remove = Component.literal("[↓]").withStyle(
            Style.EMPTY
                .applyFormat(ChatFormatting.RED)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TranslationUtil.trans("silicone_dolls.commands.button.bot_group.unload")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bot group unload %s".formatted(botGroupInfo.name)))
        );
        MutableComponent info = Component.literal("[i]").withStyle(
            Style.EMPTY
                .applyFormat(ChatFormatting.RED)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TranslationUtil.trans("silicone_dolls.commands.button.bot_group.info")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bot group info %s".formatted(botGroupInfo.name)))
        );
        MutableComponent delete = Component.literal("[\uD83D\uDDD1]").withStyle(
            Style.EMPTY
                .applyFormat(ChatFormatting.RED)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TranslationUtil.trans("silicone_dolls.commands.button.bot_group.remove")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bot group remove %s".formatted(botGroupInfo.name)))
        );
        MutableComponent component = Component.literal("▶ ").append(name);
        component.append(" ").append(load);
        component.append(" ").append(remove);
        component.append(" ").append(info);
        return component.append(" ").append(delete);
    }


    public record BotInfo(
        String name,
        String desc,
        Vec3 pos,
        Vec2 facing,
        @SerializedName("dim_type") ResourceKey<Level> dimType,
        GameType mode,
        boolean flying,
        PlayerActionPack actions
    ) {
    }

    public record BotGroupInfo(
        String name,
        List<String> bots
    ) {
    }
}