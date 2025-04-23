package dev.anvilcraft.rg.sd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import dev.anvilcraft.rg.api.RGAdditional;
import dev.anvilcraft.rg.api.server.ServerRGRuleManager;
import dev.anvilcraft.rg.api.server.TranslationUtil;
import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import dev.anvilcraft.rg.sd.util.DimTypeSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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

    public SiliconeDolls(@NotNull @SuppressWarnings("unused") IEventBus modEventBus, @NotNull ModContainer modContainer) {
        modContainer.registerExtensionPoint(RGAdditional.class, this);
    }

    @Override
    public void loadServerRules(@NotNull ServerRGRuleManager manager) {
        manager.register(SiliconeDollsServerRules.class);
        TranslationUtil.loadLanguage(SiliconeDolls.class, SiliconeDolls.MODID, "en_us");
        TranslationUtil.loadLanguage(SiliconeDolls.class, SiliconeDolls.MODID, "zh_cn");
    }
}
