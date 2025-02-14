package dev.anvilcraft.rg.sd.util;

import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import org.jetbrains.annotations.NotNull;

public interface IServerPlayerInjector {
    default @NotNull PlayerActionPack getActionPack() {
        throw new AssertionError();
    }

    default @NotNull PlayerInventoryContainer getInventoryContainer() {
        throw new AssertionError();
    }

    default @NotNull PlayerEnderChestContainer getEnderChestContainer() {
        throw new AssertionError();
    }
}
