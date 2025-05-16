package net.borisshoes.nations.mixins;

import net.borisshoes.nations.land.InteractionEvents;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
public class BoneMealItemMixin {
   
   @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
   private void check(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
      if (InteractionEvents.growBonemeal(context)) {
         cir.setReturnValue(ActionResult.PASS);
         cir.cancel();
      }
   }
}
