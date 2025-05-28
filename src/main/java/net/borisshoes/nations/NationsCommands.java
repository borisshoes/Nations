package net.borisshoes.nations;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.borisshoes.nations.cca.INationsDataComponent;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.borisshoes.nations.gameplay.*;
import net.borisshoes.nations.gui.ChunkGui;
import net.borisshoes.nations.integration.DynmapCalls;
import net.borisshoes.nations.land.NationsLand;
import net.borisshoes.nations.research.ResearchTech;
import net.borisshoes.nations.utils.GenericTimer;
import net.borisshoes.nations.utils.NationsUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.borisshoes.nations.Nations.*;
import static net.borisshoes.nations.cca.PlayerComponentInitializer.PLAYER_DATA;
import static net.borisshoes.nations.cca.WorldDataComponentInitializer.NATIONS_DATA;

public class NationsCommands {
   
   public static final HashMap<ServerPlayerEntity, ChunkPos> SETTLE_WARNING = new HashMap<>();
   
   public static CompletableFuture<Suggestions> getNationSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder){
      String start = builder.getRemaining().toLowerCase(Locale.ROOT);
      Set<String> items = new HashSet<>();
      for(Nation nation : getNations()){
         items.add(nation.getId().toLowerCase(Locale.ROOT));
      }
      items.stream().filter(s -> s.startsWith(start)).forEach(builder::suggest);
      return builder.buildFuture();
   }
   
   public static CompletableFuture<Suggestions> getResearchSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder){
      String start = builder.getRemaining().toLowerCase(Locale.ROOT);
      Set<String> items = new HashSet<>();
      for(ResearchTech research : NationsRegistry.RESEARCH){
         items.add(research.getId().toLowerCase(Locale.ROOT));
      }
      items.stream().filter(s -> s.startsWith(start)).forEach(builder::suggest);
      return builder.buildFuture();
   }
   
   public static <E extends Enum<E>> CompletableFuture<Suggestions> getEnumSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, Class<E> enumClass){
      String start = builder.getRemaining().toLowerCase(Locale.ROOT);
      Set<String> items = new HashSet<>();
      for(E value : enumClass.getEnumConstants()){
         items.add(value.name().toLowerCase(Locale.ROOT));
      }
      items.stream().filter(s -> s.startsWith(start)).forEach(builder::suggest);
      return builder.buildFuture();
   }
   
   public static int test(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         
         NbtElement nbt = src.getPlayer().getMainHandStack().toNbt(src.getServer().getRegistryManager());
         String sellNbt = nbt.asString();
         src.sendMessage(Text.literal(sellNbt).styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD,sellNbt))));
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int surveyWholeWorld(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         ServerWorld world = src.getWorld();
         INationsDataComponent nationsData = NATIONS_DATA.get(src.getServer().getOverworld());
         HashMap<ResourceType,Integer> map = new HashMap<>();
         
         HashMap<ChunkPos,Pair<Triple<Integer,Integer,Integer>,Triple<Integer,Integer,Integer>>> resultsMap = new HashMap<>();
         HashMap<ChunkPos,Pair<Triple<Integer,Integer,Integer>,Triple<Integer,Integer,Integer>>> chunkMap = new HashMap<>();
         
         int radius = NationsConfig.getInt(NationsRegistry.SETTLE_RADIUS_CFG);
         int count = 1;
         for(NationChunk chunk : nationsData.getChunks()){
            ChunkPos pos = chunk.getPos();
            int finalCount = count;
            Nations.addTickTimerCallback(world, new GenericTimer(count, () -> {
               Triple<Integer, Integer, Integer> chunkValues = NationsUtils.calculateChunkCoinGeneration(ctx.getSource().getWorld(), pos);
               Triple<Integer, Integer, Integer> capValues = new ImmutableTriple<>(0,0,0);
               
               CapturePoint cap = Nations.getCapturePoint(new ChunkPos(pos.x, pos.z));
               if(cap != null){
                  switch(cap.getType()){
                     case GROWTH -> capValues = new ImmutableTriple<>(cap.getRawYield(),0,0);
                     case MATERIAL -> capValues = new ImmutableTriple<>(0,cap.getRawYield(),0);
                     case RESEARCH -> capValues = new ImmutableTriple<>(0,0,cap.getRawYield());
                  }
               }
               chunkMap.put(new ChunkPos(pos.x, pos.z),new Pair<>(
                     new ImmutableTriple<>(chunkValues.getLeft(),chunkValues.getMiddle(),chunkValues.getRight()), capValues
               ));
               
               if(finalCount % 100 == 0) System.out.println("Processed: "+ finalCount);
            }));
            count++;
         }
         
         Nations.addTickTimerCallback(world, new GenericTimer((count+2), () -> {
            for(NationChunk chunk : nationsData.getChunks()){
               ChunkPos pos = chunk.getPos();
               int growthRaw = 0;
               int materialRaw = 0;
               int researchRaw = 0;
               int growthCaps = 0;
               int materialCaps = 0;
               int researchCaps = 0;
               
               for (int dx = -radius; dx <= radius; dx++){
                  for(int dz = -radius; dz <= radius; dz++){
                     if(Math.abs(dx) + Math.abs(dz) <= radius){
                        ChunkPos chunkPos = new ChunkPos(pos.x + dx, pos.z + dz);
                        if(NationsLand.isOutOfBounds(world.getRegistryKey(), chunkPos)) continue;
                        Pair<Triple<Integer,Integer,Integer>,Triple<Integer,Integer,Integer>> data = chunkMap.get(chunkPos);
                        growthRaw += data.getLeft().getLeft();
                        materialRaw += data.getLeft().getMiddle();
                        researchRaw += data.getLeft().getRight();
                        growthCaps += data.getRight().getLeft();
                        materialCaps += data.getRight().getMiddle();
                        researchCaps += data.getRight().getRight();
                     }
                  }
               }
               
               resultsMap.put(pos,new Pair<>(
                     new ImmutableTriple<>((int) (growthRaw*0.01),(int) (materialRaw*0.01),(int) (researchRaw*0.01)),
                     new ImmutableTriple<>(growthCaps,materialCaps,researchCaps)
               ));
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\Boris\\Desktop\\data.txt"))){
               for(Map.Entry<ChunkPos, Pair<Triple<Integer, Integer, Integer>, Triple<Integer, Integer, Integer>>> entry : resultsMap.entrySet()){
                  ChunkPos pos = entry.getKey();
                  Triple<Integer, Integer, Integer> raw = entry.getValue().getLeft();
                  Triple<Integer, Integer, Integer> caps = entry.getValue().getRight();
                  int growthRaw = raw.getLeft();
                  int materialRaw = raw.getMiddle();
                  int researchRaw = raw.getRight();
                  int growthTotal = raw.getLeft() + caps.getLeft();
                  int materialTotal = raw.getMiddle() + caps.getMiddle();
                  int researchTotal = raw.getRight() + caps.getRight();
                  int rawTotal = growthRaw+materialRaw+researchRaw;
                  int total = growthTotal+materialTotal+researchTotal;
                  String dataPoint = "("+pos.x+","+pos.z+", ["+growthRaw+","+materialRaw+","+researchRaw+","+growthTotal+","+materialTotal+","+researchTotal+","+rawTotal+","+total+"])";
                  writer.write(dataPoint+",");
               }
            }catch(Exception e){
               log(2,e.toString());
            }
         }));
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int adminSurvey(CommandContext<ServerCommandSource> ctx, int radius){
      try{
         ServerCommandSource src = ctx.getSource();
         Vec3d pos = src.getPosition();
         if(!Nations.isWorldInitialized()){
            src.sendError(Text.translatable("text.nations.world_not_initialized"));
            return -1;
         }
         
         Triple<Integer,Integer,Integer> values = NationsUtils.calculateChunkCoinGeneration(ctx.getSource().getWorld(),pos,radius);
         
         src.sendMessage(Text.translatable("text.nations.survey",radius,
               Text.literal(values.getLeft()+" ").append(ResourceType.GROWTH.getText()).formatted(Formatting.GREEN),
               Text.literal(values.getMiddle()+" ").append(ResourceType.MATERIAL.getText()).formatted(Formatting.GOLD),
               Text.literal(values.getRight()+" ").append(ResourceType.RESEARCH.getText()).formatted(Formatting.AQUA)));
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   private static boolean initializing = false;
   private static List<CapturePoint> initializingPoints = new ArrayList<>();
   public static int initializeWorld(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         ServerWorld world = src.getWorld();
         INationsDataComponent nationsData = NATIONS_DATA.get(src.getServer().getOverworld());
         if(nationsData.isWorldInitialized() || initializing){
            src.sendFeedback(() -> Text.translatable("text.nations.world_already_initialized"),false);
            return 0;
         }
         initializing = true;
         initializingPoints.clear();
         
         int border = NationsConfig.getInt(NationsRegistry.WORLD_BORDER_RADIUS_OVERWORLD_CFG);
         int minDist = NationsConfig.getInt(NationsRegistry.CAPTURE_POINT_MIN_DIST_CFG);
         int spawnRadius = NationsConfig.getInt(NationsRegistry.SPAWN_RADIUS_CFG) + 1;
         long seed = world.getSeed();
         
         List<NationChunk> chunks = new ArrayList<>();
         for(int x = -border; x < border; x++){
            for(int z = -border; z < border; z++){
               chunks.add(new NationChunk(new ChunkPos(x,z)));
            }
         }
         src.sendMessage(Text.literal("Initialized "+chunks.size()+" chunks. Beginning Capture Point generation..."));
         
         Set<ChunkPos> points = NationsUtils.generatePoissonPoints(-border,border,-border,border,seed,minDist);
         Random rand = Random.create(seed);
         ResourceType[] types = ResourceType.values();
         StringBuilder listStr = new StringBuilder("[");
         int count = 0;
         for(ChunkPos point : points){
            if(point.x >= -spawnRadius && point.x < spawnRadius && point.z >= -spawnRadius && point.z < spawnRadius) continue;
            final ChunkPos chunkPos = new ChunkPos(point.x,point.z);
            final int finalCount = count;
            Nations.addTickTimerCallback(world, new GenericTimer(20*count, () -> {
               Heightmap heightmap = world.getChunk(chunkPos.x,chunkPos.z).getHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES);
               double avg = 0;
               double blockCount = 0;
               for(int i = 3; i <= 13; i++){
                  for(int j = 3; j <= 13; j++){
                     avg += heightmap.get(i,j);
                     blockCount++;
                  }
               }
               int height = (int) (avg/blockCount)-3;
               CapturePoint newCap = new CapturePoint(chunkPos,height,types[rand.nextInt(types.length)],rand.nextBetween(100,250));
               initializingPoints.add(newCap);
               
               StructurePlacer.placeStructure(CAPTURE_POINT_STRUCTURES.get(newCap.getType()), ctx.getSource().getServer().getOverworld(), chunkPos.getBlockPos(3,height,3));
               DynmapCalls.addCapturePointMarker(newCap);
               
               if(finalCount % 10 == 0){
                  src.sendMessage(Text.literal("Generating..."));
               }
            }));
            
            listStr.append("(").append(chunkPos.x).append(",").append(chunkPos.z).append("), ");
            count++;
         }
         listStr.replace(listStr.length()-2,listStr.length(),"]");
         
         src.sendMessage(Text.literal("Calculated "+points.size()+" points within radius "+border+" with minDist "+minDist).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD,listStr.toString()))));
         
         Nations.addTickTimerCallback(world, new GenericTimer(20*count, () -> {
            nationsData.initializeWorld(initializingPoints,chunks);
            src.sendMessage(Text.literal("Completed world initialization!"));
            
            HashMap<ResourceType,Integer> map = new HashMap<>();
            for(CapturePoint capturePoint : initializingPoints){
               if(map.containsKey(capturePoint.getType())){
                  map.put(capturePoint.getType(),map.get(capturePoint.getType())+1);
               }else{
                  map.put(capturePoint.getType(),1);
               }
            }
            map.forEach((type, typeCount) -> {
               src.sendMessage(Text.literal("Generated "+typeCount+" "+type.asString()+" Capture Points"));
            });
            initializing = false;
         }));
         
         DynmapCalls.addBorderMarker();
         DynmapCalls.addSpawnMarker();
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int giveVictoryPoints(CommandContext<ServerCommandSource> ctx, String nationId, int count){
      try{
         ServerCommandSource src = ctx.getSource();
         Nation nation = Nations.getNation(nationId);
         if(nation == null){
            src.sendError(Text.translatable("text.nations.no_nation_error"));
            return -1;
         }
         nation.addVictoryPoints(count);
         int vp = nation.getVictoryPoints();
         src.sendFeedback(() -> Text.translatable("text.nations.add_victory_points",nation.getName(),vp), true);
         return vp;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int nationSurvey(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         Vec3d pos = src.getPosition();
         if(!Nations.isWorldInitialized()){
            src.sendError(Text.translatable("text.nations.world_not_initialized"));
            return -1;
         }
         
         int radius = NationsConfig.getInt(NationsRegistry.SETTLE_RADIUS_CFG);
         int border = NationsConfig.getInt(NationsRegistry.WORLD_BORDER_RADIUS_OVERWORLD_CFG);
         ChunkPos surveyPos = new ChunkPos(BlockPos.ofFloored(pos));
         Triple<Integer,Integer,Integer> values = NationsUtils.calculateChunkCoinGeneration(ctx.getSource().getWorld(),pos,radius);
         Pair<Triple<Integer,Integer,Integer>,Triple<Integer,Integer,Integer>> capValues = NationsUtils.calculateCapturePointYields(ctx.getSource().getWorld(),pos,radius);
         boolean beyondBorder = Math.abs(surveyPos.x) + radius > border || Math.abs(surveyPos.z) + radius > border;
         if(beyondBorder) src.sendMessage(Text.translatable("text.nations.survey_beyond_border").formatted(Formatting.RED));
         
         block:{
            for (int dx = -radius; dx <= radius; dx++) {
               for (int dz = -radius; dz <= radius; dz++) {
                  if (Math.abs(dx) + Math.abs(dz) <= radius) {
                     ChunkPos chunkPos2 = new ChunkPos(surveyPos.x + dx, surveyPos.z + dz);
                     if(NationsLand.isSpawnDMZChunk(chunkPos2.getStartPos())){
                        src.sendMessage(Text.translatable("text.nations.cannot_settle_near_spawn"));
                        break block;
                     }
                  }
               }
            }
         }
         
         src.sendMessage(Text.translatable("text.nations.survey",radius,
               Text.literal(values.getLeft()+" ").append(ResourceType.GROWTH.getText()).formatted(Formatting.GREEN),
               Text.literal(values.getMiddle()+" ").append(ResourceType.MATERIAL.getText()).formatted(Formatting.GOLD),
               Text.literal(values.getRight()+" ").append(ResourceType.RESEARCH.getText()).formatted(Formatting.AQUA)));
         
         Triple<Integer,Integer,Integer> capCounts = capValues.getLeft();
         Triple<Integer,Integer,Integer> capYields = capValues.getRight();
         src.sendMessage(Text.translatable("text.nations.cap_survey",radius,
               Text.literal(capYields.getLeft()+" ").append(ResourceType.GROWTH.getText()).append(Text.literal(" ("+capCounts.getLeft()+")")).formatted(Formatting.GREEN),
               Text.literal(capYields.getMiddle()+" ").append(ResourceType.MATERIAL.getText()).append(Text.literal(" ("+capCounts.getMiddle()+")")).formatted(Formatting.GOLD),
               Text.literal(capYields.getRight()+" ").append(ResourceType.RESEARCH.getText()).append(Text.literal(" ("+capCounts.getRight()+")")).formatted(Formatting.AQUA)));
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int nationSettle(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         if(!src.isExecutedByPlayer()){
            src.sendError(Text.translatable("text.nations.not_player"));
            return -1;
         }
         if(!Nations.isWorldInitialized() || !src.getWorld().getRegistryKey().equals(ServerWorld.OVERWORLD)){
            src.sendError(Text.translatable("text.nations.world_not_initialized"));
            return -1;
         }
         ServerPlayerEntity player = src.getPlayer();
         Nation nation = Nations.getNation(player);
         if(nation == null){
            src.sendError(Text.translatable("text.nations.no_player_nation_error"));
            return -1;
         }
         
         if(nation.isFounded()){
            src.sendError(Text.translatable("text.nations.nation_founded_error"));
            return -1;
         }
         ChunkPos chunkPos = new ChunkPos(player.getBlockPos());
         if(Nations.getCapturePoint(chunkPos) != null){
            src.sendError(Text.translatable("text.nations.cannot_settle_on_cap"));
            return -1;
         }
         NationChunk nChunk = Nations.getChunk(chunkPos);
         if(nChunk.getControllingNation() != null){
            src.sendError(Text.translatable("text.nations.cannot_settle_in_nation"));
            return -1;
         }
         if(!nation.hasPermissions(player)){
            src.sendError(Text.translatable("text.nations.player_not_executor"));
            return -1;
         }
         
         int settleRadius = NationsConfig.getInt(NationsRegistry.SETTLE_RADIUS_CFG);
         
         for (int dx = -settleRadius; dx <= settleRadius; dx++) {
            for (int dz = -settleRadius; dz <= settleRadius; dz++) {
               if (Math.abs(dx) + Math.abs(dz) <= settleRadius) {
                  ChunkPos chunkPos2 = new ChunkPos(chunkPos.x + dx, chunkPos.z + dz);
                  if(NationsLand.isSpawnDMZChunk(chunkPos2.getStartPos())){
                     src.sendError(Text.translatable("text.nations.cannot_settle_near_spawn"));
                     return -1;
                  }
               }
            }
         }
         
         if(SETTLE_WARNING.containsKey(player) && SETTLE_WARNING.get(player).equals(chunkPos)){
            nation.settleNation(player.getServerWorld(),chunkPos);
            SETTLE_WARNING.remove(player);
         }else{
            src.sendMessage(Text.translatable("text.nations.settle_warning").formatted(Formatting.GOLD));
            SETTLE_WARNING.put(player,chunkPos);
            nationSurvey(ctx);
         }
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int refresh(CommandContext<ServerCommandSource> ctx){
      try{
         CONFIG.read();
         CONFIG.save();
         SHOP.read();
         SHOP.save();
         ctx.getSource().sendMessage(Text.translatable("text.nations.reload_config"));
         
         Nations.refreshNationChunkOwnership();
         Nations.refreshWorldBorders();
         DynmapCalls.redrawDynmap();
         
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int deleteNation(CommandContext<ServerCommandSource> ctx, String nationId){
      try{
         ServerCommandSource src = ctx.getSource();
         Nation nation = Nations.getNation(nationId);
         if(nation == null){
            src.sendError(Text.translatable("text.nations.no_nation_error"));
            return -1;
         }
         INationsDataComponent nationsData = NATIONS_DATA.get(src.getServer().getOverworld());
         
         for(NationChunk chunk : nationsData.getChunks()){
            if(chunk.getControllingNation() != null && chunk.getControllingNation().equals(nation)){
               chunk.reset();
            }
         }
         
         for(CapturePoint capturePoint : getCapturePoints()){
            if(capturePoint.getControllingNation().equals(nation)){
               capturePoint.setControllingNationId(null);
            }
         }
         
         for(UUID member : nation.getMembers()){
            MinecraftServer server = ctx.getSource().getServer();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(member);
            if(player == null){
               UserCache userCache = server.getUserCache();
               userCache.load();
               Optional<GameProfile> profile = userCache.getByUuid(member);
               if(profile.isPresent()){
                  player = server.getPlayerManager().createPlayer(profile.get(), SyncedClientOptions.createDefault());
                  server.getPlayerManager().loadPlayerData(player);
               }
            }
            if(player != null){
               PLAYER_DATA.get(player).setNation(null);
               PLAYER_DATA.get(player).setChannel(ChatChannel.GLOBAL);
            }
         }
         
         String name = nation.getName();
         nationsData.removeNation(nation.getId());
         DynmapCalls.redrawDynmap();
         
         src.sendFeedback(() -> Text.translatable("text.nations.nation_deleted",name,nationId), true);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int updateColors(CommandContext<ServerCommandSource> ctx, String nationId, String textColorMain, String textColorSub, String dyeColor){
      try{
         ServerCommandSource src = ctx.getSource();
         Nation nation = Nations.getNation(nationId);
         if(nation == null){
            src.sendError(Text.translatable("text.nations.no_nation_error"));
            return -1;
         }
         
         int parsedColorMain,parsedColorSub;
         try{
            if(textColorMain.startsWith("0x")){
               textColorMain = textColorMain.substring(2);
            }else if(textColorMain.startsWith("#")){
               textColorMain = textColorMain.substring(1);
            }
            if(textColorSub.startsWith("0x")){
               textColorSub = textColorSub.substring(2);
            }else if(textColorSub.startsWith("#")){
               textColorSub = textColorSub.substring(1);
            }
            
            
            if (textColorMain.matches("[0-9A-Fa-f]{6}")) {
               parsedColorMain = Integer.parseInt(textColorMain, 16);
            } else {
               parsedColorMain = Integer.parseInt(textColorMain);
            }
            if (textColorSub.matches("[0-9A-Fa-f]{6}")) {
               parsedColorSub = Integer.parseInt(textColorSub, 16);
            } else {
               parsedColorSub = Integer.parseInt(textColorSub);
            }
         }catch(Exception e){
            src.sendError(Text.translatable("text.nations.color_error"));
            return -1;
         }
         
         nation.changeColors(parsedColorMain,parsedColorSub,DyeColor.byName(dyeColor,DyeColor.WHITE));
         src.sendFeedback(() -> Text.translatable("text.nations.updated_colors",nationId), true);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int changeName(CommandContext<ServerCommandSource> ctx, String nationId, String nationName){
      try{
         ServerCommandSource src = ctx.getSource();
         Nation nation = Nations.getNation(nationId);
         if(nation == null){
            src.sendError(Text.translatable("text.nations.no_nation_error"));
            return -1;
         }
         
         nation.changeName(nationName);
         src.sendFeedback(() -> Text.translatable("text.nations.updated_name",nationId), true);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int createNation(CommandContext<ServerCommandSource> ctx, String nationId, String nationName, String textColorMain, String textColorSub, String dyeColor){
      try{
         List<Nation> nations = Nations.getNations();
         ServerCommandSource src = ctx.getSource();
         if(!Nations.isWorldInitialized()){
            src.sendError(Text.translatable("text.nations.world_not_initialized"));
            return -1;
         }
         
         if(nationId.equals("all") || nationId.isBlank() || nations.stream().anyMatch(n -> n.getId().equals(nationId) || n.getName().equals(nationName))){
            src.sendError(Text.translatable("text.nations.nation_creation_error"));
            return -1;
         }
         
         int parsedColorMain,parsedColorSub;
         try{
            if(textColorMain.startsWith("0x")){
               textColorMain = textColorMain.substring(2);
            }else if(textColorMain.startsWith("#")){
               textColorMain = textColorMain.substring(1);
            }
            if(textColorSub.startsWith("0x")){
               textColorSub = textColorSub.substring(2);
            }else if(textColorSub.startsWith("#")){
               textColorSub = textColorSub.substring(1);
            }
            
            
            if (textColorMain.matches("[0-9A-Fa-f]{6}")) {
               parsedColorMain = Integer.parseInt(textColorMain, 16);
            } else {
               parsedColorMain = Integer.parseInt(textColorMain);
            }
            if (textColorSub.matches("[0-9A-Fa-f]{6}")) {
               parsedColorSub = Integer.parseInt(textColorSub, 16);
            } else {
               parsedColorSub = Integer.parseInt(textColorSub);
            }
         }catch(Exception e){
            src.sendError(Text.translatable("text.nations.color_error"));
            return -1;
         }
         
         Nation newNation = new Nation(nationId, nationName);
         newNation.setTextColor(parsedColorMain);
         newNation.setTextColorSub(parsedColorSub);
         newNation.setDyeColor(DyeColor.byName(dyeColor,DyeColor.WHITE));
         INationsDataComponent nationsData = NATIONS_DATA.get(src.getServer().getOverworld());
         nationsData.addNation(newNation);
         src.sendFeedback(() -> Text.translatable("text.nations.nation_created",nationName,nationId), true);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int joinNation(CommandContext<ServerCommandSource> ctx, String nationId, ServerPlayerEntity player){
      try{
         ServerCommandSource src = ctx.getSource();
         Nation nation = Nations.getNation(nationId);
         if(nation == null){
            src.sendError(Text.translatable("text.nations.no_nation_error"));
            return -1;
         }
         if(player == null){
            src.sendError(Text.translatable("text.nations.not_player"));
            return -1;
         }
         Nations.getPlayer(player).setNation(nation);
         nation.addPlayer(player);
         src.sendFeedback(() -> Text.translatable("text.nations.join_nation"), false);
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int leaveNation(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player){
      try{
         ServerCommandSource src = ctx.getSource();
         Nation nation = Nations.getNation(player);
         if(nation == null){
            src.sendError(Text.translatable("text.nations.no_nation_error"));
            return -1;
         }
         
         nation.removePlayer(player);
         src.sendFeedback(() -> Text.translatable("text.nations.leave_nation"), false);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int startResearch(CommandContext<ServerCommandSource> ctx, String researchId){
      try{
         ServerCommandSource src = ctx.getSource();
         if(!src.isExecutedByPlayer()){
            src.sendError(Text.translatable("text.nations.not_player"));
            return -1;
         }
         ServerPlayerEntity player = src.getPlayer();
         Nation nation = Nations.getNation(player);
         if(nation == null){
            src.sendError(Text.translatable("text.nations.no_player_nation_error"));
            return -1;
         }
         
         ResearchTech tech = NationsRegistry.RESEARCH.get(Identifier.of(MOD_ID,researchId));
         if(tech == null){
            src.sendError(Text.translatable("text.nations.invalid_tech"));
            return -1;
         }
         
         int maxTier = NationsConfig.getInt(NationsRegistry.RESEARCH_TIER_CFG);
         if(maxTier < tech.getTier()){
            src.sendError(Text.translatable("text.nations.tech_not_available"));
            return -1;
         }
         
         List<ResearchTech> missingPrereqs = tech.missingPrereqs(nation.getCompletedTechs());
         if(!missingPrereqs.isEmpty()){
            src.sendError(Text.translatable("text.nations.missing_tech_prereqs"));
            for(ResearchTech missingPrereq : missingPrereqs){
               src.sendError(Text.literal(" - ").append(missingPrereq.getName()));
            }
            return -1;
         }
         
         nation.setActiveTech(tech);
         src.sendFeedback(() -> Text.translatable("text.nations.set_tech",tech.getName()), false);
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int cancelResearch(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         if(!src.isExecutedByPlayer()){
            src.sendError(Text.translatable("text.nations.not_player"));
            return -1;
         }
         ServerPlayerEntity player = src.getPlayer();
         Nation nation = Nations.getNation(player);
         if(nation == null){
            src.sendError(Text.translatable("text.nations.no_player_nation_error"));
            return -1;
         }
         
         nation.setActiveTech(null);
         src.sendFeedback(() -> Text.translatable("text.nations.cancelled_research"), false);
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int openChunkMenu(CommandContext<ServerCommandSource> ctx, ChunkSectionPos pos){
      try{
         ServerCommandSource src = ctx.getSource();
         if(!src.isExecutedByPlayer()){
            src.sendError(Text.translatable("text.nations.not_player"));
            return -1;
         }
         if(!Nations.isWorldInitialized()){
            src.sendError(Text.translatable("text.nations.world_not_initialized"));
            return -1;
         }
         ServerPlayerEntity player = src.getPlayer();
         ChunkPos chunkPos = pos.toChunkPos();
         NationChunk chunk = Nations.getChunk(chunkPos);
         if(chunk == null){
            src.sendError(Text.translatable("text.nations.chunk_out_of_bounds"));
            return -1;
         }
         
         ChunkGui chunkGui = new ChunkGui(player,chunkPos);
         chunkGui.open();
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int changeChatChannel(CommandContext<ServerCommandSource> ctx, ChatChannel channel){
      try{
         ServerCommandSource src = ctx.getSource();
         if(!src.isExecutedByPlayer()){
            src.sendError(Text.translatable("text.nations.not_player"));
            return -1;
         }
         ServerPlayerEntity player = src.getPlayer();
         Nation nation = Nations.getNation(player);
         if(channel == ChatChannel.NATION && nation == null){
            src.sendError(Text.translatable("text.nations.no_player_nation_error"));
            return -1;
         }
         INationsProfileComponent data = Nations.getPlayer(player);
         if(data.getChannel() == channel) return 0;
         data.setChannel(channel);
         src.sendMessage(Text.translatable("text.nations.set_channel",Text.literal(channel.asString().toUpperCase(Locale.ROOT)).formatted(channel.getColor())));
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int changePerms(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, boolean promote){
      try{
         ServerCommandSource src = ctx.getSource();
         Nation nation = Nations.getNation(player);
         if(nation == null){
            src.sendError(Text.translatable("text.nations.other_no_player_nation_error"));
            return -1;
         }
         
         boolean isLeader = nation.isLeader(player);
         boolean isExecutor = nation.isExecutor(player);
         if(promote){
            if(isExecutor){
               if(src.hasPermissionLevel(2)){
                  nation.promote(player);
                  src.sendMessage(Text.translatable("text.nations.promoted_leader",player.getStyledDisplayName()));
               }else{
                  src.sendError(Text.translatable("text.nations.not_operator"));
                  return -1;
               }
            }else if(isLeader){
               src.sendError(Text.translatable("text.nations.already_leader"));
               return -1;
            }else{
               nation.promote(player);
               src.sendMessage(Text.translatable("text.nations.promoted_executor",player.getStyledDisplayName()));
            }
         }else{
            if(isLeader){
               if(src.hasPermissionLevel(2)){
                  nation.demote(player);
                  src.sendMessage(Text.translatable("text.nations.demoted_leader",player.getStyledDisplayName()));
               }else{
                  src.sendError(Text.translatable("text.nations.not_operator"));
                  return -1;
               }
            }else if(isExecutor){
               nation.demote(player);
               src.sendMessage(Text.translatable("text.nations.demoted_executor",player.getStyledDisplayName()));
            }else{
               src.sendError(Text.translatable("text.nations.already_member"));
               return -1;
            }
         }
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int adminResearch(CommandContext<ServerCommandSource> ctx, String nationId, String researchId, boolean grant, boolean removePostReqs){
      try{
         ServerCommandSource src = ctx.getSource();
         Nation nation = Nations.getNation(nationId);
         if(nation == null){
            src.sendError(Text.translatable("text.nations.no_nation_error"));
            return -1;
         }
         ResearchTech tech = NationsRegistry.RESEARCH.get(Identifier.of(MOD_ID,researchId));
         if(tech == null){
            src.sendError(Text.translatable("text.nations.invalid_tech"));
            return -1;
         }
         int maxTier = NationsConfig.getInt(NationsRegistry.RESEARCH_TIER_CFG);
         if(maxTier < tech.getTier()){
            src.sendError(Text.translatable("text.nations.tech_not_available"));
            return -1;
         }
         
         if(grant){
            boolean added = nation.addTechAndPrereqs(tech);
            if(added){
               src.sendFeedback(() -> Text.translatable("text.nations.added_tech", tech.getName(), nation.getFormattedName()), true);
               return 1;
            }else{
               src.sendFeedback(() -> Text.translatable("text.nations.already_researched", nation.getFormattedName(), tech.getName()), false);
               return 0;
            }
         }else{
            nation.removeTech(tech,removePostReqs);
            src.sendFeedback(() -> Text.translatable("text.nations.removed_tech", tech.getName(), nation.getFormattedName()), true);
            return 1;
         }
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int chunkInfo(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         if(!src.isExecutedByPlayer()){
            src.sendError(Text.translatable("text.nations.not_player"));
            return -1;
         }
         ServerPlayerEntity player = src.getPlayer();
         ChunkPos pos = player.getChunkPos();
         NationChunk nChunk = Nations.getChunk(pos);
         if(nChunk == null || !src.getWorld().getRegistryKey().equals(ServerWorld.OVERWORLD)){
            src.sendError(Text.translatable("text.nations.invalid_chunk"));
            return -1;
         }
         
         if(nChunk.getControllingNation() == null){
            src.sendMessage(Text.translatable("text.nations.chunk_uncontrolled"));
         }else{
            if(nChunk.isClaimed()){
               src.sendMessage(Text.translatable("text.nations.chunk_claimed_by", nChunk.getControllingNation().getFormattedName()));
            }else if(nChunk.isInfluenced()){
               src.sendMessage(Text.translatable("text.nations.chunk_influenced_by", nChunk.getControllingNation().getFormattedName()));
            }
         }
         
         Triple<Integer,Integer,Integer> values = NationsUtils.calculateChunkCoinGeneration(player.getServerWorld(),player.getPos(),0);
         src.sendMessage(Text.translatable("text.nations.survey",0,
               Text.literal(values.getLeft()+" ").append(ResourceType.GROWTH.getText()).formatted(Formatting.GREEN),
               Text.literal(values.getMiddle()+" ").append(ResourceType.MATERIAL.getText()).formatted(Formatting.GOLD),
               Text.literal(values.getRight()+" ").append(ResourceType.RESEARCH.getText()).formatted(Formatting.AQUA)));
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int nickPlayer(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, String nickname){
      try{
         ServerCommandSource src = ctx.getSource();
         INationsProfileComponent data = Nations.getPlayer(player);
         
         if(nickname.isEmpty()){
            data.setNickname("");
            src.sendMessage(Text.translatable("text.nations.reset_nickname",player.getStyledDisplayName()));
         }else{
            String regex = "^[A-Za-z0-9_]{1,32}$";
            if(nickname.matches(regex)){
               data.setNickname(nickname);
               src.sendMessage(Text.translatable("text.nations.set_nickname",player.getStyledDisplayName()));
            }else{
               src.sendError(Text.translatable("text.nations.invalid_nickname"));
               return -1;
            }
         }
         
         for(ServerPlayerEntity p : src.getServer().getPlayerManager().getPlayerList()){
            p.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(src.getServer().getPlayerManager().getPlayerList()));
         }
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int bypassClaims(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         if(!src.isExecutedByPlayer()){
            src.sendError(Text.translatable("text.nations.not_player"));
            return -1;
         }
         ServerPlayerEntity player = src.getPlayer();
         INationsProfileComponent data = Nations.getPlayer(player);
         data.setClaimBypass(!data.bypassesClaims());
         src.sendMessage(Text.translatable("text.nations.bypass_claims",String.valueOf(data.bypassesClaims())));
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
      
   }
   
   public static int forceWar(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         INationsDataComponent data = NATIONS_DATA.get(SERVER.getOverworld());
         data.setNextWar(System.currentTimeMillis());
         src.sendMessage(Text.translatable("text.nations.forced_war"));
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int forceRift(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         INationsDataComponent data = NATIONS_DATA.get(SERVER.getOverworld());
         data.setNextRift(System.currentTimeMillis());
         src.sendMessage(Text.translatable("text.nations.forced_rift"));
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int cancelRift(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         if(LAST_RIFT != null && LAST_RIFT.isActive()){
            LAST_RIFT.forceClose();
         }
         src.sendMessage(Text.translatable("text.nations.closed_rift"));
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int forceHourlyTick(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         if(!Nations.isWorldInitialized()){
            src.sendError(Text.translatable("text.nations.world_not_initialized"));
            return -1;
         }
         TimedEvents.doHourlyTick(src.getServer());
         src.sendMessage(Text.translatable("text.nations.forced_hourly_tick"));
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int forceDailyTick(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         if(!Nations.isWorldInitialized()){
            src.sendError(Text.translatable("text.nations.world_not_initialized"));
            return -1;
         }
         TimedEvents.doDailyTick(src.getServer());
         src.sendMessage(Text.translatable("text.nations.forced_daily_tick"));
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int addShopItem(CommandContext<ServerCommandSource> ctx, int price){
      try{
         ServerCommandSource src = ctx.getSource();
         if(!src.isExecutedByPlayer()){
            src.sendError(Text.translatable("text.nations.not_player"));
            return -1;
         }
         ServerPlayerEntity player = src.getPlayer();
         ItemStack mainhand = player.getMainHandStack();
         ItemStack offhand = player.getOffHandStack();
         if(mainhand.isEmpty() || offhand.isEmpty() || price < 1){
            src.sendError(Text.translatable("text.nations.invalid_items_or_price"));
            return -1;
         }
         
         SHOP.getOffers().add(new Pair<>(mainhand.copy(), new Pair<>(offhand.getItem(),price)));
         SHOP.save();
         src.sendMessage(Text.translatable("text.nations.shop_entry_added"));
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int sendMail(CommandContext<ServerCommandSource> ctx, String nationId){
      try{
         ServerCommandSource src = ctx.getSource();
         if(!src.isExecutedByPlayer()){
            src.sendError(Text.translatable("text.nations.not_player"));
            return -1;
         }
         ServerPlayerEntity player = src.getPlayer();
         ItemStack mainhand = player.getMainHandStack();
         if(mainhand.isEmpty()){
            src.sendError(Text.translatable("text.nations.mainhand_empty"));
            return -1;
         }
         
         if(nationId.equals("all")){
            for(Nation nation : getNations()){
               nation.addMail(mainhand.copy());
            }
         }else{
            Nation nation = Nations.getNation(nationId);
            if(nation == null){
               src.sendError(Text.translatable("text.nations.no_nation_error"));
               return -1;
            }
            nation.addMail(mainhand.copy());
         }
         
         src.sendMessage(Text.translatable("text.nations.mail_sent"));
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int announce(CommandContext<ServerCommandSource> ctx, String message){
      try{
         ServerCommandSource src = ctx.getSource();
         Nations.announce(Text.empty()
               .append(Text.literal("!!! ").formatted(Formatting.BOLD,Formatting.DARK_RED))
               .append(Text.literal(message).formatted(Formatting.BOLD,Formatting.RED))
               .append(Text.literal(" !!!").formatted(Formatting.BOLD,Formatting.DARK_RED)));
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int researchStatus(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         if(!src.isExecutedByPlayer()){
            src.sendError(Text.translatable("text.nations.not_player"));
            return -1;
         }
         ServerPlayerEntity player = src.getPlayer();
         Nation nation = Nations.getNation(player);
         if(nation == null){
            src.sendError(Text.translatable("text.nations.no_player_nation_error"));
            return -1;
         }
         
         ResearchTech activeTech = nation.getActiveTech();
         MutableText researchText = activeTech == null ? Text.translatable("text.nations.nothing") : activeTech.getName();
         float percentage = activeTech == null ? 0 : 100*((float) nation.getProgress(activeTech) / activeTech.getCost());
         src.sendMessage(Text.translatable("text.nations.currently_researching",
               researchText.formatted(Formatting.LIGHT_PURPLE),
               Text.literal(String.format("%03.2f",percentage)).formatted(Formatting.AQUA,Formatting.BOLD)
         ).formatted(Formatting.DARK_AQUA));
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int leaderboard(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         
         ArrayList<Nation> order = new ArrayList<>(getNations());
         order.sort(Comparator.comparingInt(n -> -n.getVictoryPoints()));
         for(Nation nation : order){
            src.sendMessage(Text.translatable("text.nations.victory_point_readout",
                  nation.getFormattedNameTag(false),
                  Text.literal(String.format("%,d",nation.getVictoryPoints())).formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD),
                  Text.translatable("text.nations.victory_points").formatted(Formatting.DARK_PURPLE)
            ).formatted(Formatting.DARK_PURPLE));
         }
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int setNextWar(CommandContext<ServerCommandSource> ctx, long time){
      try{
         ServerCommandSource src = ctx.getSource();
         Instant instant = Instant.ofEpochMilli(time);
         ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
         DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
         
         INationsDataComponent data = NATIONS_DATA.get(src.getServer().getOverworld());
         data.setNextWar(time);
         src.sendMessage(Text.translatable("text.nations.set_next_war",zdt.format(fmt)));
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int getNextWar(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         INationsDataComponent data = NATIONS_DATA.get(src.getServer().getOverworld());
         Instant instant = Instant.ofEpochMilli(data.getNextWar());
         ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
         DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
         src.sendMessage(Text.translatable("text.nations.get_next_war",zdt.format(fmt)));
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int toggleTrespassAlerts(CommandContext<ServerCommandSource> ctx){
      try{
         ServerCommandSource src = ctx.getSource();
         if(!src.isExecutedByPlayer()){
            src.sendError(Text.translatable("text.nations.not_player"));
            return -1;
         }
         ServerPlayerEntity player = src.getPlayer();
         Nation nation = Nations.getNation(player);
         if(nation == null){
            src.sendError(Text.translatable("text.nations.no_player_nation_error"));
            return -1;
         }
         
         INationsProfileComponent profile = Nations.getPlayer(player);
         profile.toggleTrespassAlerts();
         src.sendMessage(Text.translatable("text.nations.toggled_trespass_alerts",String.valueOf(profile.trespassAlerts())));
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int transferCapturePoint(CommandContext<ServerCommandSource> ctx, String nationId, ChunkSectionPos chunkPos){
      try{
         ServerCommandSource src = ctx.getSource();
         CapturePoint cap = Nations.getCapturePoint(chunkPos.toChunkPos());
         
         if(cap == null){
            src.sendError(Text.translatable("text.nations.no_cap_error"));
            return -1;
         }
         
         Nation nation = Nations.getNation(nationId);
         if(nation == null){
            src.sendError(Text.translatable("text.nations.no_nation_error"));
            return -1;
         }
         
         cap.transferOwnership(src.getServer().getOverworld(),nation);
         MutableText announcement = Text.translatable("text.nations.cap_transfer",
               cap.getType().getText().formatted(Formatting.BOLD),
               Text.translatable("text.nations.capture_point").formatted(Formatting.BOLD,cap.getType().getTextColor()),
               Text.literal(cap.getChunkPos().toString()).formatted(Formatting.YELLOW,Formatting.BOLD),
               nation.getFormattedName().formatted(Formatting.BOLD)
         ).formatted(Formatting.DARK_AQUA);
         Nations.announce(announcement);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
}
