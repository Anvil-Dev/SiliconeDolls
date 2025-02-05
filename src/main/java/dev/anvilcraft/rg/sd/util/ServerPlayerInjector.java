package dev.anvilcraft.rg.sd.util;

import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import org.jetbrains.annotations.NotNull;

public interface ServerPlayerInjector {
    @NotNull PlayerActionPack getActionPack();
}
