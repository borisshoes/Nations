package net.borisshoes.nations.gui;

import net.borisshoes.nations.NationsRegistry;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class GrowthCoinSlot extends Slot {
   public GrowthCoinSlot(Inventory inventory, int index, int x, int y){
      super(inventory, index, x, y);
   }
   
   @Override
   public boolean canInsert(ItemStack stack){
      return super.canInsert(stack) && stack.isOf(NationsRegistry.GROWTH_COIN_ITEM);
   }
}
