package net.borisshoes.nations.mixins;

import net.borisshoes.arcananovum.blocks.forge.StarlightForgeBlockEntity;
import net.borisshoes.arcananovum.recipes.arcana.ForgeRequirement;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ForgeRequirement.class)
public class ArcanaForgeRequirementMixin {
   
   @Inject(method = "forgeMeetsRequirement", at = @At("HEAD"), cancellable = true)
   private void nations_disableForgeRequirement(StarlightForgeBlockEntity forge, boolean message, ServerPlayerEntity player, CallbackInfoReturnable<Boolean> cir){
      cir.setReturnValue(true);
   }
}
