package net.borisshoes.nations.mixins;

import net.borisshoes.nations.land.InteractionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class, ItemFrameEntity.class, VehicleEntity.class, ArmorStandEntity.class, EndCrystalEntity.class, ItemEntity.class})
public abstract class EntityDamageMixin {
   
   @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
   private void onDamage(ServerWorld serverLevel, DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> cir) {
      if (InteractionEvents.preventDamage((Entity) (Object) this, damageSource)) {
         cir.setReturnValue(false);
         cir.cancel();
      }
   }
}