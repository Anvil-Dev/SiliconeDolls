package dev.anvilcraft.rg.sr;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SiliconeRubber.MODID)
public class SiliconeRubber {
    public static final String MODID = "silicone_rubber";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SiliconeRubber(IEventBus modEventBus, ModContainer modContainer) {
    }
}
