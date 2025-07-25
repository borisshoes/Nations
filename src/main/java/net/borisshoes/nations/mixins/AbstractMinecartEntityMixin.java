package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractMinecartEntity.class)
public class AbstractMinecartEntityMixin {
   
   @ModifyExpressionValue(method = "pushAwayFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;hasPassenger(Lnet/minecraft/entity/Entity;)Z"))
   private boolean nations_fixNestedMinecartGlitch(boolean original, Entity entity){
      if(original) return true;
      AbstractMinecartEntity cart = (AbstractMinecartEntity) (Object) this;
      for(Entity passenger : cart.getPassengersDeep()){
        if(entity.equals(passenger)) {
           return true;
        }
      }
      return original;
   }
}
