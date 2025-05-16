package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.borisshoes.nations.land.NationsLand;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.PortalForcer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.stream.Stream;

@Mixin(PortalForcer.class)
public class PortalForcerMixin {
   
   @Final
   @Shadow
   private ServerWorld world;
   
   @ModifyExpressionValue(method = "getPortalPos", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;"))
   private Stream<BlockPos> nations_portalBoundaryCheck(Stream<BlockPos> original, BlockPos pos, boolean destIsNether, WorldBorder worldBorder){
      RegistryKey<World> world = destIsNether ? World.NETHER : World.OVERWORLD;
      return original.filter(blockPos -> !NationsLand.isOutOfBounds(world, blockPos));
   }
   
   @ModifyExpressionValue(method = "createPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;contains(Lnet/minecraft/util/math/BlockPos;)Z", ordinal = 0))
   private boolean nations_portalPlacementCheck1(boolean original, @Local(ordinal = 1) BlockPos.Mutable mutable, @Local Direction direction){
      if(!original) return false;
      return !NationsLand.isOutOfBounds(world.getRegistryKey(), new ChunkPos(mutable)) && !NationsLand.isOutOfBounds(world.getRegistryKey(), new ChunkPos(mutable.move(direction, 1)));
   }
   
   @ModifyExpressionValue(method = "createPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;clampFloored(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/BlockPos;"))
   private BlockPos nations_shiftPortalPlacement(BlockPos original){
      return NationsLand.moveInBounds(world.getRegistryKey(),original);
   }
}
