package net.borisshoes.nations.mixins;

import net.borisshoes.arcananovum.items.ChestTranslocator;
import net.borisshoes.nations.land.NationsLand;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestTranslocator.ChestTranslocatorItem.class)
public class ChestTranslocatorMixin {
   @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
   private void nations_stopChestTranslocator(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir){
      if(!(context.getPlayer() instanceof ServerPlayerEntity player)) return;
      if(!NationsLand.canBreakBlock(context.getWorld(),player,context.getBlockPos(),true) || !NationsLand.canUseItemOnBlock(context.getWorld(),player,context.getBlockPos(),context.getStack(),true)){
         cir.setReturnValue(ActionResult.PASS);
      }
   }
}
