package net.borisshoes.nations.mixins;

import net.borisshoes.arcananovum.items.TelescopingBeacon;
import net.borisshoes.nations.land.NationsLand;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TelescopingBeacon.TelescopingBeaconItem.class)
public class TelescopingBeaconMixin {
   
   @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
   private void nations_stopTelescopingBeacon(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir){
      ChunkPos chunkPos = new ChunkPos(context.getBlockPos());
      if(!(context.getPlayer() instanceof ServerPlayerEntity player)) return;
      for(int i = -1; i <= 1; i++){
         for(int j = -1; j <= 1; j++){
            ChunkPos chunkPos1 = new ChunkPos(chunkPos.x+i, chunkPos.z+i);
            if(!NationsLand.canBreakBlock(context.getWorld(),player,chunkPos1.getBlockPos(0,0,0),true) || !NationsLand.canUseItemOnBlock(context.getWorld(),player,chunkPos1.getBlockPos(0,0,0),context.getStack(),true)){
               cir.setReturnValue(ActionResult.PASS);
            }
         }
      }
   }
}
