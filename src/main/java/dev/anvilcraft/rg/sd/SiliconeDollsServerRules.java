package dev.anvilcraft.rg.sd;

import dev.anvilcraft.rg.api.RGValidator;
import dev.anvilcraft.rg.api.Rule;

public class SiliconeDollsServerRules {

    @Rule(
        allowed = {"ops", "true", "false", "1", "2", "3", "4"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.CommandRuleValidator.class
    )
    public static String commandPlayer = "ops";

    @Rule(
        allowed = {"ops", "true", "false", "1", "2", "3", "4"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.CommandRuleValidator.class
    )
    public static String commandBotList = "ops";

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
