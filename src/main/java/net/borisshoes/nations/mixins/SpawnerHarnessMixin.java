package net.borisshoes.nations.mixins;

import net.borisshoes.arcananovum.items.SpawnerHarness;
import net.borisshoes.nations.land.NationsLand;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnerHarness.SpawnerHarnessItem.class)
public class SpawnerHarnessMixin {
   
   @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
   private void nations_stopSpawnerHarness(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir){
      if(!(context.getPlayer() instanceof ServerPlayerEntity player)) return;
      if(!NationsLand.canBreakBlock(context.getWorld(),player,context.getBlockPos(),true) || !NationsLand.canUseItemOnBlock(context.getWorld(),player,context.getBlockPos(),context.getStack(),true)){
         cir.setReturnValue(ActionResult.PASS);
      }
   }
}
