package dev.anvilcraft.rg.sd.util;

import dev.anvilcraft.rg.api.RGValidator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Supplier;

public class CommandRuleValidator extends RGValidator.StringInSetValidator {
    @Override
    public Set<String> getSet() {
        return Set.of("ops", "true", "false");
    }

    public static boolean hasPermission(@NotNull Supplier<String> supplier, @NotNull CommandSourceStack stack) {
        String s = supplier.get();
        return switch (s) {
            case "ops" -> stack.hasPermission(Commands.LEVEL_ALL);
            case "true" -> true;
            default -> false;
        };
    }
}
