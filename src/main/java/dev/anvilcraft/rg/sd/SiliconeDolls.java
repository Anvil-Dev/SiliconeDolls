package dev.anvilcraft.rg.sd;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.rg.sd.init.ModCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(SiliconeDolls.MODID)
public class SiliconeDolls {
    public static final String MODID = "silicone_dolls";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SiliconeDolls(@NotNull IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    public void registerCommands(@NotNull RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}
