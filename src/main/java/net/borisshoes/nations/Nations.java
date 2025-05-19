package net.borisshoes.nations;

import com.mojang.authlib.GameProfile;
import net.borisshoes.nations.callbacks.*;
import net.borisshoes.nations.cca.INationsDataComponent;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.borisshoes.nations.gameplay.*;
import net.borisshoes.nations.integration.DynmapCalls;
import net.borisshoes.nations.integration.DynmapFunctions;
import net.borisshoes.nations.land.InteractionEvents;
import net.borisshoes.nations.utils.ConfigUtils;
import net.borisshoes.nations.utils.ShopUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.mixin.item.CraftingRecipeMixin;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.CrafterBlock;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameRules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static net.borisshoes.nations.cca.PlayerComponentInitializer.PLAYER_DATA;
import static net.borisshoes.nations.cca.WorldDataComponentInitializer.NATIONS_DATA;

public class Nations implements ModInitializer {
   
   private static final Logger LOGGER = LogManager.getLogger("Nations");
   public static final ArrayList<TickTimerCallback> SERVER_TIMER_CALLBACKS = new ArrayList<>();
   public static final ArrayList<Pair<ServerWorld,TickTimerCallback>> WORLD_TIMER_CALLBACKS = new ArrayList<>();
   
   public static MinecraftServer SERVER = null;
   public static final boolean DEV_MODE = true;
   private static final String CONFIG_NAME = "Nations.properties";
   private static final String SHOP_NAME = "NationsShop.txt";
   public static final String MOD_ID = "nations";
   public static final String BLANK_UUID = "00000000-0000-4000-8000-000000000000";
   public static final String JERALD_UUID = "5de15dee-0e50-4440-a19e-1a44da3f79dd";
   public static final Identifier EventPhase = Identifier.of(MOD_ID, "events");
   public static final HashMap<ResourceType, StructurePlacer.Structure> CAPTURE_POINT_STRUCTURES = new HashMap<>();
   public static StructurePlacer.Structure NATION_STRUCTURE;
   public static ConfigUtils CONFIG;
   public static ShopUtils SHOP;
   public static int DEBUG_VALUE = 0;
   public static NetherRift LAST_RIFT;
   
   @Override
   public void onInitialize(){
      CONFIG = new ConfigUtils(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_NAME).toFile(), LOGGER, NationsRegistry.CONFIG_SETTINGS.stream().map(NationsConfig.ConfigSetting::makeConfigValue).collect(Collectors.toCollection(HashSet::new)));
      NationsRegistry.initialize();
      
      CommandRegistrationCallback.EVENT.register(CommandRegisterCallback::registerCommands);
      ServerTickEvents.END_WORLD_TICK.register(WorldTickCallback::onWorldTick);
      ServerTickEvents.END_SERVER_TICK.register(TickCallback::onTick);
      PlayerBlockBreakEvents.BEFORE.register(InteractionEvents::breakBlocks);
      AttackBlockCallback.EVENT.register(InteractionEvents::startBreakBlocks);
      UseBlockCallback.EVENT.addPhaseOrdering(EventPhase, Event.DEFAULT_PHASE);
      UseBlockCallback.EVENT.register(EventPhase, InteractionEvents::useBlocks);
      UseEntityCallback.EVENT.register(((player, world, hand, entity, hitResult) -> {
         if (hitResult != null)
            return InteractionEvents.useAtEntity(player, world, hand, entity, null);
         return InteractionEvents.useEntity(player, world, hand, entity);
      }));
      AttackEntityCallback.EVENT.register(InteractionEvents::attackEntity);
      UseItemCallback.EVENT.register(InteractionEvents::useItem);
      ServerLifecycleEvents.SERVER_STARTING.register(Nations::serverStarting);
      ServerLifecycleEvents.SERVER_STARTED.register(Nations::serverStarted);
      ServerPlayConnectionEvents.JOIN.register(PlayerConnectionCallback::onPlayerJoin);
      ServerPlayConnectionEvents.DISCONNECT.register(PlayerConnectionCallback::onPlayerLeave);
      
      if (FabricLoader.getInstance().isModLoaded("dynmap")) DynmapFunctions.reg();
      
      LOGGER.info("Nations 2.0 Has Finally Arrived!");
   }
   
   public static void serverStarting(MinecraftServer server){
      Nations.SERVER = server;
      SHOP = new ShopUtils(FabricLoader.getInstance().getConfigDir().resolve(SHOP_NAME).toFile(),LOGGER,server);
      
      for(ResourceType value : ResourceType.values()){
         CAPTURE_POINT_STRUCTURES.put(value,StructurePlacer.loadFromFile(MOD_ID,value.name().toLowerCase(Locale.ROOT)+"_capture_point"));
      }
      Nations.NATION_STRUCTURE = StructurePlacer.loadFromFile(MOD_ID,"nation_center");
      
      for(Team team : server.getScoreboard().getTeams()){
         if(team.getName().contains("nations")){
            server.getScoreboard().removeTeam(team);
         }
      }
      
      NationsRegistry.withServer(server);
   }
   
   public static void serverStarted(MinecraftServer server){
      DynmapCalls.redrawDynmap();
      
      INationsDataComponent data = NATIONS_DATA.get(server.getOverworld());
      if(!data.getRiftData().isEmpty()){
         LAST_RIFT = NetherRift.fromNbt(data.getRiftData(),server.getOverworld(),server.getWorld(ServerWorld.NETHER));
      }
   }
   
   public static boolean isWorldInitialized(){
      return NATIONS_DATA.get(SERVER.getOverworld()).isWorldInitialized();
   }
   
   public static INationsProfileComponent getPlayerOrOffline(UUID playerId){
      MinecraftServer server = Nations.SERVER;
      ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
      if(player != null){
         return PLAYER_DATA.get(player);
      }else{
         GameProfile profile = server.getUserCache().getByUuid(playerId).orElse(null);
         if(profile == null) return null;
         player = server.getPlayerManager().createPlayer(profile, SyncedClientOptions.createDefault());
         server.getPlayerManager().loadPlayerData(player);
         return PLAYER_DATA.get(player);
      }
   }
   
   public static INationsProfileComponent getPlayer(ServerPlayerEntity player){
      return PLAYER_DATA.get(player);
   }
   
   public static List<Nation> getNations(){
      return NATIONS_DATA.get(SERVER.getOverworld()).getNations();
   }
   
   public static Nation getNation(String id){
      return NATIONS_DATA.get(SERVER.getOverworld()).getNation(id);
   }
   
   public static Nation getNation(ServerPlayerEntity player){
      return PLAYER_DATA.get(player).getNation();
   }
   
   public static List<CapturePoint> getCapturePoints(){
      return NATIONS_DATA.get(SERVER.getOverworld()).getCapturePoints();
   }
   
   public static CapturePoint getCapturePoint(String id){
      return NATIONS_DATA.get(SERVER.getOverworld()).getCapturePoint(id);
   }
   
   public static CapturePoint getCapturePoint(ChunkPos pos){
      return NATIONS_DATA.get(SERVER.getOverworld()).getCapturePoint(pos);
   }
   
   public static NationChunk getChunk(int x, int z){
      return getChunk(new ChunkPos(x,z));
   }
   
   public static NationChunk getChunk(ChunkPos pos){
      return NATIONS_DATA.get(SERVER.getOverworld()).getChunk(pos);
   }
   
   public static List<NationChunk> getChunks(){
      return NATIONS_DATA.get(SERVER.getOverworld()).getChunks();
   }
   
   public static String isClaimed(ChunkPos pos){
      NationChunk chunk = getChunk(pos);
      if(chunk == null || !chunk.isClaimed()) return "";
      return chunk.getControllingNation().getId();
   }
   
   public static String isInfluenced(ChunkPos pos){
      NationChunk chunk = getChunk(pos);
      if(chunk == null || !chunk.isInfluenced()) return "";
      return chunk.getControllingNation().getId();
   }
   
   public static boolean isClaimedAgainst(ChunkPos pos, ServerPlayerEntity player){
      NationChunk chunk = getChunk(pos);
      if(chunk == null || !chunk.isClaimed()) return false;
      Nation controllingNation = chunk.getControllingNation();
      if(controllingNation == null) return false;
      Nation playerNation = getNation(player);
      if(playerNation == null) return true;
      return !controllingNation.equals(playerNation);
   }
   
   public static boolean isInfluencedAgainst(ChunkPos pos, ServerPlayerEntity player){
      NationChunk chunk = getChunk(pos);
      if(chunk == null || !chunk.isInfluenced()) return false;
      Nation controllingNation = chunk.getControllingNation();
      if(controllingNation == null) return false;
      Nation playerNation = getNation(player);
      if(playerNation == null) return true;
      return !controllingNation.equals(playerNation);
   }
   
   public static boolean isChunkAnchored(ChunkPos pos){
      NationChunk chunk = getChunk(pos);
      if(chunk == null) return false;
      return chunk.isAnchored();
   }
   
   public static boolean isWartime(){
      return WarManager.isWarActive();
   }
   
   public static void announce(MutableText text){
      Nations.SERVER.getPlayerManager().broadcast(text,false);
   }
   
   public static void refreshNationChunkOwnership(){
      for(NationChunk chunk : NATIONS_DATA.get(SERVER.getOverworld()).getChunks()){
         if(chunk.getControllingNation() != null){
            chunk.setControllingNationId(chunk.getControllingNation().getId());
            chunk.setInfluenced(true);
         }else{
            chunk.reset();
         }
      }
      for(Nation nation : getNations()){
         nation.getChunks().removeIf(chunk -> chunk.getControllingNation() == null || !chunk.getControllingNation().equals(nation));
      }
   }
   
   public static boolean addTickTimerCallback(TickTimerCallback callback){
      return SERVER_TIMER_CALLBACKS.add(callback);
   }
   
   public static boolean addTickTimerCallback(ServerWorld world, TickTimerCallback callback){
      return WORLD_TIMER_CALLBACKS.add(new Pair<>(world,callback));
   }
   
   public static void devPrint(String msg){
      if(DEV_MODE){
         System.out.println(msg);
      }
   }
   
   /**
    * Uses built in logger to log a message
    * @param level 0 - Info | 1 - Warn | 2 - Error | 3 - Fatal | Else - Debug
    * @param msg  The {@code String} to be printed.
    */
   public static void log(int level, String msg){
      switch(level){
         case 0 -> LOGGER.info(msg);
         case 1 -> LOGGER.warn(msg);
         case 2 -> LOGGER.error(msg);
         case 3 -> LOGGER.fatal(msg);
         default -> LOGGER.debug(msg);
      }
   }
}
