package dev.anvilcraft.rg.sd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import dev.anvilcraft.rg.api.RGAdditional;
import dev.anvilcraft.rg.api.server.ServerRGRuleManager;
import dev.anvilcraft.rg.api.server.TranslationUtil;
import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import dev.anvilcraft.rg.sd.init.ModCommands;
import dev.anvilcraft.rg.tools.serializer.DimTypeSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(SiliconeDolls.MODID)
public class SiliconeDolls implements RGAdditional {
    public static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeHierarchyAdapter(ResourceKey.class, new DimTypeSerializer())
        .registerTypeHierarchyAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
        .registerTypeHierarchyAdapter(PlayerActionPack.class, new PlayerActionPack.Serializer())
        .registerTypeHierarchyAdapter(PlayerActionPack.Action.class, new PlayerActionPack.Action.Serializer())
        .create();
    public static final String MODID = "silicone_dolls";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SiliconeDolls(@NotNull IEventBus modEventBus, @NotNull ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        modContainer.registerExtensionPoint(RGAdditional.class, this);
    }

    public void registerCommands(@NotNull RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    @Override
    public void loadServerRules(@NotNull ServerRGRuleManager manager) {
        manager.register(SiliconeDollsServerRules.class);
        TranslationUtil.loadLanguage(SiliconeDolls.class, SiliconeDolls.MODID, "en_us");
        TranslationUtil.loadLanguage(SiliconeDolls.class, SiliconeDolls.MODID, "zh_cn");
    }
}
