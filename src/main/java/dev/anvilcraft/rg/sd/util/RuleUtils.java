package dev.anvilcraft.rg.sd.util;

import dev.anvilcraft.rg.sd.SiliconeDollsServerRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public class RuleUtils {
    public static boolean openFakePlayerEnderChest(Player player) {
        if ("true".equals(SiliconeDollsServerRules.openFakePlayerEnderChest)) return true;
        return "ender_chest".equals(SiliconeDollsServerRules.openFakePlayerEnderChest) &&
            (
                player.getMainHandItem().is(Items.ENDER_CHEST) ||
                    player.getOffhandItem().is(Items.ENDER_CHEST)
            );
    }
}
