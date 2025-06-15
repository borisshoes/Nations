package net.borisshoes.nations.gameplay;

import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.cca.INationsDataComponent;
import net.borisshoes.nations.research.ResearchTech;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import static net.borisshoes.nations.Nations.TECH_TRACKER;
import static net.borisshoes.nations.cca.WorldDataComponentInitializer.NATIONS_DATA;

public class TimedEvents {
   
   public static void tickTimedEvents(MinecraftServer server){
      INationsDataComponent data = NATIONS_DATA.get(server.getOverworld());
      if(!data.isWorldInitialized()) return;
      TimedEvents.updateChunkCache(server);
      
      long curTime = System.currentTimeMillis();
      long nextRift = data.getNextRift();
      long nextWar = data.getNextWar();
      long lastDayTick = data.getLastDayTick();
      long lastHourTick = data.getLastHourTick();
      
      ZoneId zone = ZoneId.systemDefault();
      ZonedDateTime now = ZonedDateTime.now(zone);
      ZonedDateTime lastHourTickZDT = Instant.ofEpochMilli(lastHourTick).atZone(zone);
      ZonedDateTime startOfThisHour = now.truncatedTo(ChronoUnit.HOURS);
      boolean beenHour = !now.isBefore(startOfThisHour) && now.isBefore(startOfThisHour.plusMinutes(1)) && ChronoUnit.MINUTES.between(lastHourTickZDT, now) >= 59;
      
      ZonedDateTime todayNoon = now.withHour(12).withMinute(0).withSecond(0).withNano(0);
      ZonedDateTime lastDayTickZDT = Instant.ofEpochMilli(lastDayTick).atZone(zone);
      boolean beenDay = !now.isBefore(todayNoon) && now.isBefore(todayNoon.plusMinutes(1)) && ChronoUnit.MINUTES.between(lastDayTickZDT, now) >= 1439;
      
      if(beenDay){ // Do daily tick
         TimedEvents.doDailyTick(server);
         data.setLastDayTick(curTime);
      }
      if(beenHour){ // Do hourly tick
         TimedEvents.doHourlyTick(server);
         data.setLastHourTick(curTime);
      }
      
      data.setLastTimeCheck(curTime);
      
      
      if(curTime > nextWar){ // Do War
         boolean warEnabled = NationsConfig.getBoolean(NationsRegistry.WAR_ENABLED_CFG);
         if(warEnabled) TimedEvents.startWar(server);
         data.setNextWar(now.plusWeeks(1L).toEpochSecond()*1000L);
      }
      
      if(curTime > nextRift){ // Open Rift
         boolean riftsEnabled = NationsConfig.getBoolean(NationsRegistry.RIFTS_ENABLED_CFG);
         if(riftsEnabled) TimedEvents.openRift(server);
         int max = NationsConfig.getInt(NationsRegistry.RIFT_MAX_COOLDOWN_CFG);
         int min = NationsConfig.getInt(NationsRegistry.RIFT_MIN_COOLDOWN_CFG);
         int minutes = (int) (Math.random() * (max-min) + min);
         data.setNextRift(now.plusMinutes(minutes).toEpochSecond()*1000);
      }
      
      if(Nations.LAST_RIFT != null){
         Nations.LAST_RIFT.tick();
      }
   }
   
   public static void doDailyTick(MinecraftServer server){
      TECH_TRACKER.clear();
      Nations.getNations().forEach(nation -> TECH_TRACKER.put(nation,nation.getCompletedTechs().stream().map(ResearchTech::getKey).collect(Collectors.toSet())));
      Nations.getCapturePoints().forEach(cap -> cap.dailyTick(server.getOverworld()));
      Nations.getNations().forEach(nation -> nation.dailyTick(server.getOverworld()));
      TECH_TRACKER.clear();
   }
   
   public static void doHourlyTick(MinecraftServer server){
      TECH_TRACKER.clear();
      Nations.getNations().forEach(nation -> TECH_TRACKER.put(nation,nation.getCompletedTechs().stream().map(ResearchTech::getKey).collect(Collectors.toSet())));
      Nations.getCapturePoints().forEach(cap -> cap.hourlyTick(server.getOverworld()));
      Nations.getNations().forEach(nation -> nation.hourlyTick(server.getOverworld()));
      TECH_TRACKER.clear();
   }
   
   private static void startWar(MinecraftServer server){
      WarManager.startWar();
   }
   
   private static void openRift(MinecraftServer server){
      INationsDataComponent data = NATIONS_DATA.get(server.getOverworld());
      int duration = NationsConfig.getInt(NationsRegistry.RIFT_DURATION_CFG);
      int warmup = NationsConfig.getInt(NationsRegistry.RIFT_WARMUP_CFG);
      Nations.LAST_RIFT = new NetherRift(server.getOverworld(),server.getWorld(World.NETHER),warmup*20L,duration*20L);
      data.setRiftData(Nations.LAST_RIFT.toNbt(new NbtCompound()));
   }
   
   private static void updateChunkCache(MinecraftServer server){
      ServerWorld world = server.getOverworld();
      double updatesPerMinute = NationsConfig.getDouble(NationsRegistry.CHUNK_CACHE_UPDATES_PER_MINUTE_CFG);
      if(updatesPerMinute == 0) return;
      
      double updatesPerTick = updatesPerMinute / 1200.0;
      double total = server.getTicks() * updatesPerTick;
      double totalLast = (server.getTicks() - 1) * updatesPerTick;
      int diff = (int) Math.floor(total) - (int) Math.floor(totalLast);
      
      int radius = NationsConfig.getInt(NationsRegistry.WORLD_BORDER_RADIUS_OVERWORLD_CFG);
      for(int i = 0; i < diff; i++){
         int x = (int) (Math.random() * (2*radius) - radius);
         int z = (int) (Math.random() * (2*radius) - radius);
         NationChunk chunk = Nations.getChunk(x,z);
         if(chunk == null){
            i--;
         }else{
            chunk.updateYield(world);
         }
      }
   }
}
