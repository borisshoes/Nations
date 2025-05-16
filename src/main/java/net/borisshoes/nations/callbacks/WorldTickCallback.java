package net.borisshoes.nations.callbacks;

import net.borisshoes.arcananovum.blocks.ContinuumAnchor;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.CapturePoint;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.NationChunk;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static net.borisshoes.nations.Nations.WORLD_TIMER_CALLBACKS;

public class WorldTickCallback {
   
   public static void onWorldTick(ServerWorld serverWorld){
      try{
         // Tick Timer Callbacks
         WORLD_TIMER_CALLBACKS.removeIf(tickTimers(serverWorld)::contains);
         
         if(serverWorld.getRegistryKey().equals(ServerWorld.OVERWORLD)){
            for(CapturePoint capturePoint : Nations.getCapturePoints()){
               capturePoint.tick(serverWorld);
            }
            for(Nation nation : Nations.getNations()){
               nation.tick(serverWorld);
            }
            
            for(NationChunk chunk : Nations.getChunks()){
               if(chunk.isAnchored()){
                  serverWorld.getChunkManager().addTicket(NationsRegistry.TICKET_TYPE,chunk.getPos(),2,chunk.getPos());
               }
            }
            
            if(Nations.SHOP != null){
               Nations.SHOP.tick();
            }
         }
         
         
         
      }catch(Exception e){
         e.printStackTrace();
      }
   }
   
   @NotNull
   private static ArrayList<Pair<ServerWorld, TickTimerCallback>> tickTimers(ServerWorld serverWorld){
      ArrayList<Pair<ServerWorld,TickTimerCallback>> toRemove = new ArrayList<>();
      for(int i = 0; i < WORLD_TIMER_CALLBACKS.size(); i++){
         Pair<ServerWorld,TickTimerCallback> pair = WORLD_TIMER_CALLBACKS.get(i);
         TickTimerCallback t = pair.getRight();
         if(pair.getLeft().getRegistryKey() == serverWorld.getRegistryKey()){
            if(t.decreaseTimer() == 0){
               t.onTimer();
               toRemove.add(pair);
            }
         }
      }
      return toRemove;
   }
}
