package net.borisshoes.nations.gameplay;

import net.borisshoes.arcananovum.ArcanaRegistry;
import net.borisshoes.arcananovum.damage.ArcanaDamageTypes;
import net.borisshoes.arcananovum.utils.SpawnPile;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.utils.GenericTimer;
import net.borisshoes.nations.utils.ParticleEffectUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

import java.util.*;
import java.util.stream.Collectors;

public class WarManager {
   
   public static final int DUEL_RANGE = 2;
   private static final HashSet<Contest> ACTIVE_CONTESTS = new HashSet<>();
   private static final HashMap<CapturePoint,ServerPlayerEntity> PENDING_CONTESTS = new HashMap<>();
   private static final HashSet<CapturePoint> COMPLETED_CAPS = new HashSet<>();
   
   private static long warStart = 0;
   private static boolean warActive = false;
   
   public static NbtCompound saveWarData(NbtCompound compound){
      NbtList completedCapList = new NbtList();
      NbtCompound pendingContests = new NbtCompound();
      completedCapList.addAll(COMPLETED_CAPS.stream().map(cap -> NbtString.of(cap.getId().toString())).collect(Collectors.toSet()));
      PENDING_CONTESTS.forEach((cap, player) -> pendingContests.putString(cap.getId().toString(),player.getUuidAsString()));
      
      compound.putLong("warStart",warStart);
      compound.putBoolean("warActive",warActive);
      compound.put("completedCaps",completedCapList);
      compound.put("pendingContests",pendingContests);
      return compound;
   }
   
   public static void loadWarData(NbtCompound compound){
      PENDING_CONTESTS.clear();
      COMPLETED_CAPS.clear();
      NbtList completedCapList = compound.getList("completedCaps", NbtElement.STRING_TYPE);
      NbtCompound pendingContests = compound.getCompound("pendingContests");
      for(String key : pendingContests.getKeys()){
         ServerPlayerEntity player = Nations.SERVER.getPlayerManager().getPlayer(UUID.fromString(pendingContests.getString(key)));
         if(player != null) PENDING_CONTESTS.put(Nations.getCapturePoint(key),player);
      }
      for(NbtElement e : completedCapList){
         COMPLETED_CAPS.add(Nations.getCapturePoint(e.asString()));
      }
      
      warStart = compound.getLong("warStart");
      warActive = compound.getBoolean("warActive");
   }
   
   public static void startWar(){
      warStart = System.currentTimeMillis();
      ACTIVE_CONTESTS.clear();
      PENDING_CONTESTS.clear();
      COMPLETED_CAPS.clear();
   }
   
   public static void endWar(){
      warActive = false;
      ACTIVE_CONTESTS.clear();
      PENDING_CONTESTS.clear();
      COMPLETED_CAPS.clear();
   }
   
   public static void tickWar(MinecraftServer server){
      int warDuration = NationsConfig.getInt(NationsRegistry.WAR_DURATION_CFG);
      long warEnd = warStart + 60000L * warDuration;
      long now = System.currentTimeMillis();
      if(now > warEnd && warActive){
         endWar();
      }
      
      if(warActive){
         tickContests(server);
      }
   }
   
   private static void tickContests(MinecraftServer server){
      for(Contest contest : ACTIVE_CONTESTS){
         contest.tick(server);
      }
   }
   
   public static void concludeContest(MinecraftServer server, Contest record, ServerPlayerEntity winner){
      ServerWorld contestWorld = server.getWorld(NationsRegistry.CONTEST_DIM);
      ServerWorld overworld = server.getOverworld();
      CapturePoint cap = record.capturePoint();
      ChunkPos capPos = cap.getChunkPos();
      
      COMPLETED_CAPS.add(cap);
      ACTIVE_CONTESTS.remove(record);
      Vec3d tpPos = cap.getBeaconPos().toCenterPos().add(0,2,0);
      winner.addStatusEffect(new StatusEffectInstance(ArcanaRegistry.DEATH_WARD_EFFECT,110,0,false,false,false));
      
      Text controllingNationText = cap.getControllingNation() == null ? Text.translatable("text.nations.unclaimed_tag") : cap.getControllingNation().getFormattedNameTag(false);
      if(winner.equals(record.attacker)){ // Attacker win, change cap ownership
         Nation nation = Nations.getNation(winner);
         if(nation != null){
            MutableText announcement = Text.translatable("text.nations.cap_duel_attacker_victory",
                  controllingNationText,
                  cap.getType().getText().formatted(Formatting.BOLD),
                  Text.translatable("text.nations.capture_point").formatted(Formatting.BOLD,cap.getType().getTextColor()),
                  Text.literal(cap.getChunkPos().toString()).formatted(Formatting.YELLOW,Formatting.BOLD),
                  winner.getDisplayName()
            ).formatted(Formatting.RED);
            Nations.announce(announcement);
            cap.transferOwnership(overworld, nation);
         }
      }else{ // Defender win, buff output
         MutableText announcement = Text.translatable("text.nations.cap_duel_defender_victory",
               controllingNationText,
               cap.getType().getText().formatted(Formatting.BOLD),
               Text.translatable("text.nations.capture_point").formatted(Formatting.BOLD,cap.getType().getTextColor()),
               Text.literal(cap.getChunkPos().toString()).formatted(Formatting.YELLOW,Formatting.BOLD),
               winner.getDisplayName()
         ).formatted(Formatting.RED);
         Nations.announce(announcement);
         
         // TODO
      }
      
      winner.networkHandler.sendPacket(new TitleFadeS2CPacket(20, 40, 20));
      winner.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("text.nations.duel_winner").formatted(Formatting.LIGHT_PURPLE)));
      
      Nations.addTickTimerCallback(new GenericTimer(20*5, () -> {
         winner.teleportTo(new TeleportTarget(overworld,tpPos, Vec3d.ZERO,winner.getYaw(),winner.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
         winner.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.MASTER, 0.5f, 1.2f);
         ParticleEffectUtils.netherRiftTeleport(overworld,winner.getPos(),0);
      }));
      Nations.addTickTimerCallback(new GenericTimer(20*5, () -> {
         teardownDuel(capPos,contestWorld);
      }));
   }
   
   public static boolean isWarActive(){
      return warActive;
   }
   
   public static void startContest(MinecraftServer server, CapturePoint cap, ServerPlayerEntity attacker, ServerPlayerEntity defender){
      ServerWorld contestWorld = server.getWorld(NationsRegistry.CONTEST_DIM);
      ServerWorld overworld = server.getOverworld();
      Contest record = new Contest(cap, attacker, defender);
      ChunkPos capPos = cap.getChunkPos();
      ACTIVE_CONTESTS.add(record);
      setupDuel(server,cap);
      
      sendPrepTeleportMessage(attacker,defender);
      
      Nations.addTickTimerCallback(new GenericTimer(20*11, () -> {
         ChunkPos corner1 = new ChunkPos(capPos.x-DUEL_RANGE,capPos.z-DUEL_RANGE);
         ChunkPos corner2 = new ChunkPos(capPos.x+DUEL_RANGE,capPos.z+DUEL_RANGE);
         BlockPos pos1 = corner1.getBlockPos(8,0,8);
         BlockPos pos2 = corner2.getBlockPos(8,0,8);
         int y1 = SpawnPile.getSurfaceY(contestWorld,contestWorld.getLogicalHeight()-5,pos1.getX(),pos1.getZ());
         int y2 = SpawnPile.getSurfaceY(contestWorld,contestWorld.getLogicalHeight()-5,pos2.getX(),pos2.getZ());
         
         attacker.teleportTo(new TeleportTarget(contestWorld,pos1.toCenterPos().add(0,y1,0), Vec3d.ZERO,attacker.getYaw(),attacker.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
         attacker.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.MASTER, 0.5f, 1.2f);
         ParticleEffectUtils.netherRiftTeleport(contestWorld,attacker.getPos(),0);
         
         defender.teleportTo(new TeleportTarget(contestWorld,pos2.toCenterPos().add(0,y2,0), Vec3d.ZERO,defender.getYaw(),defender.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
         defender.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.MASTER, 0.5f, 1.2f);
         ParticleEffectUtils.netherRiftTeleport(contestWorld,defender.getPos(),0);
      }));
      
      Nations.addTickTimerCallback(new GenericTimer(20*12, () -> {
         record.setBegun(true);
      }));
   }
   
   private static void sendPrepTeleportMessage(ServerPlayerEntity attacker, ServerPlayerEntity defender){
      for(int i = 0; i < 10; i++){
         final int t = 10-i;
         Nations.addTickTimerCallback(new GenericTimer(20*i, () -> {
            attacker.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 20, 0));
            defender.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 20, 0));
         }));
         
         Nations.addTickTimerCallback(new GenericTimer(20*i+5, () -> {
            attacker.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("text.nations.duel_init",t).formatted(Formatting.LIGHT_PURPLE)));
            attacker.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable("text.nations.duel_opponent",defender.getDisplayName()).formatted(Formatting.DARK_PURPLE)));
            
            defender.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("text.nations.duel_init",t).formatted(Formatting.LIGHT_PURPLE)));
            defender.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable("text.nations.duel_opponent",attacker.getDisplayName()).formatted(Formatting.DARK_PURPLE)));
         }));
      }
      
      
      Nations.addTickTimerCallback(new GenericTimer(20*13, () -> {
         attacker.networkHandler.sendPacket(new TitleFadeS2CPacket(20, 40, 20));
         attacker.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("text.nations.duel_start").formatted(Formatting.RED,Formatting.BOLD)));
         
         defender.networkHandler.sendPacket(new TitleFadeS2CPacket(20, 40, 20));
         defender.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("text.nations.duel_start").formatted(Formatting.RED,Formatting.BOLD)));
      }));
   }
   
   private static void setupDuel(MinecraftServer server, CapturePoint cap){
      ServerWorld contestWorld = server.getWorld(NationsRegistry.CONTEST_DIM);
      ServerWorld overworld = server.getOverworld();
      ChunkPos capPos = cap.getChunkPos();
      
      teardownDuel(capPos,contestWorld);
      
      Nations.addTickTimerCallback(new GenericTimer(10, () -> {
         for(Pair<BlockPos, BlockPos> duelCorner : getDuelCorners(capPos, contestWorld)){
            for(BlockPos blockPos : BlockPos.iterate(duelCorner.getLeft(), duelCorner.getRight())){
               contestWorld.setBlockState(blockPos,NationsRegistry.CONTEST_BOUNDARY_BLOCK.getDefaultState(),Block.FORCE_STATE+Block.SKIP_DROPS+Block.REDRAW_ON_MAIN_THREAD);
            }
         }
      }));
      
      Nations.addTickTimerCallback(new GenericTimer(20, () -> {
         for(int x = -DUEL_RANGE; x <= DUEL_RANGE; x++){
            for(int z = -DUEL_RANGE; z <= DUEL_RANGE; z++){
               ChunkPos copyChunk = new ChunkPos(capPos.x+x,capPos.z+z);
               copyChunk(copyChunk,overworld,contestWorld);
            }
         }
      }));
   }
   
   private static List<Pair<BlockPos,BlockPos>> getDuelCorners(ChunkPos pos, ServerWorld contestWorld){
      ArrayList<Pair<BlockPos,BlockPos>> poses = new ArrayList<>();
      int range = DUEL_RANGE+1;
      BlockPos corner1 = (new ChunkPos(pos.x-range,pos.z-range).getBlockPos(15,contestWorld.getBottomY()+1,15));
      BlockPos corner2 = (new ChunkPos(pos.x+range,pos.z+range).getBlockPos(0,contestWorld.getBottomY()+1,0));
      BlockPos corner3 = (new ChunkPos(pos.x+range,pos.z-range).getBlockPos(0,contestWorld.getLogicalHeight()-1,15));
      BlockPos corner4 = (new ChunkPos(pos.x-range,pos.z+range).getBlockPos(15,contestWorld.getLogicalHeight()-1,0));
      poses.add(new Pair<>(corner1,corner2));
      poses.add(new Pair<>(corner1,corner3));
      poses.add(new Pair<>(corner1,corner4));
      poses.add(new Pair<>(corner2,corner3));
      poses.add(new Pair<>(corner2,corner4));
      poses.add(new Pair<>(corner3,corner4));
      return poses;
   }
   
   private static void teardownDuel(ChunkPos pos, ServerWorld contestWorld){
      for(int x = -DUEL_RANGE; x <= DUEL_RANGE; x++){
         for(int z = -DUEL_RANGE; z <= DUEL_RANGE; z++){
            ChunkPos copyChunk = new ChunkPos(pos.x+x,pos.z+z);
            BlockPos corner1 = copyChunk.getBlockPos(0,contestWorld.getBottomY(),0);
            BlockPos corner2 = copyChunk.getBlockPos(15,contestWorld.getLogicalHeight(),15);
            for(BlockPos blockPos : BlockPos.iterate(corner1, corner2)){
               contestWorld.setBlockState(blockPos, Blocks.AIR.getDefaultState(), Block.FORCE_STATE+Block.SKIP_DROPS+Block.REDRAW_ON_MAIN_THREAD);
            }
         }
      }
      
      for(Pair<BlockPos, BlockPos> duelCorner : getDuelCorners(pos, contestWorld)){
         for(BlockPos blockPos : BlockPos.iterate(duelCorner.getLeft(), duelCorner.getRight())){
            contestWorld.setBlockState(blockPos,Blocks.AIR.getDefaultState(),Block.FORCE_STATE+Block.SKIP_DROPS+Block.REDRAW_ON_MAIN_THREAD);
         }
      }
   }
   
   private static void copyChunk(ChunkPos pos, ServerWorld overworld, ServerWorld contestWorld){
      BlockPos corner1 = pos.getBlockPos(0,overworld.getBottomY(),0);
      BlockPos corner2 = pos.getBlockPos(15,overworld.getLogicalHeight(),15);
      
      for(BlockPos blockPos : BlockPos.iterate(corner1, corner2)){
         BlockState blockState = overworld.getBlockState(blockPos);
         if(blockState.hasBlockEntity() || blockState.isAir()) continue;
         contestWorld.setBlockState(blockPos,blockState, Block.FORCE_STATE+Block.SKIP_DROPS+Block.REDRAW_ON_MAIN_THREAD);
      }
   }
   
   public static HashSet<Contest> getActiveContests(){
      return ACTIVE_CONTESTS;
   }
   
   public static HashMap<CapturePoint, ServerPlayerEntity> getPendingContests(){
      return PENDING_CONTESTS;
   }
   
   public static HashSet<CapturePoint> getCompletedCaps(){
      return COMPLETED_CAPS;
   }
   
   
   public static class Contest{
      private CapturePoint capturePoint;
      private ServerPlayerEntity attacker;
      private ServerPlayerEntity defender;
      private int age;
      private boolean begun;
      
      public Contest(CapturePoint capturePoint, ServerPlayerEntity attacker, ServerPlayerEntity defender){
         this.capturePoint = capturePoint;
         this.attacker = attacker;
         this.defender = defender;
         this.age = 0;
         this.begun = false;
      }
      
      public void tick(MinecraftServer server){
         ServerWorld contestWorld = server.getWorld(NationsRegistry.CONTEST_DIM);
         ServerWorld overworld = server.getOverworld();
         ChunkPos pos = capturePoint().getChunkPos();
         int range = DUEL_RANGE+1;
         BlockPos corner1 = (new ChunkPos(pos.x-range,pos.z-range).getBlockPos(15,contestWorld.getBottomY(),15));
         BlockPos corner2 = (new ChunkPos(pos.x+range,pos.z+range).getBlockPos(0,contestWorld.getLogicalHeight(),0));
         Box bounds = new Box(corner1.toCenterPos(),corner2.toCenterPos());
         
         if(begun){
            List<ServerPlayerEntity> players = contestWorld.getPlayers(p -> !p.isSpectator() && !p.isCreative() && p.getBoundingBox().intersects(bounds));
            if(players.contains(attacker)){
               players.remove(attacker);
            }else{ // Defender victory
               attacker.damage(contestWorld, ArcanaDamageTypes.of(contestWorld,NationsRegistry.CONTEST_DAMAGE,defender,defender),attacker.getHealth()*100);
            }
            
            if(players.contains(defender)){
               players.remove(defender);
            }else{ // Attacker victory
               defender.damage(contestWorld, ArcanaDamageTypes.of(contestWorld,NationsRegistry.CONTEST_DAMAGE,attacker,attacker),defender.getHealth()*100);
            }
            
            for(ServerPlayerEntity player : players){ // Interlopers
               player.teleportTo(new TeleportTarget(contestWorld,player.getPos(), Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
               player.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.MASTER, 0.5f, 1.2f);
               ParticleEffectUtils.netherRiftTeleport(contestWorld,player.getPos(),0);
            }
         }
      }
      
      public void setBegun(boolean begun){
         this.begun = begun;
      }
      
      public CapturePoint capturePoint(){
         return capturePoint;
      }
      
      public ServerPlayerEntity attacker(){
         return attacker;
      }
      
      public ServerPlayerEntity defender(){
         return defender;
      }
      
      public int getAge(){
         return age;
      }
      
      public boolean hasBegun(){
         return begun;
      }
      
      @Override
      public boolean equals(Object o){
         if(this == o) return true;
         if(!(o instanceof Contest that)) return false;
         return capturePoint.getId().equals(that.capturePoint.getId()) && attacker.equals(that.attacker) && defender.equals(that.defender);
      }
      
      @Override
      public int hashCode(){
         return Objects.hash(capturePoint, attacker, defender);
      }
   }
}
