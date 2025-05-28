package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.land.InteractionEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
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
   private void nations_collision(World world, BlockPos pos, Entity entity, CallbackInfo ci) {
      if (InteractionEvents.cancelEntityBlockCollision(this.asBlockState(), world, pos, entity)) {
         ci.cancel();
      }
   }
   
   @ModifyReturnValue(method = "getHardness", at = @At("RETURN"))
   private float nations_contestBlockHardness(float original, BlockView world, BlockPos pos){
      if(world instanceof ServerWorld serverWorld && serverWorld.getRegistryKey().equals(NationsRegistry.CONTEST_DIM)){
         return Math.min(original,15);
      }
      return original;
   }
}
