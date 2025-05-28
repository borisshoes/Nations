package net.borisshoes.nations.utils;

import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.CapturePoint;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.NationChunk;
import net.borisshoes.nations.land.NationsLand;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Pair;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;

public class NationsUtils {
   
   public static int calculateClaimOrInfluenceCost(ChunkPos chunk, Nation nation, boolean claim){
      int baseCost = claim ? NationsConfig.getInt(NationsRegistry.CLAIM_COIN_COST_CFG) : NationsConfig.getInt(NationsRegistry.INFLUENCE_COIN_COST_CFG);
      double chunkDeduction = 0;
      double chunkAppreciation = nation.getChunks().size();
      
      for(int i = -2; i <= 2; i++){
         for(int j = -2; j <= 2; j++){
            NationChunk nChunk = Nations.getChunk(chunk.x+i,chunk.z+j);
            if((i==0 && j==0) || nChunk == null || !nation.equals(nChunk.getControllingNation())) continue;
            if(nChunk.isClaimed()){
               chunkDeduction += 2;
            }else if(nChunk.isInfluenced()){
               chunkDeduction += 1;
            }
         }
      }
      chunkAppreciation *= NationsConfig.getDouble(NationsRegistry.TERRITORY_CHUNK_APPRECIATION_CFG);
      chunkDeduction *= NationsConfig.getDouble(NationsRegistry.TERRITORY_CHUNK_DEDUCTION_CFG);
      double finalCost = baseCost * (1 + chunkAppreciation) * (1 - chunkDeduction);
      return (int) Math.max(0,finalCost);
   }
   
   public static Triple<Integer,Integer,Integer> calculateChunkCoinGeneration(ServerWorld serverWorld, ChunkPos chunkPos){
      Heightmap heightmap = serverWorld.getChunk(chunkPos.x,chunkPos.z).getHeightmap(Heightmap.Type.WORLD_SURFACE);
      int growth = 0, material = 0, research = 0;
      for(int x = 0; x < 16; x++){
         for(int z = 0; z < 16; z++){
            int y = heightmap.get(x,z);
            BlockPos blockPos = new BlockPos(chunkPos.x*16 + x, y, chunkPos.z*16 + z);
            RegistryEntry<Biome> biome = serverWorld.getBiome(blockPos);
            RegistryKey<Biome> biomeKey = serverWorld.getRegistryManager().getOrThrow(RegistryKeys.BIOME).getKey(biome.value()).orElse(BiomeKeys.THE_VOID);
            int[] values = NationsConfig.getBiomeConfigValue(biomeKey);
            //System.out.println("Checking block: "+blockPos.toShortString()+" found biome: "+biomeKey.getValue()+" with values: "+values.getLeft()+" "+values.getMiddle()+" "+values.getRight());
            growth += values[0];
            material += values[1];
            research += values[2];
         }
      }
      return new ImmutableTriple<>(growth,material,research);
   }
   
   public static Triple<Integer,Integer,Integer> calculateChunkCoinGeneration(ServerWorld serverWorld, Vec3d centerPos, int radius){
      ChunkPos centerChunk = new ChunkPos(ChunkSectionPos.getSectionCoordFloored(centerPos.getX()),ChunkSectionPos.getSectionCoordFloored(centerPos.getZ()));
      int growth = 0, material = 0, research = 0;
      final double scaleFactor = 0.01;
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            if (Math.abs(dx) + Math.abs(dz) <= radius) {
               ChunkPos chunkPos = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
               if(NationsLand.isOutOfBounds(serverWorld.getRegistryKey(),chunkPos)) continue;
               Triple<Integer,Integer,Integer> values = calculateChunkCoinGeneration(serverWorld, chunkPos);
               growth += values.getLeft();
               material += values.getMiddle();
               research += values.getRight();
            }
         }
      }
      growth = (int)(growth*scaleFactor);
      material = (int)(material*scaleFactor);
      research = (int)(research*scaleFactor);
      return new ImmutableTriple<>(growth,material,research);
   }
   
   public static Pair<Triple<Integer,Integer,Integer>,Triple<Integer,Integer,Integer>> calculateCapturePointYields(ServerWorld serverWorld, Vec3d centerPos, int radius){
      ChunkPos centerChunk = new ChunkPos(ChunkSectionPos.getSectionCoordFloored(centerPos.getX()),ChunkSectionPos.getSectionCoordFloored(centerPos.getZ()));
      int growth = 0, material = 0, research = 0;
      int growthC = 0, materialC = 0, researchC = 0;
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            if (Math.abs(dx) + Math.abs(dz) <= radius) {
               ChunkPos chunkPos = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
               if(NationsLand.isOutOfBounds(serverWorld.getRegistryKey(),chunkPos)) continue;
               CapturePoint cap = Nations.getCapturePoint(chunkPos);
               if(cap == null) continue;
               switch(cap.getType()){
                  case GROWTH -> {
                     growth += cap.getRawYield();
                     growthC++;
                  }
                  case MATERIAL -> {
                     material += cap.getRawYield();
                     materialC++;
                  }
                  case RESEARCH -> {
                     research += cap.getRawYield();
                     researchC++;
                  }
               }
            }
         }
      }
      return new Pair<>(new ImmutableTriple<>(growthC,materialC,researchC),new ImmutableTriple<>(growth,material,research));
   }
   
   
   public static Set<ChunkPos> generatePoissonPoints(double minX, double maxX, double minZ, double maxZ, long seed, int minDist) {
      Random rand = Random.create(seed);
      Set<ChunkPos> points = new HashSet<>();
      List<ChunkPos> active = new ArrayList<>();
      
      // Start with a random initial point inside the region.
      double initialX = minX + rand.nextDouble() * (maxX - minX);
      double initialZ = minZ + rand.nextDouble() * (maxZ - minZ);
      ChunkPos initialPoint = new ChunkPos((int) initialX, (int) initialZ);
      points.add(initialPoint);
      active.add(initialPoint);
      
      // Number of candidates to try before removing an active point.
      int k = 30;
      
      while (!active.isEmpty()) {
         // Select a random point from the active list.
         int index = rand.nextInt(active.size());
         ChunkPos p = active.get(index);
         boolean foundCandidate = false;
         
         // Try to generate up to k candidate points around p.
         for (int i = 0; i < k; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            // The candidate radius is chosen uniformly between MIN_DIST and 2*MIN_DIST.
            double radius = minDist + rand.nextDouble() * minDist;
            double newX = p.x + radius * Math.cos(angle);
            double newZ = p.z + radius * Math.sin(angle);
            
            // Ensure the candidate is inside the region.
            if (newX >= minX && newX < maxX && newZ >= minZ && newZ < maxZ) {
               ChunkPos candidate = new ChunkPos((int) newX, (int) newZ);
               boolean valid = true;
               // Check that candidate is at least MIN_DIST away from all already accepted points.
               for (ChunkPos other : points) {
                  double dx = candidate.x - other.x;
                  double dz = candidate.z - other.z;
                  if (dx * dx + dz * dz < minDist * minDist) {
                     valid = false;
                     break;
                  }
               }
               if (valid) {
                  points.add(candidate);
                  active.add(candidate);
                  foundCandidate = true;
                  break; // Accept the candidate and stop trying for this active point.
               }
            }
         }
         // If no candidate was found, remove the point from the active list.
         if (!foundCandidate) {
            active.remove(index);
         }
      }
      return points;
   }
}
