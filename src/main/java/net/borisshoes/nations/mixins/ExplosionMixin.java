package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.nations.land.WorldEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(ExplosionImpl.class)
public class ExplosionMixin {
   
   @Inject(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/explosion/ExplosionImpl;damageEntities()V", shift = At.Shift.AFTER))
   private void explosionHook(CallbackInfo ci, @Local List<BlockPos> list) {
      ExplosionImpl explosion = (ExplosionImpl)(Object)this;
      if(explosion.getWorld() != null){
         WorldEvents.modifyExplosion(list, explosion.getWorld());
      }
   }
}
