package net.borisshoes.nations.mixins;

import net.borisshoes.nations.land.InteractionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.entity.mob.EndermanEntity$PlaceBlockGoal")
public class EndermanPlaceMixin {
   @Final
   @Shadow
   private EndermanEntity enderman;
   
   @Inject(method = "canPlaceOn", at = @At(value = "HEAD"), cancellable = true)
   private void placeCheck(World world, BlockPos posAbove, BlockState carriedState, BlockState stateAbove, BlockState state, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      if (!InteractionEvents.canEndermanInteract(this.enderman, pos)) {
         cir.setReturnValue(false);
         cir.cancel();
      }
   }
}
