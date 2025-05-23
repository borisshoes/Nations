package net.borisshoes.nations.mixins;

import net.borisshoes.nations.land.NationsLand;
import net.minecraft.block.BlockState;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBlock.class)
public class PistonBlockMixin {
   
   @Inject(method = "onSyncedBlockEvent", at = @At("HEAD"), cancellable = true)
   private void nations_stopPiston(BlockState state, World world, BlockPos pos, int type, int data, CallbackInfoReturnable<Boolean> cir){
//      if(!NationsLand.redstoneEnabled(world.getRegistryKey(),new ChunkPos(pos))){
//         cir.setReturnValue(false);
//         cir.cancel();
//      }
   }
}
