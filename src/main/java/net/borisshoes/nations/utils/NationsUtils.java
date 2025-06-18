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
import org.joml.Vector2d;
import org.joml.Vector2i;

import java.util.*;
import java.util.stream.Collectors;

public class NationsUtils {
   
   public static double calculateCompactnessModifier(double pp, double re){
      double mid = NationsConfig.getDouble(NationsRegistry.TERRITORY_COMPACTNESS_COST_MIDPOINT_CFG);
      double maxInc = NationsConfig.getDouble(NationsRegistry.TERRITORY_COMPACTNESS_COST_MAX_INCREASE_CFG);
      double maxRed = NationsConfig.getDouble(NationsRegistry.TERRITORY_COMPACTNESS_COST_MAX_REDUCTION_CFG);
      double thresh = NationsConfig.getDouble(NationsRegistry.TERRITORY_COMPACTNESS_MINIMUM_CFG);
      double combinedCompactness = Math.sqrt(pp*re);
      double p3 = MiscUtils.calculateThirdTerritoryScaleParameter(thresh,maxInc,mid,1.0,1.0,maxRed);
      double p1 = (maxInc-1) / (Math.exp(p3*thresh) - Math.exp(p3*mid));
      double p2 = maxInc - p1 * Math.exp(p3*thresh);
      double modifier = p2 + p1 * Math.exp(p3*combinedCompactness);
      return modifier;
   }
   
   public static int calculateClaimOrInfluenceCost(ChunkPos chunk, Nation nation, boolean claim){
      if(claim){
         return NationsConfig.getInt(NationsRegistry.CLAIM_COIN_COST_CFG);
      }
      int baseCost = NationsConfig.getInt(NationsRegistry.INFLUENCE_COIN_COST_CFG);
      double modifier = NationsConfig.getDouble(NationsRegistry.TERRITORY_COST_MODIFIER_CFG);
      int manifestLvl = nation.getBuffLevel(NationsRegistry.MANIFEST_DESTINY);
      double manifestReduction = NationsConfig.getDouble(NationsRegistry.MANIFEST_DESTINY_REDUCTION_CFG);
      
      Set<NationChunk> chunks = nation.getChunks();
      List<Vector2i> chunkCoords = chunks.stream().map(c -> new Vector2i(c.getPos().x,c.getPos().z)).collect(Collectors.toCollection(ArrayList::new));
      
      Vector2d centroid = MiscUtils.calculateCentroid(chunkCoords);
      double distance = centroid.distance(chunk.x,chunk.z);
      double pp = MiscUtils.calculatePolsbyPopper(chunkCoords);
      double re = MiscUtils.calculateReock(chunkCoords);
      double compactnessMod = calculateCompactnessModifier(pp,re);
      double finalCost = baseCost * compactnessMod * modifier * distance * (1 - manifestReduction*manifestLvl);

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
      double growth = 0, material = 0, research = 0;
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            if (Math.abs(dx) + Math.abs(dz) <= radius) {
               ChunkPos chunkPos = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
               if(NationsLand.isOutOfBounds(serverWorld.getRegistryKey(),chunkPos) || Nations.getChunk(chunkPos) == null) continue;
               Triple<Double,Double,Double> values = Nations.getChunk(chunkPos).getYield();
               growth += values.getLeft();
               material += values.getMiddle();
               research += values.getRight();
            }
         }
      }
      return new ImmutableTriple<>((int)growth,(int)material,(int)research);
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
