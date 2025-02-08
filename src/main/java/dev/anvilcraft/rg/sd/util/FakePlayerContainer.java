package dev.anvilcraft.rg.sd.util;

import dev.anvilcraft.rg.api.server.TranslationUtil;
import dev.anvilcraft.rg.sd.SiliconeDolls;
import dev.anvilcraft.rg.sd.SiliconeDollsServerRules;
import dev.anvilcraft.rg.sd.entity.FakePlayer;
import dev.anvilcraft.rg.tools.chest.menu.CustomChestMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class FakePlayerContainer extends CustomChestMenu {
    protected final Player player;

    public FakePlayerContainer(Player player) {
        this.player = player;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        Map.Entry<NonNullList<ItemStack>, Integer> pair = getItemSlot(slot);
        if (pair != null) {
            return pair.getKey().get(pair.getValue());
        } else {
            return ItemStack.EMPTY;
        }
    }

    public abstract Map.Entry<NonNullList<ItemStack>, Integer> getItemSlot(int slot);

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        Map.Entry<NonNullList<ItemStack>, Integer> pair = getItemSlot(slot);
        NonNullList<ItemStack> list = null;
        if (pair != null) {
            list = pair.getKey();
            slot = pair.getValue();
        }
        if (list != null && !list.get(slot).isEmpty()) {
            return ContainerHelper.removeItem(list, slot, amount);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        Map.Entry<NonNullList<ItemStack>, Integer> pair = getItemSlot(slot);
        NonNullList<ItemStack> list = null;
        if (pair != null) {
            list = pair.getKey();
            slot = pair.getValue();
        }
        if (list != null && !list.get(slot).isEmpty()) {
            ItemStack itemStack = list.get(slot);
            list.set(slot, ItemStack.EMPTY);
            return itemStack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        Map.Entry<NonNullList<ItemStack>, Integer> pair = getItemSlot(slot);
        NonNullList<ItemStack> list = null;
        if (pair != null) {
            list = pair.getKey();
            slot = pair.getValue();
        }
        if (list != null) {
            list.set(slot, stack);
        }
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.player.isAlive() && player.distanceToSqr(this.player) <= 64.0;
    }

    public static InteractionResult openInventory(@NotNull Player player, FakePlayer fakePlayer) {
        SimpleMenuProvider provider;
        if (player.isShiftKeyDown()) {
            // 打开末影箱
            if (RuleUtils.openFakePlayerEnderChest(player)) {
                provider = new SimpleMenuProvider(
                    (i, inventory, p) -> ChestMenu.sixRows(
                        i, inventory,
                        SiliconeDolls.FAKE_PLAYER_INVENTORY_CONTAINER_MAP.get(fakePlayer).getValue()
                    ),
                    TranslationUtil.trans("silicone_dolls.player.ender_chest", fakePlayer.getDisplayName())
                );
            } else {
                // 打开额外功能菜单
                provider = new SimpleMenuProvider(
                    (i, inventory, p) -> ChestMenu.threeRows(
                        i, inventory,
                        SiliconeDolls.FAKE_PLAYER_INVENTORY_CONTAINER_MAP.get(fakePlayer).getValue()
                    ),
                    TranslationUtil.trans("silicone_dolls.player.other_controller", fakePlayer.getDisplayName())
                );
            }
        } else if (SiliconeDollsServerRules.openFakePlayerInventory) {
            // 打开物品栏
            provider = new SimpleMenuProvider(
                (i, inventory, p) -> new FakePlayerInventoryMenu(
                    i, inventory,
                    SiliconeDolls.FAKE_PLAYER_INVENTORY_CONTAINER_MAP.get(fakePlayer).getKey()
                ),
                TranslationUtil.trans("silicone_dolls.player.inventory", fakePlayer.getDisplayName())
            );
        } else {
            return InteractionResult.PASS;
        }
        player.openMenu(provider);
        return InteractionResult.CONSUME;
    }
}
