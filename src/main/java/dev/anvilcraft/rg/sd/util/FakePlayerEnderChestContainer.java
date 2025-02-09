package dev.anvilcraft.rg.sd.util;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.rg.sd.entity.FakePlayer;
import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import dev.anvilcraft.rg.tools.chest.menu.control.AutoResetButton;
import dev.anvilcraft.rg.tools.chest.menu.control.Button;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FakePlayerEnderChestContainer extends FakePlayerContainer {
    public final NonNullList<ItemStack> items;
    private final NonNullList<ItemStack> buttons = NonNullList.withSize(27, ItemStack.EMPTY);
    private final List<NonNullList<ItemStack>> compartments;
    private final @NotNull PlayerActionPack ap;

    public FakePlayerEnderChestContainer(Player player) {
        super(player);
        this.items = this.player.getEnderChestInventory().getItems();
        this.compartments = ImmutableList.of(this.items, this.buttons);
        this.ap = ((IServerPlayerInjector) this.player).getActionPack();
        this.createButton();
    }

    @Override
    public int getContainerSize() {
        return this.items.size() + this.buttons.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.items) {
            if (itemStack.isEmpty()) {
                continue;
            }
            return false;
        }
        return true;
    }

    public Map.Entry<NonNullList<ItemStack>, Integer> getItemSlot(int slot) {
        if (slot > 26) {
            return Map.entry(items, slot - 27);
        } else {
            return Map.entry(buttons, slot);
        }
    }

    @Override
    public void clearContent() {
        for (List<ItemStack> list : this.compartments) {
            list.clear();
        }
    }

    private void createButton() {
        List<Integer> slots = new ArrayList<>();
        Button sneakButton = new Button(false, "silicone_dolls.button.action.sneak");
        sneakButton.addTurnOnFunction(() -> this.ap.setSneaking(true));
        sneakButton.addTurnOffFunction(() -> this.ap.setSneaking(false));
        this.addButton(0, sneakButton);
        Button jumpButton = new Button(false, "silicone_dolls.button.action.jump_continuous");
        jumpButton.addTurnOnFunction(() -> this.ap.start(PlayerActionPack.ActionType.JUMP, PlayerActionPack.Action.continuous()));
        jumpButton.addTurnOffFunction(() -> this.ap.start(PlayerActionPack.ActionType.JUMP, PlayerActionPack.Action.once()));
        this.addButton(1, jumpButton);
        AutoResetButton quitButton = new AutoResetButton("silicone_dolls.button.action.quit");
        quitButton.addTurnOnFunction(() -> {
            if (this.player instanceof FakePlayer fake) fake.kill();
        });
        this.addButton(26, quitButton);
        for (Map.Entry<Integer, Button> button : super.buttons) {
            slots.add(button.getKey());
        }
        for (int i = 0; i < 27; i++) {
            if (slots.contains(i)) continue;
            this.addButton(i, AutoResetButton.NONE);
        }
    }
}
