package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.gameplay.Nation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WolfEntity.class)
public class WolfEntityMixin {
   
   @ModifyExpressionValue(method = "canAttackWithOwner", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;shouldDamagePlayer(Lnet/minecraft/entity/player/PlayerEntity;)Z"))
   private boolean nations_stopWolfAttack(boolean original, LivingEntity target, LivingEntity owner){
      if(owner instanceof ServerPlayerEntity ownerPlayer){
         if(target instanceof ServerPlayerEntity targetPlayer){
            Nation targetNation = Nations.getNation(targetPlayer);
            Nation ownerNation = Nations.getNation(ownerPlayer);
            if(targetNation != null && targetNation.equals(ownerNation)){
               return false;
            }
         }else if(target instanceof Tameable tameable && tameable.getOwner() instanceof ServerPlayerEntity otherOwner){
            Nation targetNation = Nations.getNation(otherOwner);
            Nation ownerNation = Nations.getNation(ownerPlayer);
            if(targetNation != null && targetNation.equals(ownerNation)){
               return false;
            }
         }
      }
      return original;
   }
   
   @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/WolfEntity;tickAngerLogic(Lnet/minecraft/server/world/ServerWorld;Z)V"))
   private void nations_stopWolfAnger(CallbackInfo ci){
      WolfEntity wolf = (WolfEntity) (Object) this;
      if(wolf.isTamed() && wolf.getOwner() instanceof ServerPlayerEntity owner){
         ServerPlayerEntity target = null;
         if(wolf.getAngryAt() != null && ((ServerWorld)wolf.getWorld()).getEntity(wolf.getAngryAt()) instanceof ServerPlayerEntity t){
            target = t;
         }else if(wolf.getAttacking() instanceof ServerPlayerEntity t){
            target = t;
         }else if(wolf.getTarget() instanceof ServerPlayerEntity t){
            target = t;
         }else if(wolf.getAngryAt() != null && ((ServerWorld)wolf.getWorld()).getEntity(wolf.getAngryAt()) instanceof Tameable tameable && tameable.getOwner() instanceof ServerPlayerEntity t){
            target = t;
         }else if(wolf.getAttacking() instanceof Tameable tameable && tameable.getOwner() instanceof ServerPlayerEntity t){
            target = t;
         }else if(wolf.getTarget() instanceof Tameable tameable && tameable.getOwner() instanceof ServerPlayerEntity t){
            target = t;
         }else if(wolf.getAttacker() instanceof ServerPlayerEntity t){
            target = t;
         }
         if(target != null){
            Nation targetNation = Nations.getNation(target);
            Nation ownerNation = Nations.getNation(owner);
            if(targetNation != null && targetNation.equals(ownerNation)){
               wolf.setAttacking(null);
               wolf.stopAnger();
            }
         }
      }
   }
}
