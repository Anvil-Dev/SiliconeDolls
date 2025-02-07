package dev.anvilcraft.rg.sd.tools;

import com.google.gson.JsonObject;
import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import dev.anvilcraft.rg.sd.util.ServerPlayerInjector;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class FakePlayerSerializer {
    public static @NotNull JsonObject actionPackToJson(PlayerActionPack actionPack) {
        JsonObject object = new JsonObject();
        Map<PlayerActionPack.ActionType, PlayerActionPack.Action> actions = actionPack.getActions();
        PlayerActionPack.Action attack = actions.get(PlayerActionPack.ActionType.ATTACK);
        PlayerActionPack.Action use = actions.get(PlayerActionPack.ActionType.USE);
        PlayerActionPack.Action jump = actions.get(PlayerActionPack.ActionType.JUMP);

        if (attack != null && !attack.done) {
            object.addProperty("attack", attack.interval * ((attack.isContinuous) ? -1 : 1));
        }
        if (use != null && !use.done) {
            object.addProperty("use", use.interval * ((use.isContinuous) ? -1 : 1));
        }
        if (jump != null && !jump.done) {
            object.addProperty("jump", jump.interval * ((jump.isContinuous) ? -1 : 1));
        }
        object.addProperty("sneaking", actionPack.isSneaking());
        object.addProperty("sprinting", actionPack.isSprinting());
        object.addProperty("forward", actionPack.getForward());
        object.addProperty("strafing", actionPack.getStrafing());
        return object;
    }

    public static void applyActionPackFromJson(JsonObject actions, ServerPlayer player) {
        PlayerActionPack ap = ((ServerPlayerInjector) player).getActionPack();
        if (actions.has("sneaking")) ap.setSneaking(actions.get("sneaking").getAsBoolean());
        if (actions.has("sprinting")) ap.setSprinting(actions.get("sprinting").getAsBoolean());
        if (actions.has("forward")) ap.setForward(actions.get("forward").getAsFloat());
        if (actions.has("strafing")) ap.setStrafing(actions.get("strafing").getAsFloat());
        if (actions.has("attack")) {
            int attack = actions.get("attack").getAsInt();
            if (attack < 0) {
                ap.start(PlayerActionPack.ActionType.ATTACK, PlayerActionPack.Action.continuous());
            } else {
                ap.start(PlayerActionPack.ActionType.ATTACK, PlayerActionPack.Action.interval(attack));
            }
        }
        if (actions.has("use")) {
            int use = actions.get("use").getAsInt();
            if (use < 0) {
                ap.start(PlayerActionPack.ActionType.USE, PlayerActionPack.Action.continuous());
            } else {
                ap.start(PlayerActionPack.ActionType.USE, PlayerActionPack.Action.interval(use));
            }
        }
        if (actions.has("jump")) {
            int jump = actions.get("jump").getAsInt();
            if (jump < 0) {
                ap.start(PlayerActionPack.ActionType.JUMP, PlayerActionPack.Action.continuous());
            } else {
                ap.start(PlayerActionPack.ActionType.JUMP, PlayerActionPack.Action.interval(jump));
            }
        }
    }
}
