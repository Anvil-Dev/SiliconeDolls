package dev.anvilcraft.rg.sr;

import dev.anvilcraft.rg.api.RGValidator;
import dev.anvilcraft.rg.api.Rule;
import dev.anvilcraft.rg.sr.util.CommandRuleValidator;

public class SiliconeDollsServerRules {

    @Rule(
        allowed = {"ops", "true", "false"},
        categories = SiliconeDolls.MODID,
        validator = CommandRuleValidator.class
    )
    public static String commandPlayer = "ops";

    @Rule(
        allowed = {"true", "false"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.BooleanValidator.class
    )
    public static boolean allowListingFakePlayers = false;

    @Rule(
        allowed = {"true", "false"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.BooleanValidator.class
    )
    public static boolean allowSpawningOfflinePlayers = false;
}
