package net.borisshoes.nations.gameplay;

import net.borisshoes.arcananovum.items.PearlOfRecall;
import net.borisshoes.arcananovum.utils.SpawnPile;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.integration.DynmapCalls;
import net.borisshoes.nations.utils.ParticleEffectUtils;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.BossBarCommand;
import net.minecraft.server.command.SpreadPlayersCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

import java.util.*;
import java.util.stream.Collectors;

import static net.borisshoes.nations.Nations.MOD_ID;
import static net.borisshoes.nations.cca.WorldDataComponentInitializer.NATIONS_DATA;

public class NetherRift {
   
   private BlockPos overworldPos;
   private BlockPos netherPos;
   private final ServerWorld overworld;
   private final ServerWorld nether;
   private final long warmupTime;
   private final long duration;
   private long age;
   private boolean active;
   private CommandBossBar bossBar;
   private HashSet<Integer> revealed = new HashSet<>();
   private final HashMap<ServerPlayerEntity, Integer> cooldowns = new HashMap<>();
   private static final Identifier barId = Identifier.of(MOD_ID,"nations.nether_rift");
   
   private NetherRift(ServerWorld overworld, ServerWorld nether, BlockPos overworldPos, BlockPos netherPos, long warmupTime, long duration, long age, boolean active, HashSet<Integer> revealed){
      this.overworld = overworld;
      this.nether = nether;
      this.overworldPos = overworldPos;
      this.netherPos = netherPos;
      this.warmupTime = warmupTime;
      this.duration = duration;
      this.age = age;
      this.active = active;
      this.revealed = revealed;
      this.bossBar = overworld.getServer().getBossBarManager().get(barId);
      if(this.bossBar == null){
         bossBar = overworld.getServer().getBossBarManager().add(barId, getBossbarText());
         bossBar.setColor(BossBar.Color.PURPLE);
         bossBar.setStyle(BossBar.Style.PROGRESS);
         bossBar.setPercent(0);
      }
   }
   
   public NetherRift(ServerWorld overworld, ServerWorld nether, long warmupTime, long duration){
      this.overworld = overworld;
      this.nether = nether;
      this.warmupTime = warmupTime;
      this.duration = duration;
      this.age = 0;
      this.active = true;
      findNewLocation();
      bossBar = overworld.getServer().getBossBarManager().add(barId, getBossbarText());
      bossBar.setColor(BossBar.Color.PURPLE);
      bossBar.setStyle(BossBar.Style.PROGRESS);
      bossBar.setPercent(0);
   }
   
   private String getCoordStr(){
      int maxLength = String.valueOf(-NationsConfig.getInt(NationsRegistry.WORLD_BORDER_RADIUS_OVERWORLD_CFG)*16).length();
      String x = String.format("%"+maxLength+"d", overworldPos.getX());
      String y = String.format("%4d", overworldPos.getY());
      String z = String.format("%"+maxLength+"d", overworldPos.getZ());
      return x+", "+y+", "+z;
   }
   
   private void findNewLocation(){
      List<NationChunk> chunks = new ArrayList<>(Nations.getChunks());
      Collections.shuffle(chunks);
      
      NationChunk chunk = chunks.stream().filter(c -> c.getControllingNation() == null).findFirst().orElse(null);
      if(chunk == null){
         chunk = chunks.getFirst();
      }
      int x = overworld.getRandom().nextInt(16);
      int z = overworld.getRandom().nextInt(16);
      int y = 64;
      
      BlockPos.Mutable mutable = chunk.getPos().getBlockPos(x,overworld.getLogicalHeight() + 1,z).mutableCopy();
      boolean topAir = overworld.getBlockState(mutable).isAir();
      mutable.move(Direction.DOWN);
      boolean bottomAir = overworld.getBlockState(mutable).isAir();
      while(mutable.getY() > overworld.getBottomY()){
         mutable.move(Direction.DOWN);
         boolean floorNotAir = overworld.getBlockState(mutable).isAir();
         if(!floorNotAir && bottomAir && topAir){
            y = mutable.getY() + 1;
            break;
         }
         topAir = bottomAir;
         bottomAir = floorNotAir;
      }
      this.overworldPos = chunk.getPos().getBlockPos(x,y,z);
      
      int border = NationsConfig.getInt(NationsRegistry.WORLD_BORDER_RADIUS_NETHER_CFG) * 16;
      netherPos = SpawnPile.makeSpawnLocations(1,border,100,nether,new BlockPos(0,0,0)).getFirst();
   }
   
   public void tick(){
      if(!isActive()){
         if(!bossBar.getPlayers().isEmpty()) bossBar.clearPlayers();
         if(overworld.getServer().getBossBarManager().get(barId) != null) overworld.getServer().getBossBarManager().remove(bossBar);
         return;
      }
      if(age == 0){
         if(warmupTime <= 0){
            spawn();
         }else{
            Nations.announce(Text.translatable("text.nations.nether_rift.spawning",Text.translatable("text.nations.nether_rift").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD)).formatted(Formatting.DARK_PURPLE));
         }
      }else if(age == warmupTime){
         spawn();
      }else if(age-warmupTime == duration/2){
         Nations.announce(Text.translatable("text.nations.nether_rift.flicker", Text.translatable("text.nations.nether_rift").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD)).formatted(Formatting.DARK_PURPLE));
      }else if(age-warmupTime == duration-5999){
         Nations.announce(Text.translatable("text.nations.nether_rift.unstable", Text.translatable("text.nations.nether_rift").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD)).formatted(Formatting.DARK_PURPLE));
      }else if(age-warmupTime == duration-100){
         for(ServerPlayerEntity player : nether.getPlayers()){
            player.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.MASTER, 1.0f, 0.7f);
         }
      }else if(age-warmupTime == duration){
         close();
      }else if(age-warmupTime > duration-100){
         for(ServerPlayerEntity player : nether.getPlayers()){
            nether.spawnParticles(ParticleTypes.REVERSE_PORTAL,true,true,player.getX(),player.getY()+player.getHeight()/2,player.getZ(),15,0.3,0.5,0.3,0.3);
         }
      }
      boolean warmingUp = age < warmupTime;
      
      if(age % 20 == 0){
         if(warmingUp){
            int revealSize = revealed.size();
            int strSize = getCoordStr().length();
            float percentage = (float) age / warmupTime;
            float revealPercentage = (float) revealSize / strSize;
            if(revealPercentage < percentage && revealSize < strSize){
               int ind;
               do {
                  ind = overworld.getRandom().nextInt(strSize + 1);
               } while (!revealed.add(ind));
               bossBar.setName(getBossbarText());
            }
            bossBar.setPercent(percentage);
         }else{
            for(ServerPlayerEntity player : overworld.getPlayers(player -> player.squaredDistanceTo(getOverworldCenter()) < 4 && !cooldowns.containsKey(player))){
               player.teleportTo(new TeleportTarget(nether,SpawnPile.makeSpawnLocations(1,10,100,nether,netherPos).getFirst().toCenterPos(),Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
               player.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.MASTER, 0.5f, 1.2f);
               ParticleEffectUtils.netherRiftTeleport(nether,player.getPos(),0);
               cooldowns.put(player,10);
               Nations.getPlayer(player).setRiftReturnPos(overworldPos);
            }
            for(ServerPlayerEntity player : nether.getPlayers(player -> player.squaredDistanceTo(getNetherCenter()) < 4 && !cooldowns.containsKey(player))){
               player.teleportTo(new TeleportTarget(overworld,SpawnPile.makeSpawnLocations(1,10,overworld,overworldPos).getFirst().toCenterPos(),Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
               player.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.MASTER, 0.5f, 1.2f);
               ParticleEffectUtils.netherRiftTeleport(overworld,player.getPos(),0);
               cooldowns.put(player,10);
               Nations.getPlayer(player).setRiftReturnPos(null);
            }
            float percentage = (float) (age - warmupTime) / duration;
            bossBar.setPercent(1-percentage);
         }
         
         List<ServerPlayerEntity> toRemove = new ArrayList<>();
         for(Map.Entry<ServerPlayerEntity, Integer> entry : cooldowns.entrySet()){
            if(entry.getValue() == 1) toRemove.add(entry.getKey());
            cooldowns.put(entry.getKey(), entry.getValue()-1);
         }
         toRemove.forEach(cooldowns::remove);
         
         for(ServerPlayerEntity player : overworld.getServer().getPlayerManager().getPlayerList()){
            if(!bossBar.getPlayers().contains(player)){
               bossBar.addPlayer(player);
            }
         }
         
         NATIONS_DATA.get(overworld).setRiftData(toNbt(new NbtCompound()));
      }
      
      if(!warmingUp && age % 264 == 0){
         for(ServerPlayerEntity player : overworld.getPlayers(player -> player.squaredDistanceTo(getOverworldCenter()) < 100)){
            player.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.MASTER, 0.5f, 1.2f);
         }
         for(ServerPlayerEntity player : nether.getPlayers(player -> player.squaredDistanceTo(getNetherCenter()) < 100)){
            player.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.MASTER, 0.5f, 1.2f);
         }
      }
      
      if(!warmingUp){
         ParticleEffectUtils.netherRift(overworld,overworldPos);
         ParticleEffectUtils.netherRift(nether,netherPos);
      }else{
         ParticleEffectUtils.netherRiftSpawning(overworld,overworldPos);
      }
      
      this.age++;
   }
   
   public Vec3d getOverworldCenter(){
      return overworldPos.toCenterPos().add(0,1,0);
   }
   
   public Vec3d getNetherCenter(){
      return netherPos.toCenterPos().add(0,1,0);
   }
   
   private void spawn(){
      int strSize = getCoordStr().length();
      for(int i = 0; i < strSize; i++){
         revealed.add(i);
      }
      bossBar.setName(getBossbarText());
      Nations.announce(Text.translatable("text.nations.nether_rift.spawned",
            Text.translatable("text.nations.nether_rift").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD),
            Text.literal(overworldPos.getX()+", "+overworldPos.getY()+", "+overworldPos.getZ()).formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD)
      ).formatted(Formatting.DARK_PURPLE));
      bossBar.setStyle(BossBar.Style.NOTCHED_12);
      bossBar.setPercent(1);
      for(ServerPlayerEntity player : overworld.getPlayers()){
         player.playSoundToPlayer(SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.MASTER, 0.5f, 1.5f);
      }
      NATIONS_DATA.get(overworld).setRiftData(toNbt(new NbtCompound()));
      DynmapCalls.updateNetherRiftMarker();
   }
   
   public void forceClose(){
      if(this.isActive()){
         this.age = duration-101+warmupTime;
      }
   }
   
   private void close(){
      this.active = false;
      Nations.announce(Text.translatable("text.nations.nether_rift.close", Text.translatable("text.nations.nether_rift").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD)).formatted(Formatting.DARK_PURPLE));
      for(ServerPlayerEntity player : new ArrayList<>(nether.getPlayers())){
         player.teleportTo(new TeleportTarget(overworld,getOverworldCenter(),Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
         cooldowns.put(player,10);
         Nations.getPlayer(player).setRiftReturnPos(null);
      }
      bossBar.clearPlayers();
      overworld.getServer().getBossBarManager().remove(bossBar);
      NATIONS_DATA.get(overworld).setRiftData(toNbt(new NbtCompound()));
      DynmapCalls.updateNetherRiftMarker();
   }
   
   public boolean isActive(){
      return active;
   }
   
   public boolean isOpen(){
      return isActive() && (age >= warmupTime);
   }
   
   private MutableText getBossbarText(){
      String coordStr = getCoordStr();
      MutableText text = Text.literal("")
            .append(Text.literal("☀ ").formatted(Formatting.LIGHT_PURPLE))
            .append(Text.translatable("text.nations.nether_rift").formatted(Formatting.DARK_PURPLE,Formatting.BOLD))
            .append(Text.literal(" - ").formatted(Formatting.LIGHT_PURPLE));
      for(int i = 0; i < coordStr.length(); i++){
         if(revealed.contains(i)){
            char character = coordStr.charAt(i);
            text = text.append(Text.literal(character == ' ' ? "" : character+"").formatted(Formatting.DARK_PURPLE));
         }else{
            text = text.append(Text.literal("_").formatted(Formatting.DARK_PURPLE,Formatting.BOLD,Formatting.OBFUSCATED));
         }
      }
      return text.append(Text.literal(" ☀").formatted(Formatting.LIGHT_PURPLE));
   }
   
   public NbtCompound toNbt(NbtCompound compound){
      NbtCompound oPos = new NbtCompound();
      oPos.putInt("x",overworldPos.getX());
      oPos.putInt("y",overworldPos.getY());
      oPos.putInt("z",overworldPos.getZ());
      NbtCompound nPos = new NbtCompound();
      nPos.putInt("x",netherPos.getX());
      nPos.putInt("y",netherPos.getY());
      nPos.putInt("z",netherPos.getZ());
      NbtList revealedList = new NbtList();
      revealedList.addAll(revealed.stream().map(NbtInt::of).collect(Collectors.toSet()));
      
      compound.put("overworldPos",oPos);
      compound.put("netherPos",nPos);
      compound.put("revealed",revealedList);
      compound.putLong("warmup",warmupTime);
      compound.putLong("duration",duration);
      compound.putLong("age",age);
      compound.putBoolean("active",active);
      return compound;
   }
   
   public static NetherRift fromNbt(NbtCompound compound, ServerWorld overworld, ServerWorld nether){
      NbtCompound oPos = compound.getCompound("overworldPos");
      BlockPos oBPos = new BlockPos(oPos.getInt("x"),oPos.getInt("y"),oPos.getInt("z"));
      NbtCompound nPos = compound.getCompound("netherPos");
      BlockPos nBPos = new BlockPos(nPos.getInt("x"),nPos.getInt("y"),nPos.getInt("z"));
      NbtList revealedList = compound.getList("revealed", NbtElement.INT_TYPE);
      HashSet<Integer> revealedSet = revealedList.stream().map(e -> ((NbtInt) e).intValue()).collect(Collectors.toCollection(HashSet::new));
      return new NetherRift(overworld,nether,oBPos,nBPos,
            compound.getLong("warmupTime"),
            compound.getLong("duration"),
            compound.getLong("age"),
            compound.getBoolean("active"),
            revealedSet
      );
   }
}
