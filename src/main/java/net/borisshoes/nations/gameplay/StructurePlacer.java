package net.borisshoes.nations.gameplay;

import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.utils.NationsColors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.StairShape;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

public class StructurePlacer {
   
   public record Structure(int[][][] statePattern, List<BlockState> blockStates){}
   
   public static Structure loadFromFile(String namespace, String id){
      try{
         Optional<Optional<Path>> pathOptional = FabricLoader.getInstance().getModContainer(namespace).map(container -> container.findPath("data/"+namespace+"/structures/"+id+".nbt"));
         if(pathOptional.isEmpty() || pathOptional.get().isEmpty()){
            return null;
         }
         Path path = pathOptional.get().get();
         InputStream in = Files.newInputStream(path);
         NbtCompound compound = NbtIo.readCompressed(in, NbtSizeTracker.ofUnlimitedBytes());
         if(compound == null) return null;
         
         NbtList size = compound.getList("size", NbtElement.INT_TYPE);
         int sizeX = size.getInt(0);
         int sizeY = size.getInt(1);
         int sizeZ = size.getInt(2);
         
         int[][][] pattern = new int[sizeX][sizeY][sizeZ];
         for(int i=0;i<sizeX;i++){for(int j=0;j<sizeY;j++){for(int k=0;k<sizeZ;k++){pattern[i][j][k] = -1;}}} // Set all elements to -1 because 0 is used by the palette
         
         NbtList blocks = compound.getList("blocks",NbtElement.COMPOUND_TYPE);
         for(NbtElement b : blocks){
            NbtCompound block = (NbtCompound) b;
            NbtList pos = block.getList("pos", NbtElement.INT_TYPE);
            pattern[pos.getInt(0)][pos.getInt(1)][pos.getInt(2)] = block.getInt("state");
         }
         
         // Build predicates for checking block states
         NbtList palette = compound.getList("palette",NbtElement.COMPOUND_TYPE);
         List<BlockState> states = new ArrayList<>();
         for(NbtElement e : palette){
            // Get the actual block
            NbtCompound blockTag = (NbtCompound) e;
            states.add(NbtHelper.toBlockState(Registries.BLOCK,blockTag)); // Add state
         }
         
         return new Structure(pattern,states);
      }catch(Exception e){
         e.printStackTrace();
      }
      return null;
   }
   
   public static Structure copyStructureWithColor(Structure structure, DyeColor color){
      int[][][] pattern = structure.statePattern();
      List<BlockState> states = structure.blockStates();
      List<BlockState> newStates = states.stream().map(state -> NationsColors.redyeBlock(state,color)).toList();
      return new Structure(pattern,newStates);
   }
   
   public static void placeStructure(Structure structure, ServerWorld world, BlockPos origin){
      int[][][] pattern = structure.statePattern();
      List<BlockState> states = structure.blockStates();
      
      for(int i=0;i<pattern.length;i++){
         for(int j=0;j<pattern[0].length;j++){
            for(int k=0;k<pattern[0][0].length;k++){
               int patternInt = pattern[i][j][k];
               if(patternInt == -1) continue;
               BlockState templateState = states.get(patternInt);
               BlockPos pos = origin.add(i,j,k);
               BlockState curState = world.getBlockState(pos);
               
               if(curState.isOf(Blocks.WATER)){
                  if(templateState.isAir()){
                     templateState = curState;
                  }else if(templateState.contains(Properties.WATERLOGGED)){
                     templateState = templateState.with(Properties.WATERLOGGED,true);
                  }
               }
               
               world.setBlockState(pos,templateState);
            }
         }
      }
   }
}
