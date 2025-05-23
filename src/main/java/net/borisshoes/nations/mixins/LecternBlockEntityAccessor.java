package net.borisshoes.nations.mixins;

import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.PropertyDelegate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LecternBlockEntity.class)
public interface LecternBlockEntityAccessor {
   @Accessor("inventory")
   Inventory getInv();
   
   @Accessor("propertyDelegate")
   PropertyDelegate getProp();
}
