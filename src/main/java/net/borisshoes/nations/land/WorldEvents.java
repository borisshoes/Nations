package net.borisshoes.nations.land;

import net.borisshoes.nations.Nations;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.List;
import java.util.function.Consumer;

public class WorldEvents {
   
   // TODO tick boat perms in claimed chunks & enter/exit msgs
   
   public static void modifyExplosion(List<BlockPos> list, ServerWorld world) {
      list.removeIf(pos -> !NationsLand.canExplodeBlocks(world, pos));
   }
   
   public static boolean pistonCanPush(BlockState state, World world, BlockPos blockPos, Direction direction, Direction pistonDir) {
      if (world.isClient)
         return true;
      BlockPos dirPos = blockPos.offset(direction);
      BlockPos oppPos = blockPos.offset(direction.getOpposite());
      
      boolean flag = NationsLand.canPistonPush(world,blockPos,dirPos,oppPos);
      if (!flag) {
         //Idk enough about piston behaviour to update more blocks when slime is involved.
         //Ghost blocks appear when trying to push slime contraptions across border
         world.updateListeners(blockPos, state, state, 20);
         BlockState toState = world.getBlockState(dirPos);
         world.updateListeners(dirPos, toState, toState, 20);
      }
      return flag;
   }
   
   public static boolean canFlow(BlockState fluidBlockState, BlockView world, BlockPos blockPos, Direction direction) {
      if (!(world instanceof ServerWorld serverWorld) || direction == Direction.UP || direction == Direction.DOWN)
         return true;
      return NationsLand.canFlow(serverWorld,blockPos,blockPos.offset(direction));
   }
   
   public static boolean canFireSpread(ServerWorld world, BlockPos pos) {
      return NationsLand.canFireSpread(world,pos);
   }
}
