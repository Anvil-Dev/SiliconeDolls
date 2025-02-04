package dev.anvilcraft.rg.sr.util;

import dev.anvilcraft.rg.sr.entity.PlayerActionPack;
import org.jetbrains.annotations.NotNull;

public interface ServerPlayerInjector {
    @NotNull PlayerActionPack getActionPack();
}
