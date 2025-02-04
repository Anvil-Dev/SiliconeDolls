package dev.anvilcraft.rg.sr;

import dev.anvilcraft.rg.api.RGValidator;
import dev.anvilcraft.rg.api.Rule;
import dev.anvilcraft.rg.sr.util.CommandRuleValidator;

public class SiliconeRubberServerRules {

    @Rule(
        allowed = {"ops", "true", "false"},
        categories = SiliconeRubber.MODID,
        validator = CommandRuleValidator.class
    )
    public static String commandPlayer = "ops";

    @Rule(
        allowed = {"true", "false"},
        categories = SiliconeRubber.MODID,
        validator = RGValidator.BooleanValidator.class
    )
    public static boolean allowListingFakePlayers = false;

    @Rule(
        allowed = {"true", "false"},
        categories = SiliconeRubber.MODID,
        validator = RGValidator.BooleanValidator.class
    )
    public static boolean allowSpawningOfflinePlayers = false;
}
