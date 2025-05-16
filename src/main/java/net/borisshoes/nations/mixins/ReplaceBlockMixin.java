package net.borisshoes.nations.mixins;

import net.borisshoes.nations.land.InteractionEvents;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.effect.entity.ReplaceBlockEnchantmentEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReplaceBlockEnchantmentEffect.class)
public abstract class ReplaceBlockMixin {
   
   @Inject(method = "apply", at = @At(value = "HEAD"), cancellable = true)
   private void onApply(ServerWorld world, int level, EnchantmentEffectContext context, Entity entity, Vec3d pos, CallbackInfo ci) {
      ReplaceBlockEnchantmentEffect effect = ((ReplaceBlockEnchantmentEffect) (Object) this);
      if (entity instanceof LivingEntity living && !InteractionEvents.canFrostwalkerFreeze(world, BlockPos.ofFloored(pos).add(effect.offset()), living))
         ci.cancel();
   }
   
}
