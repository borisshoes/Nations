package net.borisshoes.nations.cca;

import net.borisshoes.nations.Nations;
import net.borisshoes.nations.gameplay.CapturePoint;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.NationChunk;
import net.borisshoes.nations.gameplay.WarManager;
import net.borisshoes.nations.utils.MiscUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.Nullable;

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
   private final ArrayList<NationChunk> chunkList = new ArrayList<>();
   
   private final HashMap<UUID, UUID> killOnRelog = new HashMap<>();
   
   @Override
   public boolean isWorldInitialized(){
      return worldInitialized;
   }
   
   @Override
   public void initializeWorld(List<CapturePoint> caps, List<NationChunk> chunks){
      nations.clear();
      capturePoints.clear();
      nationChunks.clear();
      chunkList.clear();
      capturePoints.addAll(caps);
      chunkList.addAll(chunks);
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
      return chunkList;
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
   public @Nullable UUID shouldKillPlayerOnRelog(UUID playerId){
      return killOnRelog.getOrDefault(playerId,null);
   }
   
   @Override
   public void addKillOnRelog(UUID playerId, UUID killer){
      killOnRelog.put(playerId,killer);
   }
   
   @Override
   public void removeKillOnRelog(UUID playerId){
      killOnRelog.remove(playerId);
   }
   
   @Override
   public void readFromNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup){
      nations.clear();
      capturePoints.clear();
      nationChunks.clear();
      chunkList.clear();
      killOnRelog.clear();
      
      NbtList nationList = nbtCompound.getList("nations", NbtElement.COMPOUND_TYPE);
      NbtList capsList = nbtCompound.getList("capturePoints", NbtElement.COMPOUND_TYPE);
      NbtList nChunkList = nbtCompound.getList("chunks", NbtElement.COMPOUND_TYPE);
      NbtCompound killComp = nbtCompound.getCompound("killOnRelog");
      worldInitialized = nbtCompound.getBoolean("initialized");
      if(nbtCompound.contains("nextWar")) nextWar = nbtCompound.getLong("nextWar");
      if(nbtCompound.contains("nextRift")) nextRift = nbtCompound.getLong("nextRift");
      if(nbtCompound.contains("lastDayTick")) lastDayTick = nbtCompound.getLong("lastDayTick");
      if(nbtCompound.contains("lastHourTick")) lastHourTick = nbtCompound.getLong("lastHourTick");
      if(nbtCompound.contains("lastTimeCheck")) lastTimeCheck = nbtCompound.getLong("lastTimeCheck");
      if(nbtCompound.contains("riftData")) this.riftData = nbtCompound.getCompound("riftData");
      
      for(String key : killComp.getKeys()){
         UUID player = MiscUtils.getUUID(key);
         UUID killer = MiscUtils.getUUID(killComp.getString(key));
         if(!player.toString().equals(Nations.BLANK_UUID) && !killer.toString().equals(Nations.BLANK_UUID)){
            killOnRelog.put(player,killer);
         }
      }
      
      for(NbtElement e : nChunkList){
         NbtCompound chunkComp = (NbtCompound) e;
         NationChunk chunk = NationChunk.loadFromNbt(chunkComp);
         if(chunk != null){
            chunkList.add(chunk);
            nationChunks.put(chunk.getPos(), chunk);
         }
      }
      
      for(NbtElement e : nationList){
         NbtCompound nationComp = (NbtCompound) e;
         Nation nation = Nation.loadFromNbt(nationComp, nationChunks, wrapperLookup);
         if(nation != null) nations.put(nation.getId(),nation);
      }
      capturePoints.addAll(capsList.stream().map(e -> CapturePoint.loadFromNbt((NbtCompound) e)).collect(Collectors.toSet()));
   }
   
   @Override
   public void writeToNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup){
      NbtList nationList = new NbtList();
      NbtList capsList = new NbtList();
      NbtList nChunkList = new NbtList();
      NbtCompound killComp = new NbtCompound();
      
      nationList.addAll(nations.values().stream().map(nation -> nation.saveToNbt(new NbtCompound(), wrapperLookup)).collect(Collectors.toSet()));
      capsList.addAll(capturePoints.stream().map(capturePoint -> capturePoint.saveToNbt(new NbtCompound())).collect(Collectors.toSet()));
      nChunkList.addAll(chunkList.stream().map(nationChunk -> nationChunk.saveToNbt(new NbtCompound())).collect(Collectors.toSet()));
      killOnRelog.forEach((player, killer) -> killComp.putString(player.toString(),killer.toString()));
      
      nbtCompound.put("nations",nationList);
      nbtCompound.put("capturePoints",capsList);
      nbtCompound.put("chunks",nChunkList);
      nbtCompound.put("riftData",riftData);
      nbtCompound.putBoolean("initialized",worldInitialized);
      nbtCompound.putLong("nextWar",nextWar);
      nbtCompound.putLong("nextRift",nextRift);
      nbtCompound.putLong("lastDayTick", lastDayTick);
      nbtCompound.putLong("lastHourTick", lastHourTick);
      nbtCompound.putLong("lastTimeCheck", lastTimeCheck);
   }
   
}
