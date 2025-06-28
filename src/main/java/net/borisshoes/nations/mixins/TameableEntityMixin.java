package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.gameplay.Nation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(TameableEntity.class)
public class TameableEntityMixin {
   
   @ModifyReturnValue(method = "canTarget",at = @At("RETURN"))
   private boolean nations_tamedTargetCancel(boolean original, LivingEntity target){
      if(!original) return false;
      TameableEntity entity = (TameableEntity) (Object) this;
      LivingEntity owner = entity.getOwner();
      Nation ownNation = null;
      if(owner instanceof ServerPlayerEntity player){
         ownNation = Nations.getNation(player);
      }
      Nation targetNation = null;
      if(target instanceof ServerPlayerEntity player){
         targetNation = Nations.getNation(player);
      }else if(target instanceof Tameable tameable && tameable.getOwner() instanceof ServerPlayerEntity player){
         targetNation = Nations.getNation(player);
      }
      if(targetNation != null && targetNation.equals(ownNation)){
         return false;
      }
      return original;
   }
   
   @ModifyReturnValue(method = "canAttackWithOwner",at = @At("RETURN"))
   private boolean nations_tamedTargetWithOwnerCancel(boolean original, LivingEntity target, LivingEntity owner){
      if(!original) return false;
      Nation ownNation = null;
      if(owner instanceof ServerPlayerEntity player){
         ownNation = Nations.getNation(player);
      }
      Nation targetNation = null;
      if(target instanceof ServerPlayerEntity player){
         targetNation = Nations.getNation(player);
      }else if(target instanceof Tameable tameable && tameable.getOwner() instanceof ServerPlayerEntity player){
         targetNation = Nations.getNation(player);
      }
      if(targetNation != null && targetNation.equals(ownNation)){
         return false;
      }
      return original;
   }
}
