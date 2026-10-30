package dev.anvilcraft.rg.sd.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class InventoryUtil {
    public static NonNullList<ItemStack> getItems(Player player) {
        return player.getInventory().getNonEquipmentItems();
    }

    public static NonNullList<ItemStack> getArmor(Player player) {
        return new NonNullList<>(
            List.of(
                player.getItemBySlot(EquipmentSlot.FEET),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.HEAD)
            ),
            ItemStack.EMPTY
        ) {
            @Override
            public ItemStack get(int index) {
                return switch (index) {
                    case 3 -> player.getItemBySlot(EquipmentSlot.HEAD);
                    case 2 -> player.getItemBySlot(EquipmentSlot.CHEST);
                    case 1 -> player.getItemBySlot(EquipmentSlot.LEGS);
                    case 0 -> player.getItemBySlot(EquipmentSlot.FEET);
                    default -> ItemStack.EMPTY;
                };
            }

            @Override
            public ItemStack set(int index, ItemStack stack) {
                switch (index) {
                    case 3 -> player.setItemSlot(EquipmentSlot.HEAD, stack);
                    case 2 -> player.setItemSlot(EquipmentSlot.CHEST, stack);
                    case 1 -> player.setItemSlot(EquipmentSlot.LEGS, stack);
                    case 0 -> player.setItemSlot(EquipmentSlot.FEET, stack);
                }
                return stack;
            }
        };
    }

    public static NonNullList<ItemStack> getOffHand(Player player) {
        return new NonNullList<>(
            List.of(player.getItemBySlot(EquipmentSlot.OFFHAND)),
            ItemStack.EMPTY
        ) {
            @Override
            public ItemStack get(int index) {
                return index == 0 ? player.getItemBySlot(EquipmentSlot.OFFHAND) : ItemStack.EMPTY;
            }

            @Override
            public ItemStack set(int index, ItemStack stack) {
                if (index == 0) {
                    player.setItemSlot(EquipmentSlot.OFFHAND, stack);
                }
                return stack;
            }
        };
    }

    public static int getSelected(Player player) {
        return player.getInventory().getSelectedSlot();
    }
}
