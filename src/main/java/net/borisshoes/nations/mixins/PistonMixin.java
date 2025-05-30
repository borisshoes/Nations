package net.borisshoes.nations.mixins;

import net.borisshoes.nations.land.WorldEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBlock.class)
public abstract class PistonMixin {

   @Inject(method = "isMovable", at = @At(value = "HEAD"), cancellable = true)
   private static void checkMovable(BlockState blockState, World world, BlockPos blockPos, Direction direction, boolean canBreak, Direction pistonDir, CallbackInfoReturnable<Boolean> cir) {
      if (!WorldEvents.pistonCanPush(blockState, world, blockPos, direction, pistonDir)) {
         cir.setReturnValue(false);
         cir.cancel();
      }
   }
}
