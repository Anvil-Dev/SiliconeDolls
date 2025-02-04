package dev.anvilcraft.rg.sr.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.anvilcraft.rg.sr.command.PlayerCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        PlayerCommand.register(dispatcher);
    }

    public static @NotNull SuggestionProvider<CommandSourceStack> suggest(String... strings) {
        return ModCommands.suggest(List.of(strings));
    }

    public static @NotNull SuggestionProvider<CommandSourceStack> suggest(Iterable<String> strings) {
        return (context, builder) -> SharedSuggestionProvider.suggest(strings, builder);
    }

    public static <T> @Nullable T getArg(CommandContext<CommandSourceStack> context, String name, BiFunction<CommandContext<CommandSourceStack>, String, T> getter) {
        try {
            return getter.apply(context, name);
        } catch (Exception e) {
            return null;
        }
    }
}
