package net.borisshoes.nations.mixins;

import net.borisshoes.nations.land.WorldEvents;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowableFluid.class)
public class FlowableFluidMixin {
   
   @Inject(method = "canFlowThrough(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)Z", at = @At(value = "HEAD"), cancellable = true)
   private void crossClaimFlow(BlockView world, BlockPos blockPos, BlockState blockState, Direction direction, BlockPos blockPos2, BlockState blockState2, FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
      if (!WorldEvents.canFlow(blockState2, world, blockPos, direction)) {
         cir.setReturnValue(false);
         cir.cancel();
      }
   }
}
