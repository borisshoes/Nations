package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.arcananovum.entities.NulConstructEntity;
import net.borisshoes.nations.land.NationsLand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(NulConstructEntity.class)
public class NulConstructMixin {
   
   @Inject(method = "damageBlock", at = @At("HEAD"), cancellable = true)
   private void nations_stopConstruct(BlockPos pos, int damage, CallbackInfoReturnable<Boolean> cir){
      NulConstructEntity construct = (NulConstructEntity) (Object) this;
      if(!NationsLand.canExplodeBlocks(construct.getWorld(),pos)){
         cir.setReturnValue(false);
      }
   }
}
