package dev.anvilcraft.rg.sd.util;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.rg.api.server.TranslationUtil;
import dev.anvilcraft.rg.sd.entity.PlayerActionPack;
import dev.anvilcraft.rg.tools.chest.menu.control.AutoResetButton;
import dev.anvilcraft.rg.tools.chest.menu.control.Button;
import dev.anvilcraft.rg.tools.chest.menu.control.RadioList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class FakePlayerInventoryContainer extends FakePlayerContainer {
    public final NonNullList<ItemStack> items;
    public final NonNullList<ItemStack> armor;
    public final NonNullList<ItemStack> offhand;
    private final NonNullList<ItemStack> buttons = NonNullList.withSize(13, ItemStack.EMPTY);
    private final List<NonNullList<ItemStack>> compartments;
    private final PlayerActionPack ap;
    private final RadioList hotbar;

    public FakePlayerInventoryContainer(Player player) {
        super(player);
        this.items = this.player.getInventory().items;
        this.armor = this.player.getInventory().armor;
        this.offhand = this.player.getInventory().offhand;
        this.ap = ((IServerPlayerInjector) this.player).getActionPack();
        this.compartments = ImmutableList.of(this.items, this.armor, this.offhand, this.buttons);
        this.hotbar = FakePlayerInventoryContainer.createHotbarButton(this::addButton, this.ap);
        this.createButton();
    }

    @Override
    public int getContainerSize() {
        return this.items.size() + this.armor.size() + this.offhand.size() + this.buttons.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.items) {
            if (itemStack.isEmpty()) {
                continue;
            }
            return false;
        }
        for (ItemStack itemStack : this.armor) {
            if (itemStack.isEmpty()) {
                continue;
            }
            return false;
        }
        for (ItemStack itemStack : this.offhand) {
            if (itemStack.isEmpty()) {
                continue;
            }
            return false;
        }
        return true;
    }

    public Map.Entry<NonNullList<ItemStack>, Integer> getItemSlot(int slot) {
        return switch (slot) {
            case 0 -> Map.entry(buttons, 0);
            case 1, 2, 3, 4 -> Map.entry(armor, 4 - slot);
            case 5, 6 -> Map.entry(buttons, slot - 4);
            case 7 -> Map.entry(offhand, 0);
            case 8, 9, 10, 11, 12, 13, 14, 15, 16, 17 -> Map.entry(buttons, slot - 5);
            case 18, 19, 20, 21, 22, 23, 24, 25, 26,
                 27, 28, 29, 30, 31, 32, 33, 34, 35,
                 36, 37, 38, 39, 40, 41, 42, 43, 44 -> Map.entry(items, slot - 9);
            case 45, 46, 47, 48, 49, 50, 51, 52, 53 -> Map.entry(items, slot - 45);
            default -> null;
        };
    }

    @Override
    public void clearContent() {
        for (List<ItemStack> list : this.compartments) {
            list.clear();
        }
    }

    private static @NotNull RadioList createHotbarButton(BiConsumer<Integer, Button> adder, PlayerActionPack ap) {
        List<Button> hotBarList = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            Component hotBarComponent = TranslationUtil.trans("silicone_dolls.button.hotbar", i + 1).withStyle(
                Style.EMPTY.withBold(true).withItalic(false).withColor(ChatFormatting.WHITE)
            );
            boolean defaultState = i == 0;
            Button button = new Button(defaultState, i + 1,
                hotBarComponent,
                hotBarComponent
            );
            int finalI = i + 1;
            button.addTurnOnFunction(() -> ap.setSlot(finalI));
            adder.accept(i + 9, button);
            hotBarList.add(button);
        }
        return new RadioList(hotBarList, true);
    }

    private void createButton() {
        Button stopAll = new AutoResetButton("silicone_dolls.button.action.stop_all");
        Button attackInterval12 = new Button(false, "silicone_dolls.button.action.attack_interval_12");
        Button attackContinuous = new Button(false, "silicone_dolls.button.action.attack_continuous");
        Button useContinuous = new Button(false, "silicone_dolls.button.action.use_continuous");

        stopAll.addTurnOnFunction(() -> {
            attackInterval12.turnOffWithoutFunction();
            attackContinuous.turnOffWithoutFunction();
            useContinuous.turnOffWithoutFunction();
            ap.stopAll();
        });

        attackInterval12.addTurnOnFunction(() -> {
            ap.start(PlayerActionPack.ActionType.ATTACK, PlayerActionPack.Action.interval(12));
            attackContinuous.turnOffWithoutFunction();
        });
        attackInterval12.addTurnOffFunction(() -> ap.start(PlayerActionPack.ActionType.ATTACK, PlayerActionPack.Action.once()));

        attackContinuous.addTurnOnFunction(() -> {
            ap.start(PlayerActionPack.ActionType.ATTACK, PlayerActionPack.Action.continuous());
            attackInterval12.turnOffWithoutFunction();
        });
        attackContinuous.addTurnOffFunction(() -> ap.start(PlayerActionPack.ActionType.ATTACK, PlayerActionPack.Action.once()));

        useContinuous.addTurnOnFunction(() -> ap.start(PlayerActionPack.ActionType.USE, PlayerActionPack.Action.continuous()));
        useContinuous.addTurnOffFunction(() -> ap.start(PlayerActionPack.ActionType.USE, PlayerActionPack.Action.once()));

        this.addButton(0, stopAll);
        this.addButton(5, attackInterval12);
        this.addButton(6, attackContinuous);
        this.addButton(8, useContinuous);
    }

    @Override
    public void startOpen(@NotNull Player player) {
        super.startOpen(player);
        List<Button> buttonList = this.hotbar.getButtons();
        for (int i = 0; i < buttonList.size(); i++) {
            if (i == this.player.getInventory().selected) {
                buttonList.get(i).turnOnWithoutFunction();
            } else {
                buttonList.get(i).turnOffWithoutFunction();
            }
        }
    }
}
