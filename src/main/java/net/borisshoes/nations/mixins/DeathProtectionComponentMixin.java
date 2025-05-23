package net.borisshoes.nations.mixins;

import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.minecraft.component.type.DeathProtectionComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathProtectionComponent.class)
public class DeathProtectionComponentMixin {
   
   @Inject(method = "applyDeathEffects", at = @At("HEAD"))
   private void nations_cooldownDeathProtectors(ItemStack stack, LivingEntity entity, CallbackInfo ci){
      if(entity instanceof ServerPlayerEntity player){
         int duration = NationsConfig.getInt(NationsRegistry.DEATH_PROTECTOR_COOLDOWN_CFG) * 20;
         player.getItemCooldownManager().set(stack,duration);
      }
   }
}
