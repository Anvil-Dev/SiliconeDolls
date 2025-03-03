package dev.anvilcraft.rg.sd.mixin.client;

import dev.anvilcraft.rg.sd.util.IClientMenuTickInjector;
import dev.anvilcraft.rg.sd.tool.client.PlayerInventoryMenu;
import dev.anvilcraft.rg.sd.util.ISlotIconInjector;
import dev.anvilcraft.rg.tools.chest.menu.control.Button;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestMenu.class)
abstract class ChestMenuMixin implements IClientMenuTickInjector {
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void quickMove(Player player, int i, CallbackInfoReturnable<ItemStack> cir) {
        if (this.siliconeDolls$isFakePlayerMenu()) {
            cir.setReturnValue(PlayerInventoryMenu.quickMove((ChestMenu) (Object) this, i));
        }
    }

    @Override
    public void siliconeDolls$tick() {
        ChestMenu menu = (ChestMenu) (Object) this;
        if (this.siliconeDolls$isFakePlayerMenu()) {
            ((ISlotIconInjector) menu.getSlot(1)).siliconeDolls$setIcon(InventoryMenu.EMPTY_ARMOR_SLOT_HELMET);
            ((ISlotIconInjector) menu.getSlot(2)).siliconeDolls$setIcon(InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE);
            ((ISlotIconInjector) menu.getSlot(3)).siliconeDolls$setIcon(InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS);
            ((ISlotIconInjector) menu.getSlot(4)).siliconeDolls$setIcon(InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS);
            ((ISlotIconInjector) menu.getSlot(7)).siliconeDolls$setIcon(InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
        }
    }

    @Unique
    private boolean siliconeDolls$isFakePlayerMenu() {
        ItemStack itemStack = ((ChestMenu) (Object) this).getSlot(0).getItem();
        //#if MC>=12100
        if (itemStack.is(Items.STRUCTURE_VOID)) {
            CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
            return customData != null && customData.copyTag().get(Button.RG_CLEAR) != null;
        }
        return false;
    }
}
