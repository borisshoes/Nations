package net.borisshoes.nations.integration;

import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.CapturePoint;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.NetherRift;
import net.borisshoes.nations.utils.NationsColors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static net.borisshoes.nations.Nations.MOD_ID;
import static net.borisshoes.nations.cca.WorldDataComponentInitializer.NATIONS_DATA;

public class DynmapFunctions {
   
   private static MarkerSet territoryMarkerSet, capturePointsMarkerSet, interestPointsMarkerSet;
   private static MarkerIcon growthIcon, materialIcon, researchIcon, monumentIcon, riftIcon;
   private static final String TERRITORY_MARKER_ID = "nations.territory", TERRITORY_MARKER_LABEL = "Territory";
   private static final String CAPTURE_POINTS_MARKER_ID = "nations.capture_points", CAPTURE_POINTS_MARKER_LABEL = "Capture Points";
   private static final String INTEREST_POINTS_MARKER_ID = "nations.interest_points", INTEREST_POINTS_MARKER_LABEL = "Points of Interest";
   
   private static final String NETHER_RIFT_MARKER = "nations.nether_rift_marker";
   private static final String SPAWN_AREA_MARKER = "nations.spawn_area_marker";
   private static final String BORDER_MARKER = "nations.border_marker";
   
   public static void reg() {
      DynmapCommonAPIListener.register(new DynmapCommonAPIListener() {
         @Override
         public void apiEnabled(DynmapCommonAPI dynmapCommonAPI) {
            MarkerAPI markerAPI = dynmapCommonAPI.getMarkerAPI();
            territoryMarkerSet = markerAPI.createMarkerSet(TERRITORY_MARKER_ID, TERRITORY_MARKER_LABEL, dynmapCommonAPI.getMarkerAPI().getMarkerIcons(), false);
            capturePointsMarkerSet = markerAPI.createMarkerSet(CAPTURE_POINTS_MARKER_ID, CAPTURE_POINTS_MARKER_LABEL, dynmapCommonAPI.getMarkerAPI().getMarkerIcons(), false);
            interestPointsMarkerSet = markerAPI.createMarkerSet(INTEREST_POINTS_MARKER_ID, INTEREST_POINTS_MARKER_LABEL, dynmapCommonAPI.getMarkerAPI().getMarkerIcons(), false);
            growthIcon = getAndRefreshMarkerIcon(markerAPI,"capturepointgrowth","Growth Capture Point Icon","capture_point_growth");
            materialIcon = getAndRefreshMarkerIcon(markerAPI,"capturepointmaterial","Material Capture Point Icon","capture_point_material");
            researchIcon = getAndRefreshMarkerIcon(markerAPI,"capturepointresearch","Research Capture Point Icon","capture_point_research");
            monumentIcon = getAndRefreshMarkerIcon(markerAPI,"nationmonument","Nation Monument Icon","nation_monument");
            riftIcon = getAndRefreshMarkerIcon(markerAPI,"netherrift","Nether Rift Icon","nether_rift");
            capturePointsMarkerSet.addAllowedMarkerIcon(growthIcon);
            capturePointsMarkerSet.addAllowedMarkerIcon(materialIcon);
            capturePointsMarkerSet.addAllowedMarkerIcon(researchIcon);
            territoryMarkerSet.addAllowedMarkerIcon(monumentIcon);
            interestPointsMarkerSet.addAllowedMarkerIcon(riftIcon);
            DynmapCalls.dynmapLoaded = true;
         }
      });
   }
   
   protected static void addCapturePointMarker(CapturePoint cap){
      if(capturePointsMarkerSet == null) return;
      BlockPos capPos = cap.getBeaconPos();
      MarkerIcon icon = switch(cap.getType()){
         case GROWTH -> growthIcon;
         case MATERIAL -> materialIcon;
         case RESEARCH -> researchIcon;
      };
      if(icon == null) icon = capturePointsMarkerSet.getDefaultMarkerIcon();
      Marker marker = capturePointsMarkerSet.createMarker(cap.getId().toString(),cap.getMarkerLabel(),false,getWorldName(),capPos.getX(),capPos.getY(),capPos.getZ(),icon,false);
      CircleMarker circleMarker = capturePointsMarkerSet.createCircleMarker(cap.getId().toString(),cap.getMarkerLabel(),false,getWorldName(),capPos.getX(),capPos.getY(),capPos.getZ(),16,16,false);
      int color = cap.getControllingNation() == null ? 0xffffff : cap.getControllingNation().getTextColor();
      circleMarker.setLineStyle(2,0.8,color);
      circleMarker.setFillStyle(cap.getAuctionStartTime() != 0 ? 0.5 : 0.1,color);
   }
   
   protected static void updateCapturePointMarker(CapturePoint cap){
      if(capturePointsMarkerSet == null) return;
      Marker marker = capturePointsMarkerSet.findMarker(cap.getId().toString());
      CircleMarker circleMarker = capturePointsMarkerSet.findCircleMarker(cap.getId().toString());
      if(marker == null || circleMarker == null){
         addCapturePointMarker(cap);
         return;
      }
      marker.setLabel(cap.getMarkerLabel());
      circleMarker.setLabel(cap.getMarkerLabel());
      int color = cap.getControllingNation() == null ? 0xffffff : cap.getControllingNation().getTextColor();
      circleMarker.setLineStyle(2,0.8,color);
      circleMarker.setFillStyle(cap.getAuctionStartTime() != 0 ? 0.5 : 0.1,color);
   }
   
   protected static void addSpawnMarker(){
      if(interestPointsMarkerSet == null) return;
      int radius = NationsConfig.getInt(NationsRegistry.SPAWN_RADIUS_CFG);
      
      BlockPos corner1 = new ChunkPos(-radius,-radius).getBlockPos(0,0,0);
      BlockPos corner2 = new ChunkPos(radius-1, radius-1).getBlockPos(15,0,15);
      AreaMarker marker = interestPointsMarkerSet.createAreaMarker(SPAWN_AREA_MARKER,"Spawn",false, getWorldName(), new double[]{corner1.getX(),corner2.getX()}, new double[]{corner1.getZ(),corner2.getZ()}, false);
      if(marker == null) return;
      marker.setLineStyle(5,0.8, NationsColors.SPAWN_MAP_BORDER_COLOR);
      marker.setFillStyle(0.2, NationsColors.SPAWN_MAP_FILL_COLOR);
   }
   
   
   protected static void addBorderMarker(){
      if(interestPointsMarkerSet == null) return;
      int radius = NationsConfig.getInt(NationsRegistry.WORLD_BORDER_RADIUS_OVERWORLD_CFG);
      
      BlockPos corner1 = new ChunkPos(-radius,-radius).getBlockPos(0,64,0);
      BlockPos corner2 = new ChunkPos(radius-1, radius-1).getBlockPos(15,64,15);
      PolyLineMarker marker = interestPointsMarkerSet.createPolyLineMarker(BORDER_MARKER,"World Border",false, getWorldName(),
            new double[]{corner1.getX(),corner1.getX(),corner2.getX(),corner2.getX(),corner1.getX()},
            new double[]{corner1.getY(),corner2.getY(),corner1.getY(),corner2.getY(),corner2.getY()},
            new double[]{corner1.getZ(),corner2.getZ(),corner2.getZ(),corner1.getZ(),corner1.getZ()}, false);
      if(marker == null) return;
      marker.setLineStyle(7,0.8, NationsColors.SPAWN_MAP_FILL_COLOR);
   }
   
   protected static void redrawNationBorder(Nation nation){
      if(territoryMarkerSet == null || !nation.isFounded()) return;
      territoryMarkerSet.getAreaMarkers().forEach(areaMarker -> {
         if(areaMarker.getMarkerID().contains(nation.getId())) areaMarker.deleteMarker();
      });
      
      List<BlockPos> influenceCorners = nation.getInfluenceCorners();
      List<List<BlockPos>> claimCorners = nation.getClaimCorners();
      
      double[] xVals = new double[influenceCorners.size()];
      double[] zVals = new double[influenceCorners.size()];
      for(int i = 0; i < influenceCorners.size(); i++){
         xVals[i] = influenceCorners.get(i).getX();
         zVals[i] = influenceCorners.get(i).getZ();
      }
      AreaMarker influenceMarker = territoryMarkerSet.createAreaMarker(nation.getId()+".influence",nation.getName()+" - Influence",false, getWorldName(), xVals, zVals, false);
      influenceMarker.setLineStyle(5,0.8,nation.getTextColor());
      influenceMarker.setFillStyle(0.1,nation.getTextColorSub());
      int idx = 0;
      for(List<BlockPos> claimCorner : claimCorners){
         xVals = new double[claimCorner.size()];
         zVals = new double[claimCorner.size()];
         for(int i = 0; i < claimCorner.size(); i++){
            xVals[i] = claimCorner.get(i).getX();
            zVals[i] = claimCorner.get(i).getZ();
         }
         AreaMarker claimMarker = territoryMarkerSet.createAreaMarker(nation.getId()+".claim."+idx,nation.getName()+" - Claim",false, getWorldName(), xVals, zVals, false);
         claimMarker.setLineStyle(3,0.5,nation.getTextColorSub());
         claimMarker.setFillStyle(0.2,nation.getTextColor());
         idx++;
      }
      
      BlockPos pos = nation.getFoundingPos().add(8,8,8);
      MarkerIcon icon = monumentIcon != null ? monumentIcon : territoryMarkerSet.getDefaultMarkerIcon();
      Marker marker = territoryMarkerSet.createMarker(nation.getId()+".center",nation.getName()+" - Monument",false,getWorldName(),pos.getX(),pos.getY(),pos.getZ(),icon,false);
   }
   
   public static void updateNetherRiftMarker(){
      if(interestPointsMarkerSet == null) return;
      MarkerIcon icon = riftIcon;
      if(icon == null) icon = interestPointsMarkerSet.getDefaultMarkerIcon();
      Marker marker = interestPointsMarkerSet.findMarker(NETHER_RIFT_MARKER);
      if(marker != null){
         marker.deleteMarker();
      }
      NetherRift rift = Nations.LAST_RIFT;
      if(rift == null || !rift.isOpen()) return;
      Vec3d pos = rift.getOverworldCenter();
      interestPointsMarkerSet.createMarker(NETHER_RIFT_MARKER,"Nether Rift",false,getWorldName(),pos.getX(),pos.getY(),pos.getZ(),icon,false);
   }
   
   protected static void redrawDynmap(){
      if(territoryMarkerSet != null){
         territoryMarkerSet.getAreaMarkers().forEach(GenericMarker::deleteMarker);
         territoryMarkerSet.getMarkers().forEach(GenericMarker::deleteMarker);
         territoryMarkerSet.getCircleMarkers().forEach(GenericMarker::deleteMarker);
         territoryMarkerSet.getPolyLineMarkers().forEach(GenericMarker::deleteMarker);
         
         for(Nation nation : Nations.getNations()){
            redrawNationBorder(nation);
         }
      }
      
      if(capturePointsMarkerSet != null){
         capturePointsMarkerSet.getAreaMarkers().forEach(GenericMarker::deleteMarker);
         capturePointsMarkerSet.getMarkers().forEach(GenericMarker::deleteMarker);
         capturePointsMarkerSet.getCircleMarkers().forEach(GenericMarker::deleteMarker);
         capturePointsMarkerSet.getPolyLineMarkers().forEach(GenericMarker::deleteMarker);
         
         for(CapturePoint capturePoint : Nations.getCapturePoints()){
            updateCapturePointMarker(capturePoint);
         }
      }
      
      if(interestPointsMarkerSet != null){
         interestPointsMarkerSet.getAreaMarkers().forEach(GenericMarker::deleteMarker);
         interestPointsMarkerSet.getMarkers().forEach(GenericMarker::deleteMarker);
         interestPointsMarkerSet.getCircleMarkers().forEach(GenericMarker::deleteMarker);
         interestPointsMarkerSet.getPolyLineMarkers().forEach(GenericMarker::deleteMarker);
         
         addSpawnMarker();
         addBorderMarker();
         updateNetherRiftMarker();
      }
   }
   
   private static String getWorldName(){
      return Nations.SERVER.getSaveProperties().getLevelName();
   }
   
   private static InputStream getMarkerIcon(String id){
      try{
         Optional<Optional<Path>> pathOptional = FabricLoader.getInstance().getModContainer(MOD_ID).map(container -> container.findPath("assets/" + MOD_ID + "/icons/" + id + ".png"));
         if(pathOptional.isEmpty() || pathOptional.get().isEmpty()){
            return null;
         }
         Path path = pathOptional.get().get();
         return Files.newInputStream(path);
      }catch(Exception e){
         e.printStackTrace();
      }
      return null;
   }
   
   private static MarkerIcon getAndRefreshMarkerIcon(MarkerAPI api, String markerId, String label, String assetId){
      MarkerIcon icon = api.getMarkerIcon(markerId);
      if(icon != null){
         icon.setMarkerIconImage(getMarkerIcon(assetId));
         icon.setMarkerIconLabel(label);
         return icon;
      }else{
         return api.createMarkerIcon(markerId,label,getMarkerIcon(assetId));
      }
   }
}
