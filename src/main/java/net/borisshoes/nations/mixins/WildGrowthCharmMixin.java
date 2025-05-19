package net.borisshoes.nations.mixins;

import net.borisshoes.arcananovum.ArcanaRegistry;
import net.borisshoes.arcananovum.items.charms.WildGrowthCharm;
import net.borisshoes.nations.land.NationsLand;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WildGrowthCharm.WildGrowthCharmItem.class)
public class WildGrowthCharmMixin {
   
   @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
   private void nations_stopWildgrowthCharm1(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir){
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
   
   @Inject(method = "inventoryTick", at = @At("HEAD"), cancellable = true)
   private void nations_stopWildgrowthCharm2(ItemStack stack, World world, Entity entity, int slot, boolean selected, CallbackInfo ci){
      if(!(entity instanceof ServerPlayerEntity player)) return;
      ChunkPos chunkPos = new ChunkPos(entity.getBlockPos());
      for(int i = -1; i <= 1; i++){
         for(int j = -1; j <= 1; j++){
            ChunkPos chunkPos1 = new ChunkPos(chunkPos.x+i, chunkPos.z+i);
            BlockPos newPos = chunkPos1.getBlockPos(0,0,0);
            if(!NationsLand.canBreakBlock(world,player,newPos,false) || !NationsLand.canUseItemOnBlock(world,player,newPos,stack,false)){
               ci.cancel();
               return;
            }
         }
      }
   }
}
