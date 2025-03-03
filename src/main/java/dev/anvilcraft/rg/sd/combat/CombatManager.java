package dev.anvilcraft.rg.sd.combat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.anvilcraft.rg.sd.SiliconeDolls;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public class CombatManager {
    public static boolean isLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }

    public static @Nullable LiteralArgumentBuilder<CommandSourceStack> loadYSMCombat(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!CombatManager.isLoaded("yes_steve_model")) return null;
        ClassLoader loader = SiliconeDolls.class.getClassLoader();
        try {
            Class<?> modelCommand = loader.loadClass("dev.anvilcraft.rg.sd.combat.ysm.ModelCommand");
            // noinspection unchecked
            return (LiteralArgumentBuilder<CommandSourceStack>) modelCommand.getMethod("register", dispatcher.getClass()).invoke(null, dispatcher);
        } catch (Exception e) {
            SiliconeDolls.LOGGER.error(e.getMessage(), e);
            return null;
        }
    }
}
