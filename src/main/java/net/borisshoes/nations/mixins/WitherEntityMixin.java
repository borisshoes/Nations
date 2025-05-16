package net.borisshoes.nations.mixins;

import net.borisshoes.nations.land.InteractionEvents;
import net.minecraft.entity.boss.WitherEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherEntity.class)
public class WitherEntityMixin {
   @Shadow
   private int blockBreakingCooldown;
   
   @Inject(method = "mobTick", at = @At(value = "HEAD"))
   private void preventClaimDmg(CallbackInfo info) {
      WitherEntity wither = (WitherEntity) (Object) this;
      if (this.blockBreakingCooldown > 0 && !InteractionEvents.witherCanDestroy(wither))
         this.blockBreakingCooldown = -1;
   }
}
