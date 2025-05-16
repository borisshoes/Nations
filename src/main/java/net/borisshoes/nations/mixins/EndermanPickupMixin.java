package net.borisshoes.nations.mixins;

import net.borisshoes.nations.land.InteractionEvents;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.entity.mob.EndermanEntity$PickUpBlockGoal")
public abstract class EndermanPickupMixin {
   @Final
   @Shadow
   private EndermanEntity enderman;
   
   @ModifyVariable(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"), ordinal = 0)
   private BlockPos pickupCheck(BlockPos pos) {
      if (!InteractionEvents.canEndermanInteract(this.enderman, pos)) {
         return new BlockPos(BlockPos.ZERO);
      }
      return pos;
   }
}
