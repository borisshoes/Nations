package net.borisshoes.nations.mixins;

import net.borisshoes.nations.land.NationsLand;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ObserverBlock.class, ComparatorBlock.class, DispenserBlock.class})
public class RedstoneComponentsMixin {
   
   @Inject(method = "scheduledTick", at = @At("HEAD"), cancellable = true)
   private void nations_stopObserver(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci){
      if(!NationsLand.redstoneEnabled(world.getRegistryKey(),new ChunkPos(pos))){
         ci.cancel();
      }
   }
}
