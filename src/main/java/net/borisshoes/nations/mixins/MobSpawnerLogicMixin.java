package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.nations.Nations;
import net.minecraft.block.spawner.MobSpawnerLogic;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobSpawnerLogic.class)
public class MobSpawnerLogicMixin {
   
   @ModifyReturnValue(method = "isPlayerInRange", at = @At(value = "RETURN"))
   private boolean nations_isPlayerInRange(boolean original, World world, BlockPos pos){
      if(original) return true;
      
      if(world.getRegistryKey().equals(ServerWorld.OVERWORLD)){
         ChunkPos chunkPos = new ChunkPos(pos);
         return Nations.isChunkAnchored(chunkPos);
      }
      return false;
   }
}
