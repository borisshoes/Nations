package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

@Mixin(TeleportTarget.class)
public class TeleportTargetMixin {
   
   @ModifyReturnValue(method = "missingSpawnBlock", at = @At("RETURN"))
   private static TeleportTarget nations_getNewRespawnLocation(TeleportTarget original, ServerWorld world, Entity entity, TeleportTarget.PostDimensionTransition postDimensionTransition){
      if(entity instanceof ServerPlayerEntity player){
         INationsProfileComponent data = Nations.getPlayer(player);
         if(data.getNation() != null && data.getNation().isFounded()){
            return new TeleportTarget(world, data.getNation().getHologramPos(), Vec3d.ZERO, 0.0F, 0.0F, true, false, Set.of(), postDimensionTransition);
         }
      }
      return original;
   }
}
