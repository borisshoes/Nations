package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.item.BlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(BlockItem.class)
public class BlockItemMixin {
   
   @ModifyExpressionValue(method = "writeNbtToBlockEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BlockEntityType;canPotentiallyExecuteCommands()Z"))
   private static boolean nations_fixSpawnerSurvivalPlace(boolean original){
      return false;
   }
}
