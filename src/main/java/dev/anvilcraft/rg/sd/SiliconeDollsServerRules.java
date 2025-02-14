package dev.anvilcraft.rg.sd;

import dev.anvilcraft.rg.api.RGValidator;
import dev.anvilcraft.rg.api.Rule;

import java.util.Set;

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
    public static String commandBot = "ops";

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

    @Rule(
        allowed = {"true", "false"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.BooleanValidator.class
    )
    public static boolean openFakePlayerInventory = false;

    @Rule(
        allowed = {"ops", "true", "false", "1", "2", "3", "4"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.CommandRuleValidator.class
    )
    public static String openRealPlayerInventory = "false";

    public static class OpenFakePlayerEnderChestValidator extends RGValidator.StringInSetValidator {
        @Override
        public Set<String> getSet() {
            return Set.of("true", "false", "ender_chest");
        }
    }

    @Rule(
        allowed = {"true", "false", "ender_chest"},
        categories = SiliconeDolls.MODID,
        validator = OpenFakePlayerEnderChestValidator.class
    )
    public static String openFakePlayerEnderChest = "false";

    @Rule(
        allowed = {"true", "false"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.BooleanValidator.class
    )
    public static boolean fakePlayerResident = false;

    @Rule(
        allowed = {"true", "false"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.BooleanValidator.class
    )
    public static boolean fakePlayerReloadAction = false;

    @Rule(
        allowed = {"true", "false"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.BooleanValidator.class
    )
    public static boolean fakePlayerAutoReplenishment = false;

    @Rule(
        allowed = {"true", "false"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.BooleanValidator.class
    )
    public static boolean fakePlayerAutoReplenishmentFromShulkerBox = false;

    @Rule(
        allowed = {"true", "false"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.BooleanValidator.class
    )
    public static boolean fakePlayerAutoFish = false;

    @Rule(
        allowed = {"true", "false"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.BooleanValidator.class
    )
    public static boolean fakePlayerAutoReplaceTool = false;

    @Rule(
        allowed = {"#none", "bot_"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.StringValidator.class
    )
    public static String fakePlayerNamePrefix = "#none";

    @Rule(
        allowed = {"#none", "_fake"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.StringValidator.class
    )
    public static String fakePlayerNameSuffix = "#none";

    @Rule(
        allowed = {"true", "false"},
        categories = SiliconeDolls.MODID,
        validator = RGValidator.BooleanValidator.class
    )
    public static boolean fakePlayerSpawnNoKnockback = false;
}
