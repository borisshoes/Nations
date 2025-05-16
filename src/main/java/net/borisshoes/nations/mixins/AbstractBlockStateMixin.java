package net.borisshoes.nations.mixins;

import net.borisshoes.nations.land.InteractionEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {
   
   @Shadow
   protected abstract BlockState asBlockState();
   
   @Inject(method = "onEntityCollision", at = @At(value = "HEAD"), cancellable = true)
   private void collision(World world, BlockPos pos, Entity entity, CallbackInfo ci) {
      if (InteractionEvents.cancelEntityBlockCollision(this.asBlockState(), world, pos, entity)) {
         ci.cancel();
      }
   }
}
