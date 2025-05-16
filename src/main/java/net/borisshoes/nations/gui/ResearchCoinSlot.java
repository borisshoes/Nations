package net.borisshoes.nations.gui;

import net.borisshoes.nations.NationsRegistry;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class ResearchCoinSlot extends Slot {
   public ResearchCoinSlot(Inventory inventory, int index, int x, int y){
      super(inventory, index, x, y);
   }
   
   @Override
   public boolean canInsert(ItemStack stack){
      return super.canInsert(stack) && stack.isOf(NationsRegistry.RESEARCH_COIN_ITEM);
   }
}
