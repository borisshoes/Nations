package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.borisshoes.arcananovum.items.ContainmentCirclet;
import net.borisshoes.nations.land.NationsLand;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ContainmentCirclet.class)
public class ContainmentCircletMixin {
   
   @ModifyExpressionValue(method = "useOnEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;isIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
   private boolean nations_stopContainmentCirclet(boolean original, PlayerEntity user, LivingEntity entity, Hand hand){
      if(original) return true;
      if(!(user instanceof ServerPlayerEntity player)) return false;
      if(!NationsLand.canDamage(player,entity,true)){
         return true;
      }
      return false;
   }
}
