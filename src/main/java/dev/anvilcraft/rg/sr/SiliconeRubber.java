package dev.anvilcraft.rg.sr;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.rg.sr.init.ModCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(SiliconeRubber.MODID)
public class SiliconeRubber {
    public static final String MODID = "silicone_rubber";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SiliconeRubber(@NotNull IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    public void registerCommands(@NotNull RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}
