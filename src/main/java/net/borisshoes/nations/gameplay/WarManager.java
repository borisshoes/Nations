package net.borisshoes.nations.gameplay;

import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.SubArchetype;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.arcananovum.ArcanaConfig;
import net.borisshoes.arcananovum.ArcanaNovum;
import net.borisshoes.arcananovum.ArcanaRegistry;
import net.borisshoes.arcananovum.achievements.ArcanaAchievements;
import net.borisshoes.arcananovum.augments.ArcanaAugments;
import net.borisshoes.arcananovum.callbacks.ItemReturnTimerCallback;
import net.borisshoes.arcananovum.core.ArcanaItem;
import net.borisshoes.arcananovum.damage.ArcanaDamageTypes;
import net.borisshoes.arcananovum.items.*;
import net.borisshoes.arcananovum.items.arrows.ConcussionArrows;
import net.borisshoes.arcananovum.items.arrows.EnsnarementArrows;
import net.borisshoes.arcananovum.items.arrows.ExpulsionArrows;
import net.borisshoes.arcananovum.items.arrows.GravitonArrows;
import net.borisshoes.arcananovum.items.charms.CindersCharm;
import net.borisshoes.arcananovum.items.charms.FelidaeCharm;
import net.borisshoes.arcananovum.utils.ArcanaItemUtils;
import net.borisshoes.arcananovum.utils.EnhancedStatUtils;
import net.borisshoes.arcananovum.utils.SoundUtils;
import net.borisshoes.arcananovum.utils.SpawnPile;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.callbacks.PlayerConnectionCallback;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.borisshoes.nations.utils.GenericTimer;
import net.borisshoes.nations.utils.MiscUtils;
import net.borisshoes.nations.utils.ParticleEffectUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.borisshoes.nations.Nations.*;

public class WarManager {
   
   public static final int DUEL_RANGE = 2;
   private static final HashSet<Contest> ACTIVE_CONTESTS = new HashSet<>();
   private static final HashMap<CapturePoint,ServerPlayerEntity> PENDING_CONTESTS = new HashMap<>();
   private static final HashMap<CapturePoint,List<Nation>> LOCKED_CAPS = new HashMap<>();
   private static final HashMap<Nation,Integer> ATTACKS_ISSUED = new HashMap<>();
   private static final List<Pair<Nation,Nation>> ATTACK_PAIRS = new ArrayList<>();
   
   private static long warStart = 0;
   private static boolean warActive = false;
   private static boolean attackPhase = false;
   private static int attackCycle = 0;
   
   public static MutableText getWarStatus(@Nullable ServerPlayerEntity player){
      if(!warActive) return Text.translatable("text.nations.no_war_active").formatted(Formatting.GREEN);
      Nation nation;
      if(player != null) nation = getNation(player);
      else{
         nation = null;
      }
      long now = System.currentTimeMillis();
      long warDuration = NationsConfig.getInt(NationsRegistry.WAR_DURATION_CFG) * 60000L; // millis
      int numCycles = NationsConfig.getInt(NationsRegistry.WAR_CYCLES_CFG);
      int numAttacks = NationsConfig.getInt(NationsRegistry.WAR_ATTACK_LIMIT_CFG);
      float cycleDuration = (float) warDuration / numCycles; // millis
      long warEnd = warStart + warDuration; // millis
      long elapsed = now - warStart;
      float phaseDuration = cycleDuration / 2;
      float phaseElapsed = elapsed % phaseDuration;
      
      MutableText text = Text.empty();
      text.append(Text.translatable("text.nations.war_readout_header",
            MiscUtils.getTimeDiff(warEnd-now)).formatted(Formatting.YELLOW)
      ).formatted(Formatting.RED).append(Text.literal("\n"));
      text.append(Text.translatable("text.nations.war_readout_cycle",
            Text.literal(""+attackCycle).formatted(Formatting.AQUA,Formatting.BOLD),
            Text.literal(""+numCycles).formatted(Formatting.AQUA,Formatting.BOLD),
            Text.translatable(attackPhase ? "text.nations.attack" : "text.nations.defend").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD)
      ).formatted(Formatting.DARK_AQUA)).append(Text.literal("\n"));
      text.append(Text.translatable("text.nations.war_readout_next_phase",
            MiscUtils.getTimeDiff((long)(phaseDuration-phaseElapsed)).formatted(Formatting.GREEN)
      ).formatted(Formatting.DARK_GREEN)).append(Text.literal("\n\n"));
      if(nation != null){
         int atkCount = numAttacks-ATTACKS_ISSUED.getOrDefault(nation,0);
         text.append(Text.translatable("text.nations.war_readout_attack_count",Text.literal(""+atkCount).formatted(Formatting.BOLD,Formatting.GOLD))).formatted(Formatting.RED).append(Text.literal("\n"));
      }
      text.append(Text.translatable("text.nations.war_readout_attacks_header").formatted(Formatting.YELLOW,Formatting.BOLD)).append(Text.literal("\n"));
      PENDING_CONTESTS.forEach((cap, attacker) ->{
         Text controllingNationText = cap.getControllingNation() == null ? Text.translatable("text.nations.unclaimed_tag") : cap.getControllingNation().getFormattedNameTag(false);
         MutableText msg = Text.translatable("text.nations.cap_duel_contested",
               controllingNationText,
               cap.getType().getText(),
               Text.translatable("text.nations.capture_point").formatted(cap.getType().getTextColor()),
               Text.literal(cap.getChunkPos().toString()).formatted(Formatting.DARK_AQUA),
               attacker.getDisplayName()
         ).formatted(Formatting.YELLOW);
         text.append(Text.literal(" - ").formatted(Formatting.BOLD,Formatting.GOLD)).append(msg).append(Text.literal("\n"));
      });
      if(nation != null){
         text.append(Text.translatable("text.nations.war_readout_locked_header").formatted(Formatting.RED,Formatting.BOLD)).append(Text.literal("\n"));
         LOCKED_CAPS.forEach((cap, list) -> {
            if(!list.contains(nation)) return;
            Text controllingNationText = cap.getControllingNation() == null ? Text.translatable("text.nations.unclaimed_tag") : cap.getControllingNation().getFormattedNameTag(false);
            MutableText msg = Text.translatable("text.nations.cap_locked",
                  controllingNationText,
                  cap.getType().getText(),
                  Text.translatable("text.nations.capture_point").formatted(cap.getType().getTextColor()),
                  Text.literal(cap.getChunkPos().toString()).formatted(Formatting.DARK_AQUA)
            ).formatted(Formatting.RED);
            text.append(Text.literal(" - ").formatted(Formatting.BOLD,Formatting.DARK_RED)).append(msg).append(Text.literal("\n"));
         });
      }
      text.append(Text.translatable("text.nations.war_readout_duels_header").formatted(Formatting.GREEN,Formatting.BOLD)).append(Text.literal("\n"));
      ACTIVE_CONTESTS.forEach(contest -> {
         CapturePoint cap = contest.capturePoint();
         UUID attackerId = contest.attacker();
         UUID defenderId = contest.defender();
         ServerPlayerEntity attacker = SERVER.getPlayerManager().getPlayer(attackerId);
         ServerPlayerEntity defender = SERVER.getPlayerManager().getPlayer(defenderId);
         Text controllingNationText = cap.getControllingNation() == null ? Text.translatable("text.nations.unclaimed_tag") : cap.getControllingNation().getFormattedNameTag(false);
         MutableText msg = Text.translatable("text.nations.cap_duel_active",
               controllingNationText,
               cap.getType().getText(),
               Text.translatable("text.nations.capture_point").formatted(cap.getType().getTextColor()),
               Text.literal(cap.getChunkPos().toString()).formatted(Formatting.DARK_AQUA),
               attacker == null ? Text.literal(attackerId.toString()) : attacker.getStyledDisplayName(),
               defender == null ? Text.literal(defenderId.toString()) : defender.getStyledDisplayName()
         ).formatted(Formatting.GREEN);
         text.append(Text.literal(" - ").formatted(Formatting.BOLD,Formatting.DARK_GREEN)).append(msg).append(Text.literal("\n"));
      });
      return text;
   }
   
   private static void lockCapturePoint(CapturePoint capturePoint){
      LOCKED_CAPS.put(capturePoint,new ArrayList<>(Nations.getNations()));
   }
   
   private static void lockCapturePoint(CapturePoint capturePoint, Nation nation){
      List<Nation> locks = LOCKED_CAPS.getOrDefault(capturePoint,new ArrayList<>());
      locks.add(nation);
      LOCKED_CAPS.put(capturePoint,locks);
   }
   
   private static boolean isLocked(CapturePoint capturePoint, ServerPlayerEntity player){
      Nation atkNation = Nations.getNation(player);
      if(atkNation == null) return false;
      List<Nation> locks = LOCKED_CAPS.getOrDefault(capturePoint,new ArrayList<>());
      return locks.contains(atkNation);
   }
   
   public static boolean capIsContested(CapturePoint capturePoint){
      if(!isWarActive()) return false;
      return PENDING_CONTESTS.containsKey(capturePoint);
   }
   
   public static void cancelPendingContestsFromPlayer(ServerPlayerEntity player){
      cancelPendingContestsFromPlayer(player,null);
   }
   
   public static void cancelPendingContestsFromPlayer(ServerPlayerEntity player, ServerPlayerEntity killer){
      if(!isWarActive()) return;
      Nation killerNation = null;
      if(killer != null){
         killerNation = Nations.getNation(killer);
      }
      ArrayList<CapturePoint> toRemove = new ArrayList<>();
      for(Map.Entry<CapturePoint, ServerPlayerEntity> entry : WarManager.getPendingContests().entrySet()){
         CapturePoint cap = entry.getKey();
         if(entry.getValue().equals(player)){
            toRemove.add(cap);
         }
         if(killerNation != null){
            Nation owningNation = cap.getControllingNation();
            if(owningNation != null && owningNation.equals(killerNation)){
               if(Nations.getNation(player) != null){
                  lockCapturePoint(cap,Nations.getNation(player));
               }
            }
         }
      }
      toRemove.forEach(WarManager::cancelPendingContest);
   }
   
   public static void cancelPendingContest(CapturePoint capturePoint){
      if(!PENDING_CONTESTS.containsKey(capturePoint)) return;
      ServerPlayerEntity attacker = PENDING_CONTESTS.get(capturePoint);
      Nation atkNation = Nations.getNation(attacker);
      if(atkNation != null){
         ATTACKS_ISSUED.put(atkNation,ATTACKS_ISSUED.getOrDefault(atkNation,1)-1);
         int cost = capturePoint.calculateAttackCost(atkNation);
         int storedGrowth = atkNation.getStoredCoins().getOrDefault(ResourceType.GROWTH,0);
         atkNation.getStoredCoins().put(ResourceType.GROWTH,storedGrowth+cost);
      }
      
      Text controllingNationText = capturePoint.getControllingNation() == null ? Text.translatable("text.nations.unclaimed_tag") : capturePoint.getControllingNation().getFormattedNameTag(false);
      MutableText announcement = Text.translatable("text.nations.cap_duel_cancel",
            controllingNationText,
            capturePoint.getType().getText().formatted(Formatting.BOLD),
            Text.translatable("text.nations.capture_point").formatted(Formatting.BOLD,capturePoint.getType().getTextColor()),
            Text.literal(capturePoint.getChunkPos().toString()).formatted(Formatting.YELLOW,Formatting.BOLD),
            attacker.getDisplayName()
      ).formatted(Formatting.RED);
      Nations.announce(announcement);
      
      PENDING_CONTESTS.remove(capturePoint);
   }
   
   public static void addPendingContest(CapturePoint capturePoint, ServerPlayerEntity player){
      if(!canContestCap(capturePoint,player)) return;
      PENDING_CONTESTS.put(capturePoint,player);
      Nation atkNation = Nations.getNation(player);
      int curAtks = ATTACKS_ISSUED.getOrDefault(atkNation,0);
      ATTACKS_ISSUED.put(atkNation,curAtks+1);
      
      if(capturePoint.getControllingNation() != null){
         ATTACK_PAIRS.add(new Pair<>(atkNation,capturePoint.getControllingNation()));
      }
      
      Text controllingNationText = capturePoint.getControllingNation() == null ? Text.translatable("text.nations.unclaimed_tag") : capturePoint.getControllingNation().getFormattedNameTag(false);
      MutableText announcement = Text.translatable("text.nations.cap_duel_contested",
            controllingNationText,
            capturePoint.getType().getText().formatted(Formatting.BOLD),
            Text.translatable("text.nations.capture_point").formatted(Formatting.BOLD,capturePoint.getType().getTextColor()),
            Text.literal(capturePoint.getChunkPos().toString()).formatted(Formatting.DARK_AQUA,Formatting.BOLD),
            player.getDisplayName()
      ).formatted(Formatting.YELLOW);
      Nations.announce(announcement);
      
      int defendRadius = NationsConfig.getInt(NationsRegistry.WAR_DEFENSE_RADIUS_CFG);
      if(capturePoint.getControllingNation() != null && defendRadius > 8){
         Nation defNation = capturePoint.getControllingNation();
         ChunkPos pos = capturePoint.getChunkPos();
         Text defMsg = Text.translatable("text.nations.defend_info",
               Text.literal(String.format("%,d",defendRadius)).formatted(Formatting.BOLD,Formatting.GOLD),
               Text.literal("/nation defend "+pos.x+" "+pos.z).formatted(Formatting.ITALIC,Formatting.GOLD)
         ).formatted(Formatting.RED).styled(style ->
               style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nation defend "+pos.x+" "+pos.z))
                  .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("text.nations.click_to_defend"))));
         
         for(ServerPlayerEntity onlinePlayer : defNation.getOnlinePlayers()){
            onlinePlayer.sendMessage(defMsg,false);
         }
      }
   }
   
   public static void defendContest(CapturePoint capturePoint, ServerPlayerEntity defender){
      if(!capIsContested(capturePoint)) return;
      cancelPendingContestsFromPlayer(defender);
      ServerPlayerEntity attacker = PENDING_CONTESTS.get(capturePoint);
      List<CapturePoint> defendPoints = PENDING_CONTESTS.entrySet().stream().filter(entry -> entry.getValue().equals(attacker)).map(Map.Entry::getKey).toList();
      Contest main = startContest(defender.getServer(),capturePoint,attacker,defender);
      for(CapturePoint defendPoint : defendPoints){
         if(defendPoint.equals(capturePoint)) continue;
         Contest record = new Contest(defendPoint, attacker.getUuid(), defender.getUuid());
         record.setProxy(main);
         PENDING_CONTESTS.remove(defendPoint);
         ACTIVE_CONTESTS.add(record);
      }
   }
   
   public static boolean canContestCap(CapturePoint capturePoint, ServerPlayerEntity player){
      Nation capNation = capturePoint.getControllingNation();
      Nation atkNation = Nations.getNation(player);
      if(!isWarActive() || !canAttack()) return false;
      if(capNation == null || atkNation == null) return false;
      if(capNation.equals(atkNation)) return false;
      int defenderCaps = capNation.getCapCount();
      int attackerCaps = atkNation.getCapCount();
      int minDiff = NationsConfig.getInt(NationsRegistry.WAR_MINIMUM_CAPTURE_POINT_DIFFERENCE_CFG);
      if(attackerCaps - defenderCaps >= minDiff && ATTACK_PAIRS.stream().noneMatch(pair -> pair.getLeft().equals(capNation) && pair.getRight().equals(atkNation))) return false;
      if(PENDING_CONTESTS.containsKey(capturePoint)) return false;
      if(ACTIVE_CONTESTS.stream().anyMatch(contest -> contest.capturePoint().equals(capturePoint))) return false;
      if(isLocked(capturePoint,player)) return false;
      if(PENDING_CONTESTS.entrySet().stream().anyMatch(entry -> entry.getValue().equals(player) && entry.getKey().getControllingNation() != null && !entry.getKey().getControllingNation().equals(capNation))) return false;
      int atkLimit = NationsConfig.getInt(NationsRegistry.WAR_ATTACK_LIMIT_CFG);
      if(ATTACKS_ISSUED.getOrDefault(atkNation,0) >= atkLimit) return false;
      return true;
   }
   
   public static void startWar(){
      warStart = System.currentTimeMillis();
      attackPhase = false;
      attackCycle = 0;
      warActive = true;
      ACTIVE_CONTESTS.clear();
      PENDING_CONTESTS.clear();
      LOCKED_CAPS.clear();
      ATTACKS_ISSUED.clear();
      ATTACK_PAIRS.clear();
      int warDuration = NationsConfig.getInt(NationsRegistry.WAR_DURATION_CFG);
      Nations.announce(Text.translatable("text.nations.war_announcement",MiscUtils.getTimeDiff(warDuration * 60000L).formatted(Formatting.BOLD,Formatting.RED)).formatted(Formatting.DARK_RED,Formatting.BOLD));
      Nations.announce(Text.translatable("text.nations.war_reminder_1").formatted(Formatting.GOLD));
      Nations.announce(Text.translatable("text.nations.war_reminder_2").formatted(Formatting.GOLD));
      Nations.announce(Text.translatable("text.nations.war_reminder_3").formatted(Formatting.GOLD));
      calculatePhase();
   }
   
   public static void endWar(){
      endWarCycle(Nations.SERVER);
      warActive = false;
      attackPhase = false;
      attackCycle = 0;
      Nations.announce(Text.translatable("text.nations.war_end").formatted(Formatting.DARK_RED,Formatting.BOLD));
   }
   
   public static void calculatePhase(){
      if(!isWarActive()) return;
      long warDuration = NationsConfig.getInt(NationsRegistry.WAR_DURATION_CFG) * 60000L; // millis
      long cycleDuration = warDuration / NationsConfig.getInt(NationsRegistry.WAR_CYCLES_CFG); // millis
      long warEnd = warStart + 60000L * warDuration;
      long now = System.currentTimeMillis();
      long warElapsed = now - warStart; // millis
      attackCycle = (int) Math.ceilDiv(warElapsed,cycleDuration);
      long cycleMod = warElapsed % (cycleDuration);
      attackPhase = cycleMod <= (cycleDuration/2);
   }
   
   public static boolean canAttack(){
      return isWarActive() && attackPhase;
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
         boolean atk = attackPhase;
         int oldCycle = attackCycle;
         calculatePhase();
         boolean newAtk = attackPhase;
         int newCycle = attackCycle;
         if(atk && !newAtk){ // Attack End
            long cycleDuration = 60000L * warDuration / (NationsConfig.getInt(NationsRegistry.WAR_CYCLES_CFG) * 2L);
            Nations.announce(Text.translatable("text.nations.war_attack_end", MiscUtils.getTimeDiff(cycleDuration).formatted(Formatting.GOLD)).formatted(Formatting.RED));
         }else if(!atk && newAtk){ // Attack Start
            Nations.announce(Text.translatable("text.nations.war_attack_start").formatted(Formatting.RED));
         }
         if(oldCycle != newCycle){
            endWarCycle(server);
         }
      }
   }
   
   public static void endWarCycle(MinecraftServer server){
      ServerWorld contestWorld = server.getWorld(NationsRegistry.CONTEST_DIM);
      ServerWorld overworld = server.getOverworld();
      
      PENDING_CONTESTS.forEach((capturePoint, attacker) -> {
         ChunkPos capPos = capturePoint.getChunkPos();
         Text controllingNationText = capturePoint.getControllingNation() == null ? Text.translatable("text.nations.unclaimed_tag") : capturePoint.getControllingNation().getFormattedNameTag(false);
         Nation nation = Nations.getNation(attacker);
         if(nation != null){
            MutableText announcement = Text.translatable("text.nations.cap_duel_attacker_victory",
                  controllingNationText,
                  capturePoint.getType().getText().formatted(Formatting.BOLD),
                  Text.translatable("text.nations.capture_point").formatted(Formatting.BOLD,capturePoint.getType().getTextColor()),
                  Text.literal(capturePoint.getChunkPos().toString()).formatted(Formatting.YELLOW,Formatting.BOLD),
                  attacker.getDisplayName()
            ).formatted(Formatting.RED);
            Nations.announce(announcement);
            if(Nations.getChunk(capturePoint.getChunkPos()).getControllingNation() != null){
               capturePoint.blockadeOutput();
            }else{
               capturePoint.transferOwnership(overworld, nation);
            }
         }
         lockCapturePoint(capturePoint);
      });
      PENDING_CONTESTS.clear();
   }
   
   public static long getWarEnd(){
      int warDuration = NationsConfig.getInt(NationsRegistry.WAR_DURATION_CFG);
      return warStart + 60000L * warDuration;
   }
   
   private static void tickContests(MinecraftServer server){
      for(Contest contest : new ArrayList<>(ACTIVE_CONTESTS)){
         contest.tick(server);
      }
   }
   
   public static void concludeContest(MinecraftServer server, Contest record, UUID winnerId){
      ServerWorld contestWorld = server.getWorld(NationsRegistry.CONTEST_DIM);
      ServerWorld overworld = server.getOverworld();
      CapturePoint cap = record.capturePoint();
      ChunkPos capPos = cap.getChunkPos();
      ServerPlayerEntity winner = server.getPlayerManager().getPlayer(winnerId);
      if(record.isProxy()) return;
      
      Vec3d tpPos = cap.getBeaconPos().toCenterPos().add(0,2,0);
      if(winner != null) winner.addStatusEffect(new StatusEffectInstance(ArcanaRegistry.DEATH_WARD_EFFECT,110,0,false,false,false));
      
      List<Contest> proxies = new ArrayList<>();
      proxies.add(record);
      proxies.addAll(ACTIVE_CONTESTS.stream().filter(contest -> contest.getProxy() != null && contest.getProxy().equals(record)).toList());
      
      record.bossBar.clearPlayers();
      server.getBossBarManager().remove(record.bossBar);
      
      for(Contest proxy : proxies){
         CapturePoint proxyCap = proxy.capturePoint;
         lockCapturePoint(proxyCap);
         ACTIVE_CONTESTS.remove(proxy);
         
         Text controllingNationText = proxyCap.getControllingNation() == null ? Text.translatable("text.nations.unclaimed_tag") : proxyCap.getControllingNation().getFormattedNameTag(false);
         if(winnerId.equals(record.attacker)){ // Attacker win, change cap ownership
            INationsProfileComponent profile = Nations.getPlayerOrOffline(winnerId);
            if(profile != null && profile.getNation() != null){
               Nation nation = profile.getNation();
               MutableText announcement = Text.translatable("text.nations.cap_duel_attacker_victory",
                     controllingNationText,
                     proxyCap.getType().getText().formatted(Formatting.BOLD),
                     Text.translatable("text.nations.capture_point").formatted(Formatting.BOLD,proxyCap.getType().getTextColor()),
                     Text.literal(proxyCap.getChunkPos().toString()).formatted(Formatting.YELLOW,Formatting.BOLD),
                     winner != null ? winner.getDisplayName() : winnerId
               ).formatted(Formatting.RED);
               Nations.announce(announcement);
               if(Nations.getChunk(proxyCap.getChunkPos()).getControllingNation() != null){
                  proxyCap.blockadeOutput();
               }else{
                  proxyCap.transferOwnership(overworld, nation);
               }
            }
         }else{ // Defender win, buff output
            MutableText announcement = Text.translatable("text.nations.cap_duel_defender_victory",
                  controllingNationText,
                  proxyCap.getType().getText().formatted(Formatting.BOLD),
                  Text.translatable("text.nations.capture_point").formatted(Formatting.BOLD,proxyCap.getType().getTextColor()),
                  Text.literal(proxyCap.getChunkPos().toString()).formatted(Formatting.YELLOW,Formatting.BOLD),
                  winner != null ? winner.getDisplayName() : winnerId
            ).formatted(Formatting.RED);
            Nations.announce(announcement);
            proxyCap.buffOutput();
         }
      }
      
      if(winner != null){
         winner.heal(winner.getMaxHealth());
         winner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,1200,4));
         
         winner.networkHandler.sendPacket(new TitleFadeS2CPacket(20, 40, 20));
         winner.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("text.nations.duel_winner").formatted(Formatting.LIGHT_PURPLE)));
         
         Nations.addTickTimerCallback(new GenericTimer(20*5, () -> {
            winner.teleportTo(new TeleportTarget(overworld,tpPos, Vec3d.ZERO,winner.getYaw(),winner.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
            winner.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.MASTER, 0.5f, 1.2f);
            ParticleEffectUtils.netherRiftTeleport(overworld,winner.getPos(),0);
         }));
      }
      Nations.addTickTimerCallback(new GenericTimer(20*5, () -> {
         teardownDuel(capPos,contestWorld);
         record.returnThings();
      }));
   }
   
   public static boolean isWarActive(){
      return warActive;
   }
   
   public static Contest startContest(MinecraftServer server, CapturePoint cap, ServerPlayerEntity attacker, ServerPlayerEntity defender){
      ServerWorld contestWorld = server.getWorld(NationsRegistry.CONTEST_DIM);
      ServerWorld overworld = server.getOverworld();
      Contest record = new Contest(cap, attacker.getUuid(), defender.getUuid());
      ChunkPos capPos = cap.getChunkPos();
      PENDING_CONTESTS.remove(cap);
      ACTIVE_CONTESTS.add(record);
      setupDuel(server,cap);
      
      sendPrepTeleportMessage(attacker,defender);
      record.removeAttackerThings(attacker);
      record.removeDefenderThings(defender);
      
      Nations.addTickTimerCallback(new GenericTimer(20*11, () -> {
         ChunkPos corner1 = new ChunkPos(capPos.x-DUEL_RANGE,capPos.z-DUEL_RANGE);
         ChunkPos corner2 = new ChunkPos(capPos.x+DUEL_RANGE,capPos.z+DUEL_RANGE);
         BlockPos pos1 = corner1.getBlockPos(8,0,8);
         BlockPos pos2 = corner2.getBlockPos(8,0,8);
         Vec3d defenderPos = cap.getBeaconPos().toCenterPos().add(0,2,0);
         int y1 = SpawnPile.getSurfaceY(contestWorld,contestWorld.getLogicalHeight()-5,pos1.getX(),pos1.getZ());
         int y2 = SpawnPile.getSurfaceY(contestWorld,contestWorld.getLogicalHeight()-5,pos2.getX(),pos2.getZ());
         Vec3d attackPos = attacker.getRandom().nextBoolean() ? pos1.toCenterPos().add(0,y1,0) : pos2.toCenterPos().add(0,y2,0);
         
         attacker.teleportTo(new TeleportTarget(contestWorld,attackPos, Vec3d.ZERO,attacker.getYaw(),attacker.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
         attacker.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.MASTER, 0.5f, 1.2f);
         ParticleEffectUtils.netherRiftTeleport(contestWorld,attacker.getPos(),0);
         
         defender.teleportTo(new TeleportTarget(contestWorld,defenderPos, Vec3d.ZERO,defender.getYaw(),defender.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
         defender.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.MASTER, 0.5f, 1.2f);
         ParticleEffectUtils.netherRiftTeleport(contestWorld,defender.getPos(),0);
      }));
      
      Nations.addTickTimerCallback(new GenericTimer(20*12, () -> {
         record.setBegun(true);
      }));
      return record;
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
      
      Nations.addTickTimerCallback(new GenericTimer(6, () -> {
         for(Pair<BlockPos, BlockPos> duelCorner : getDuelCorners(capPos, contestWorld)){
            for(BlockPos blockPos : BlockPos.iterate(duelCorner.getLeft(), duelCorner.getRight())){
               contestWorld.setBlockState(blockPos,NationsRegistry.CONTEST_BOUNDARY_BLOCK.getDefaultState(),Block.FORCE_STATE+Block.SKIP_DROPS+Block.REDRAW_ON_MAIN_THREAD);
            }
         }
      }));
      
      Nations.addTickTimerCallback(new GenericTimer(10, () -> {
         for(int x = -DUEL_RANGE; x <= DUEL_RANGE; x++){
            for(int z = -DUEL_RANGE; z <= DUEL_RANGE; z++){
               ChunkPos copyChunk = new ChunkPos(capPos.x+x,capPos.z+z);
               copyChunk(copyChunk,overworld,contestWorld);
            }
         }
      }));
      
      
      Nations.addTickTimerCallback(new GenericTimer(15, () -> {
         BlockPos beaconPos = cap.getBeaconPos();
         for(int x = -1; x <= 1; x++){
            for(int z = -1; z <= 1; z++){
               contestWorld.setBlockState(beaconPos.add(x,0,z),NationsRegistry.CONTEST_BOUNDARY_BLOCK.getDefaultState());
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
         if(blockState.hasBlockEntity() || blockState.isIn(NationsRegistry.DUEL_NO_COPY_BLOCKS)) continue;
         contestWorld.setBlockState(blockPos,blockState, Block.FORCE_STATE+Block.SKIP_DROPS+Block.REDRAW_ON_MAIN_THREAD);
      }
   }
   
   private static List<ItemStack> removeIllegalThings(ServerPlayerEntity player){
      StatusEffectInstance res = player.getStatusEffect(StatusEffects.RESISTANCE);
      if(res != null && res.getAmplifier() > 0){
         player.removeStatusEffect(StatusEffects.RESISTANCE);
         player.addStatusEffect(new StatusEffectInstance(res.getEffectType(),res.getDuration(),0,res.isAmbient(),res.shouldShowParticles(),res.shouldShowIcon()));
      }
      player.removeStatusEffect(ArcanaRegistry.DEATH_WARD_EFFECT);
      
      ArrayList<ItemStack> removedStacks = new ArrayList<>();
      
      List<Pair<List<ItemStack>,ItemStack>> allItems = net.borisshoes.arcananovum.utils.MiscUtils.getAllItems(player);
      for(int i = 0; i < allItems.size(); i++){
         List<ItemStack> itemList = allItems.get(i).getLeft();
         ItemStack carrier = allItems.get(i).getRight();
         boolean changed = false;
         
         for(int j = 0; j < itemList.size(); j++){
            ItemStack item = itemList.get(j);
            ArcanaItem arcanaItem = ArcanaItemUtils.identifyItem(item);
            
            if(arcanaItem instanceof TotemOfVengeance || arcanaItem instanceof SojournerBoots || arcanaItem instanceof GreavesOfGaialtus ||
                  arcanaItem instanceof NulMemento || arcanaItem instanceof EnsnarementArrows || arcanaItem instanceof ExpulsionArrows ||
                  arcanaItem instanceof ConcussionArrows || arcanaItem instanceof GravitonArrows
            ){
               removedStacks.add(item.copyAndEmpty());
               changed = true;
            }
            
            if(EnhancedStatUtils.isEnhanced(item) && item.contains(DataComponentTypes.EQUIPPABLE)){
               removedStacks.add(item.copyAndEmpty());
               changed = true;
            }
         }
         
         if(changed && ArcanaItemUtils.identifyItem(carrier) instanceof ArcanistsBelt belt){
            belt.buildItemLore(carrier, ArcanaNovum.SERVER);
         }
         if(changed && ArcanaItemUtils.identifyItem(carrier) instanceof QuiverItem quiver){
            quiver.buildItemLore(carrier, ArcanaNovum.SERVER);
         }
      }
      
      return removedStacks;
   }
   
   public static HashSet<Contest> getActiveContests(){
      return ACTIVE_CONTESTS;
   }
   
   public static HashMap<CapturePoint, ServerPlayerEntity> getPendingContests(){
      return PENDING_CONTESTS;
   }
   
   public static class Contest{
      private final CapturePoint capturePoint;
      private final UUID attacker;
      private final UUID defender;
      private int age;
      private boolean begun;
      private Contest proxy;
      private int attackOnCapTicks;
      private CommandBossBar bossBar;
      private final List<ItemStack> attackerItems = new ArrayList<>();
      private final List<ItemStack> defenderItems = new ArrayList<>();
      private SubArchetype attackerArchetype;
      private SubArchetype defenderArchetype;
      private ServerPlayerEntity lastOnlineAttacker;
      private ServerPlayerEntity lastOnlineDefender;
      
      public Contest(CapturePoint capturePoint, UUID attacker, UUID defender){
         this.capturePoint = capturePoint;
         this.attacker = attacker;
         this.defender = defender;
         this.age = 0;
         this.begun = false;
         this.proxy = null;
      }
      
      public void removeAttackerThings(ServerPlayerEntity attackerPlayer){
         attackerItems.addAll(WarManager.removeIllegalThings(attackerPlayer));
         IArchetypeProfile archetypeProfile = AncestralArchetypes.profile(attackerPlayer);
         SubArchetype archetype = archetypeProfile.getSubArchetype();
         if(archetype != null){
            attackerArchetype = archetype;
            archetypeProfile.changeArchetype(null);
         }
      }
      
      public void removeDefenderThings(ServerPlayerEntity defenderPlayer){
         defenderItems.addAll(WarManager.removeIllegalThings(defenderPlayer));
         IArchetypeProfile archetypeProfile = AncestralArchetypes.profile(defenderPlayer);
         SubArchetype archetype = archetypeProfile.getSubArchetype();
         if(archetype != null){
            defenderArchetype = archetype;
            archetypeProfile.changeArchetype(null);
         }
      }
      
      public void tick(MinecraftServer server){
         if(proxy != null) return;
         ServerWorld contestWorld = server.getWorld(NationsRegistry.CONTEST_DIM);
         ServerWorld overworld = server.getOverworld();
         ChunkPos pos = capturePoint().getChunkPos();
         int range = DUEL_RANGE+1;
         BlockPos corner1 = (new ChunkPos(pos.x-range,pos.z-range).getBlockPos(15,contestWorld.getBottomY(),15));
         BlockPos corner2 = (new ChunkPos(pos.x+range,pos.z+range).getBlockPos(0,contestWorld.getLogicalHeight(),0));
         Box bounds = new Box(corner1.toCenterPos(),corner2.toCenterPos());
         Box boundsLeeway = new Box(corner1.toCenterPos(),corner2.toCenterPos()).expand(10);
         
         if(begun){
            ServerPlayerEntity attackerPlayer = server.getPlayerManager().getPlayer(attacker);
            ServerPlayerEntity defenderPlayer = server.getPlayerManager().getPlayer(defender);
            
            if(attackerPlayer != null){
               lastOnlineAttacker = attackerPlayer;
               removeAttackerThings(attackerPlayer);
            }
            if(defenderPlayer != null){
               lastOnlineDefender = defenderPlayer;
               removeDefenderThings(defenderPlayer);
            }
            
            List<UUID> playersLeeway = new ArrayList<>(contestWorld.getPlayers(p -> !p.isSpectator() && !p.isCreative() && p.getBoundingBox().intersects(boundsLeeway)).stream().map(Entity::getUuid).toList());
            List<UUID> players = new ArrayList<>(contestWorld.getPlayers(p -> !p.isSpectator() && !p.isCreative() && p.getBoundingBox().intersects(bounds)).stream().map(Entity::getUuid).toList());
            if(playersLeeway.contains(attacker)){
               if(!players.contains(attacker)){
                  if(attackerPlayer != null){
                     Vec3d nearest = MiscUtils.closestPointOnBoxSurface(bounds,attackerPlayer.getPos());
                     Vec3d diff = nearest.subtract(attackerPlayer.getPos());
                     Vec3d newPos = diff.normalize().multiply(diff.length()+2).add(attackerPlayer.getPos());
                     attackerPlayer.teleportTo(new TeleportTarget(contestWorld,newPos, Vec3d.ZERO,attackerPlayer.getYaw(),attackerPlayer.getPitch(),TeleportTarget.NO_OP));
                  }
               }
            }else if(attackerPlayer != null){ // Defender victory
               attackerPlayer.damage(contestWorld, ArcanaDamageTypes.of(contestWorld,NationsRegistry.CONTEST_DAMAGE,defenderPlayer),attackerPlayer.getHealth()*100);
            }
            
            if(playersLeeway.contains(defender)){
               if(!players.contains(defender)){
                  if(defenderPlayer != null){
                     Vec3d nearest = MiscUtils.closestPointOnBoxSurface(bounds,defenderPlayer.getPos());
                     Vec3d diff = nearest.subtract(defenderPlayer.getPos());
                     Vec3d newPos = diff.normalize().multiply(diff.length()+2).add(defenderPlayer.getPos());
                     defenderPlayer.teleportTo(new TeleportTarget(contestWorld,newPos, Vec3d.ZERO,defenderPlayer.getYaw(),defenderPlayer.getPitch(),TeleportTarget.NO_OP));
                  }
               }
            }else if(defenderPlayer != null){ // Attacker victory
               defenderPlayer.damage(contestWorld, ArcanaDamageTypes.of(contestWorld,NationsRegistry.CONTEST_DAMAGE,attackerPlayer),defenderPlayer.getHealth()*100);
            }
            
            for(UUID playerId : players){ // Interlopers
               if(playerId.equals(attacker) || playerId.equals(defender)) continue;
               ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
               if(player == null) continue;
               player.teleportTo(new TeleportTarget(overworld,player.getPos(), Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
               player.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.MASTER, 0.5f, 1.2f);
               ParticleEffectUtils.netherRiftTeleport(overworld,player.getPos(),0);
            }
            
            Vec3d center = capturePoint.getBeaconPos().toCenterPos().add(0,3,0);
            float centerRadius = 5;
            Formatting textColor = Formatting.GOLD;
            int particleColor = 0xff9000;
            float size = 0.5f;
            if(attackerPlayer != null && attackerPlayer.squaredDistanceTo(center) <= centerRadius*centerRadius){
               if(defenderPlayer == null || !(defenderPlayer.squaredDistanceTo(center) <= centerRadius*centerRadius)){
                  attackOnCapTicks++;
                  textColor = Formatting.GREEN;
                  particleColor = 0x00d71e;
                  size = 0.75f;
               }
            }else if(attackOnCapTicks > 0){
               if(age % NationsConfig.getInt(NationsRegistry.WAR_CAPTURE_PROGRESS_REGRESSION_RATE_CFG) == 0){
                  attackOnCapTicks--;
               }
               textColor = Formatting.RED;
               particleColor = 0xc41717;
               size = 0.75f;
            }
            
            int capDuration = NationsConfig.getInt(NationsRegistry.WAR_ATTACK_CAPTURE_DURATION_CFG); // seconds
            int contestDuration = NationsConfig.getInt(NationsRegistry.WAR_CONTEST_DURATION_CFG); // minutes
            if(age > (contestDuration * 1200)){ // Timeout - Defender Victory
               if(attackerPlayer != null){
                  attackerPlayer.damage(contestWorld, ArcanaDamageTypes.of(contestWorld,NationsRegistry.CONTEST_DAMAGE,attackerPlayer),attackerPlayer.getHealth()*100);
               }else{
                  LOGOUT_TRACKER.remove(attacker);
                  PlayerConnectionCallback.onCombatLog(attacker,server);
               }
            }
            if(attackOnCapTicks > (capDuration * 20)){ // Capture - Attacker Victory
               if(defenderPlayer != null){
                  defenderPlayer.damage(contestWorld, ArcanaDamageTypes.of(contestWorld,NationsRegistry.CONTEST_DAMAGE,defenderPlayer),defenderPlayer.getHealth()*100);
               }else{
                  LOGOUT_TRACKER.remove(defender);
                  PlayerConnectionCallback.onCombatLog(defender,server);
               }
            }
            
            
            
            if(attackOnCapTicks > 0){
               char[] blocks = {'▁', '▂', '▃', '▅', '▆', '▇', '▌'};
               double percentage = (double) attackOnCapTicks / (capDuration * 20.0);
               StringBuilder message = new StringBuilder();
               int columns = 10;
               for (int i = 0; i < columns; i++) {
                  double columnFill = (percentage * columns) - i;
                  if (columnFill <= 0.0) {
                     message.append(blocks[0]);
                  } else if (columnFill >= 1.0) {
                     message.append(blocks[blocks.length - 1]);
                  } else {
                     int idx = (int) Math.round(columnFill * (blocks.length - 1));
                     message.append(blocks[idx]);
                  }
               }
               Text captureText = Text.translatable("text.nations.capture_progress",Text.literal(message.toString()).formatted(textColor)).formatted(Formatting.BOLD,textColor);
               if(attackerPlayer != null){
                  attackerPlayer.sendMessage(captureText,true);
               }
               if(defenderPlayer != null){
                  defenderPlayer.sendMessage(captureText,true);
               }
               
               double theta = Math.PI * 2 * attackOnCapTicks / 200;
               ParticleEffectUtils.sphere(contestWorld,null,center,new DustParticleEffect(particleColor,size),centerRadius,(int)(5*centerRadius*centerRadius),1,0.1,1,theta);
            }
            
            bossBar.setName(getBossbarText());
            float percentage = (float) age / (contestDuration * 1200.0f);
            bossBar.setPercent(1-percentage);
            
            age++;
         }
      }
      
      public void returnThings(){
         if(lastOnlineAttacker != null){
            for(ItemStack attackerItem : attackerItems){
               ArcanaNovum.addTickTimerCallback(new ItemReturnTimerCallback(attackerItem,lastOnlineAttacker));
            }
            IArchetypeProfile attackerProfile = AncestralArchetypes.profile(lastOnlineAttacker);
            attackerProfile.changeArchetype(attackerArchetype);
         }
         if(lastOnlineDefender != null){
            for(ItemStack defenderItem : defenderItems){
               ArcanaNovum.addTickTimerCallback(new ItemReturnTimerCallback(defenderItem,lastOnlineDefender));
            }
            IArchetypeProfile defenderProfile = AncestralArchetypes.profile(lastOnlineDefender);
            defenderProfile.changeArchetype(defenderArchetype);
         }
      }
      
      public void setProxy(Contest main){
         this.proxy = main;
      }
      
      public void setBegun(boolean begun){
         this.begun = begun;
         if(!isProxy()){
            this.bossBar = SERVER.getBossBarManager().add(Identifier.of(MOD_ID,"contest."+capturePoint.getId().toString()), getBossbarText());
            bossBar.setColor(BossBar.Color.RED);
            bossBar.setStyle(BossBar.Style.PROGRESS);
            bossBar.setPercent(100);
            ServerPlayerEntity attackerPlayer = SERVER.getPlayerManager().getPlayer(attacker);
            ServerPlayerEntity defenderPlayer = SERVER.getPlayerManager().getPlayer(defender);
            bossBar.addPlayer(attackerPlayer);
            bossBar.addPlayer(defenderPlayer);
         }
      }
      
      public Contest getProxy(){
         return proxy;
      }
      
      public boolean isProxy(){
         return proxy != null;
      }
      
      public CapturePoint capturePoint(){
         return capturePoint;
      }
      
      public UUID attacker(){
         return attacker;
      }
      
      public UUID defender(){
         return defender;
      }
      
      public int getAge(){
         return age;
      }
      
      public boolean hasBegun(){
         return begun;
      }
      
      private MutableText getBossbarText(){
         int contestDuration = NationsConfig.getInt(NationsRegistry.WAR_CONTEST_DURATION_CFG); // minutes
         long durMillis = contestDuration * 60000L; // millis
         long ageMillis = age*50L;
         return Text.translatable("text.nations.contest_duration_bar",MiscUtils.getTimeDiff(durMillis-ageMillis).formatted(Formatting.GOLD)).formatted(Formatting.RED);
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
