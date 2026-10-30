package dev.anvilcraft.rg.sd.util;

import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import dev.anvilcraft.rg.sd.tool.PlayerEnderChestContainer;
import dev.anvilcraft.rg.sd.tool.PlayerInventoryContainer;

public interface IServerPlayerInjector {
    default PlayerActionPack getActionPack() {
        throw new AssertionError();
    }

    default PlayerInventoryContainer getInventoryContainer() {
        throw new AssertionError();
    }

    default PlayerEnderChestContainer getEnderChestContainer() {
        throw new AssertionError();
    }
}
