package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.nations.NationsRegistry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingBobberEntity.class)
public class FishingBobberEntityMixin {
   
   @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemEntity;setVelocity(DDD)V"))
   private void nations_editFishingBow(ItemStack usedItem, CallbackInfoReturnable<Integer> cir, @Local ItemEntity itemEntity){
      if(itemEntity.getStack().isOf(Items.BOW)){
         ItemStack bowStack = new ItemStack(Items.BOW);
         bowStack.set(DataComponentTypes.DAMAGE, itemEntity.getRandom().nextBetween(0, bowStack.getMaxDamage()));
         itemEntity.setStack(bowStack);
      }else if(itemEntity.getStack().isOf(Items.ENCHANTED_BOOK)){
         ItemStack coinStack = new ItemStack(NationsRegistry.RESEARCH_COIN_ITEM,itemEntity.getRandom().nextBetween(5, 25));
         itemEntity.setStack(coinStack);
      }
   }
}
