package net.borisshoes.nations.cca;

import net.borisshoes.nations.gameplay.CapturePoint;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.NationChunk;
import net.borisshoes.nations.gameplay.WarManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.ChunkPos;

import java.util.*;
import java.util.stream.Collectors;

public class NationsDataComponent implements INationsDataComponent {
   
   private boolean worldInitialized;
   private long nextWar = Long.MAX_VALUE;
   private long nextRift = Long.MAX_VALUE;
   private long lastDayTick = System.currentTimeMillis();
   private long lastHourTick = System.currentTimeMillis();
   private long lastTimeCheck = System.currentTimeMillis();
   
   private NbtCompound riftData = new NbtCompound();
   
   private final HashMap<String, Nation> nations = new HashMap<>();
   private final Set<CapturePoint> capturePoints = new HashSet<>();
   private final HashMap<ChunkPos, NationChunk> nationChunks = new HashMap<>();
   
   @Override
   public boolean isWorldInitialized(){
      return worldInitialized;
   }
   
   @Override
   public void initializeWorld(List<CapturePoint> caps, List<NationChunk> chunks){
      nations.clear();
      capturePoints.clear();
      nationChunks.clear();
      capturePoints.addAll(caps);
      chunks.forEach(chunk -> nationChunks.put(chunk.getPos(),chunk));
      this.worldInitialized = true;
   }
   
   @Override
   public void addNation(Nation nation){
      nations.put(nation.getId(), nation);
   }
   
   @Override
   public void removeNation(String id){
      nations.remove(id);
   }
   
   @Override
   public Nation getNation(String id){
      return nations.get(id);
   }
   
   @Override
   public List<Nation> getNations(){
      return nations.values().stream().toList();
   }
   
   @Override
   public long getNextRift(){
      return nextRift;
   }
   
   @Override
   public long getNextWar(){
      return nextWar;
   }
   
   @Override
   public void setNextRift(long nextRift){
      this.nextRift = nextRift;
   }
   
   @Override
   public void setNextWar(long nextWar){
      this.nextWar = nextWar;
   }
   
   @Override
   public long getLastDayTick(){
      return lastDayTick;
   }
   
   @Override
   public long getLastHourTick(){
      return lastHourTick;
   }
   
   @Override
   public void setLastDayTick(long lastDayTick){
      this.lastDayTick = lastDayTick;
   }
   
   @Override
   public void setLastHourTick(long lastHourTick){
      this.lastHourTick = lastHourTick;
   }
   
   @Override
   public long getLastTimeCheck(){
      return lastTimeCheck;
   }
   
   @Override
   public void setLastTimeCheck(long lastTimeCheck){
      this.lastTimeCheck = lastTimeCheck;
   }
   
   @Override
   public CapturePoint getCapturePoint(ChunkPos pos){
      return capturePoints.stream().filter(cap -> cap.getChunkPos().equals(pos)).findFirst().orElse(null);
   }
   
   @Override
   public CapturePoint getCapturePoint(String id){
      return capturePoints.stream().filter(cap -> cap.getId().toString().equals(id)).findFirst().orElse(null);
   }
   
   @Override
   public List<CapturePoint> getCapturePoints(){
      return capturePoints.stream().toList();
   }
   
   @Override
   public NationChunk getChunk(ChunkPos pos){
      return nationChunks.get(pos);
   }
   
   @Override
   public List<NationChunk> getChunks(){
      return nationChunks.values().stream().toList();
   }
   
   @Override
   public NbtCompound getRiftData(){
      return riftData;
   }
   
   @Override
   public void setRiftData(NbtCompound riftData){
      this.riftData = riftData;
   }
   
   @Override
   public void readFromNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup){
      nations.clear();
      capturePoints.clear();
      nationChunks.clear();
      
      NbtList nationList = nbtCompound.getList("nations", NbtElement.COMPOUND_TYPE);
      NbtList capsList = nbtCompound.getList("capturePoints", NbtElement.COMPOUND_TYPE);
      NbtList chunkList = nbtCompound.getList("chunks", NbtElement.COMPOUND_TYPE);
      worldInitialized = nbtCompound.getBoolean("initialized");
      if(nbtCompound.contains("nextWar")) nextWar = nbtCompound.getLong("nextWar");
      if(nbtCompound.contains("nextRift")) nextRift = nbtCompound.getLong("nextRift");
      if(nbtCompound.contains("lastDayTick")) lastDayTick = nbtCompound.getLong("lastDayTick");
      if(nbtCompound.contains("lastHourTick")) lastHourTick = nbtCompound.getLong("lastHourTick");
      if(nbtCompound.contains("lastTimeCheck")) lastTimeCheck = nbtCompound.getLong("lastTimeCheck");
      if(nbtCompound.contains("riftData")) this.riftData = nbtCompound.getCompound("riftData");
      
      for(NbtElement e : chunkList){
         NbtCompound chunkComp = (NbtCompound) e;
         NationChunk chunk = NationChunk.loadFromNbt(chunkComp);
         if(chunk != null) nationChunks.put(chunk.getPos(),chunk);
      }
      
      for(NbtElement e : nationList){
         NbtCompound nationComp = (NbtCompound) e;
         Nation nation = Nation.loadFromNbt(nationComp, nationChunks, wrapperLookup);
         if(nation != null) nations.put(nation.getId(),nation);
      }
      capturePoints.addAll(capsList.stream().map(e -> CapturePoint.loadFromNbt((NbtCompound) e)).collect(Collectors.toSet()));
      WarManager.loadWarData(nbtCompound.getCompound("warData"));
   }
   
   @Override
   public void writeToNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup){
      NbtList nationList = new NbtList();
      NbtList capsList = new NbtList();
      NbtList chunkList = new NbtList();
      
      nationList.addAll(nations.values().stream().map(nation -> nation.saveToNbt(new NbtCompound(), wrapperLookup)).collect(Collectors.toSet()));
      capsList.addAll(capturePoints.stream().map(capturePoint -> capturePoint.saveToNbt(new NbtCompound())).collect(Collectors.toSet()));
      chunkList.addAll(nationChunks.values().stream().map(nationChunk -> nationChunk.saveToNbt(new NbtCompound())).collect(Collectors.toSet()));
      
      nbtCompound.put("nations",nationList);
      nbtCompound.put("capturePoints",capsList);
      nbtCompound.put("chunks",chunkList);
      nbtCompound.put("riftData",riftData);
      nbtCompound.put("warData",WarManager.saveWarData(new NbtCompound()));
      nbtCompound.putBoolean("initialized",worldInitialized);
      nbtCompound.putLong("nextWar",nextWar);
      nbtCompound.putLong("nextRift",nextRift);
      nbtCompound.putLong("lastDayTick", lastDayTick);
      nbtCompound.putLong("lastHourTick", lastHourTick);
      nbtCompound.putLong("lastTimeCheck", lastTimeCheck);
   }
   
}
