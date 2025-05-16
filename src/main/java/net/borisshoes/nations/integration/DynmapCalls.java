package net.borisshoes.nations.integration;

import net.borisshoes.nations.gameplay.CapturePoint;
import net.borisshoes.nations.gameplay.Nation;
import net.minecraft.util.math.BlockPos;

public class DynmapCalls {
   
   public static boolean dynmapLoaded;
   
   public static void redrawDynmap(){
      if(!dynmapLoaded) return;
      DynmapFunctions.redrawDynmap();
   }
   
   public static void addCapturePointMarker(CapturePoint cap){
      if(!dynmapLoaded) return;
      DynmapFunctions.addCapturePointMarker(cap);
   }
   
   public static void redrawNationBorder(Nation nation){
      if(!dynmapLoaded) return;
      DynmapFunctions.redrawNationBorder(nation);
   }
   
   public static void updateCapturePointMarker(CapturePoint cap){
      if(!dynmapLoaded) return;
      DynmapFunctions.updateCapturePointMarker(cap);
   }
   
   public static void addSpawnMarker(){
      if(!dynmapLoaded) return;
      DynmapFunctions.addSpawnMarker();
   }
   
   public static void addBorderMarker(){
      if(!dynmapLoaded) return;
      DynmapFunctions.addBorderMarker();
   }
   
   public static void updateNetherRiftMarker(){
      if(!dynmapLoaded) return;
      DynmapFunctions.updateNetherRiftMarker();
   }
}
