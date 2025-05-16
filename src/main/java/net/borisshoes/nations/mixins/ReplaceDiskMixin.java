package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.nations.land.InteractionEvents;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.effect.entity.ReplaceDiskEnchantmentEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.blockpredicate.BlockPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(ReplaceDiskEnchantmentEffect.class)
public class ReplaceDiskMixin {
   
   @ModifyExpressionValue(method = "apply", at = @At(value = "FIELD", target = "Lnet/minecraft/enchantment/effect/entity/ReplaceDiskEnchantmentEffect;predicate:Ljava/util/Optional;"))
   private Optional<BlockPredicate> onApply(Optional<BlockPredicate> original, ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos1, @Local(ordinal = 1) BlockPos pos) {
      if (user instanceof LivingEntity living && !InteractionEvents.canFrostwalkerFreeze(world, pos, living))
         return Optional.of(BlockPredicate.not(BlockPredicate.alwaysTrue()));
      return original;
   }
}
