package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import net.borisshoes.nations.NationsRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PolymerBlockUtils.class)
public class PolymerBlockUtilsMixin {
   
   @ModifyReturnValue(method = "shouldMineServerSide", at = @At("RETURN"))
   private static boolean nations_overrideServerMining(boolean original, ServerPlayerEntity player, BlockPos pos, BlockState state){
      if(!original){
         if(player.getServerWorld().getRegistryKey().equals(NationsRegistry.CONTEST_DIM)){
            return true;
         }
      }
      return original;
   }
}