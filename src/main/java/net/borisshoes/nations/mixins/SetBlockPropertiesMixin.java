package net.borisshoes.nations.mixins;

import net.borisshoes.nations.land.InteractionEvents;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.effect.entity.SetBlockPropertiesEnchantmentEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SetBlockPropertiesEnchantmentEffect.class)
public class SetBlockPropertiesMixin {
   
   @Inject(method = "apply", at = @At(value = "HEAD"), cancellable = true)
   private void freeze(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos, CallbackInfo ci) {
      SetBlockPropertiesEnchantmentEffect effect = ((SetBlockPropertiesEnchantmentEffect) (Object) this);
      if (user instanceof LivingEntity living && !InteractionEvents.canFrostwalkerFreeze(world, BlockPos.ofFloored(pos).add(effect.offset()), living))
         ci.cancel();
   }
   
}
