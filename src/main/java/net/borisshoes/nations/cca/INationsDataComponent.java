package net.borisshoes.nations.cca;

import net.borisshoes.nations.gameplay.CapturePoint;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.NationChunk;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.ChunkPos;
import org.ladysnake.cca.api.v3.component.ComponentV3;

import java.util.List;
import java.util.UUID;

public interface INationsDataComponent extends ComponentV3 {
   Nation getNation(String id);
   List<Nation> getNations();
   NationChunk getChunk(ChunkPos pos);
   List<NationChunk> getChunks();
   CapturePoint getCapturePoint(ChunkPos pos);
   CapturePoint getCapturePoint(String id);
   List<CapturePoint> getCapturePoints();
   NbtCompound getRiftData();
   UUID shouldKillPlayerOnRelog(UUID playerId);
   boolean isWorldInitialized();
   long getNextWar();
   long getNextRift();
   long getLastHourTick();
   long getLastDayTick();
   long getLastTimeCheck();
   
   void initializeWorld(List<CapturePoint> caps, List<NationChunk> chunks);
   void addNation(Nation nation);
   void removeNation(String id);
   void setNextWar(long time);
   void setNextRift(long time);
   void setLastHourTick(long time);
   void setLastDayTick(long time);
   void setLastTimeCheck(long time);
   void setRiftData(NbtCompound riftData);
   void addKillOnRelog(UUID playerId, UUID killer);
   void removeKillOnRelog(UUID playerId);
}
