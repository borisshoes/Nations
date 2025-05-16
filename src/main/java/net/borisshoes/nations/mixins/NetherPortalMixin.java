package net.borisshoes.nations.mixins;

import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.dimension.NetherPortal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetherPortal.class)
public class NetherPortalMixin {
   
   @Inject(method = "createPortal", at = @At("HEAD"), cancellable = true)
   private void nations_stopPortalForming(WorldAccess world, CallbackInfo ci){
      if(NationsConfig.getBoolean(NationsRegistry.NETHER_PORTALS_DISABLED_CFG)) ci.cancel();
   }
}
