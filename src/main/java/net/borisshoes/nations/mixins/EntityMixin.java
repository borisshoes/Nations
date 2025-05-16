package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.land.InteractionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.block.Portal;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
   
   @Inject(method = "fall", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onLandedUpon(Lnet/minecraft/world/World;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;F)V"), cancellable = true)
   private void fallOnBlock(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition, CallbackInfo info) {
      if (InteractionEvents.preventFallOn((Entity) (Object) this, heightDifference, onGround, landedState, landedPosition))
         info.cancel();
   }

   @Inject(method = "tryUsePortal", at = @At("HEAD"), cancellable = true)
   private void nations_stopNetherPortals(Portal portal, BlockPos pos, CallbackInfo ci){
      boolean disabled = NationsConfig.getBoolean(NationsRegistry.NETHER_PORTALS_DISABLED_CFG);
      if(disabled && portal instanceof NetherPortalBlock) ci.cancel();
   }
}
