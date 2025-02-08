package dev.anvilcraft.rg.sd.mixin;

import dev.anvilcraft.rg.sd.util.ClientMenuTick;
import dev.anvilcraft.rg.sd.util.FakePlayerInventoryMenu;
import dev.anvilcraft.rg.sd.util.SlotIcon;
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
public class ChestMenuMixin implements ClientMenuTick {
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void quickMove(Player player, int i, CallbackInfoReturnable<ItemStack> cir) {
        if (this.siliconeDolls$isFakePlayerMenu()) {
            cir.setReturnValue(FakePlayerInventoryMenu.quickMove((ChestMenu) (Object) this, i));
        }
    }

    @Override
    public void siliconeDolls$tick() {
        ChestMenu menu = (ChestMenu) (Object) this;
        if (this.siliconeDolls$isFakePlayerMenu()) {
            ((SlotIcon) menu.getSlot(1)).siliconeDolls$setIcon(InventoryMenu.EMPTY_ARMOR_SLOT_HELMET);
            ((SlotIcon) menu.getSlot(2)).siliconeDolls$setIcon(InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE);
            ((SlotIcon) menu.getSlot(3)).siliconeDolls$setIcon(InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS);
            ((SlotIcon) menu.getSlot(4)).siliconeDolls$setIcon(InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS);
            ((SlotIcon) menu.getSlot(7)).siliconeDolls$setIcon(InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
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
