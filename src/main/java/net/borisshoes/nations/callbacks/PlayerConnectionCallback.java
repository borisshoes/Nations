package net.borisshoes.nations.callbacks;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Dynamic;
import net.borisshoes.arcananovum.ArcanaNovum;
import net.borisshoes.arcananovum.ArcanaRegistry;
import net.borisshoes.arcananovum.cardinalcomponents.IArcanaProfileComponent;
import net.borisshoes.arcananovum.core.ArcanaItem;
import net.borisshoes.arcananovum.damage.ArcanaDamageTypes;
import net.borisshoes.arcananovum.utils.SpawnPile;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.WarManager;
import net.borisshoes.nations.land.NationsLand;
import net.borisshoes.nations.mixins.LivingEntityAccessor;
import net.borisshoes.nations.mixins.PlayerEntityAccessor;
import net.borisshoes.nations.mixins.PlayerManagerAccessor;
import net.borisshoes.nations.utils.GenericTimer;
import net.borisshoes.nations.utils.MiscUtils;
import net.borisshoes.nations.utils.ParticleEffectUtils;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.dimension.DimensionType;

import java.util.*;

import static net.borisshoes.nations.Nations.BLANK_UUID;
import static net.borisshoes.nations.Nations.LOGOUT_TRACKER;

public class PlayerConnectionCallback {
   
   public static void onPlayerJoin(ServerPlayNetworkHandler netHandler, PacketSender sender, MinecraftServer server) {
      ServerPlayerEntity player = netHandler.getPlayer();
      INationsProfileComponent profile = Nations.getPlayer(player);
      Nation playerNation = profile.getNation();
      profile.addPlayerTeam(server);
      long lastOnline = profile.getLastOnline();
      long lastLoginBonus = profile.lastLoginBonus();
      long now = System.currentTimeMillis();
      if((now - lastLoginBonus) > 86400000 && playerNation != null){
         playerNation.addVictoryPoints(NationsConfig.getInt(NationsRegistry.VICTORY_POINTS_LOGIN_CFG));
         Nations.addTickTimerCallback( new GenericTimer(100, () -> player.sendMessage(Text.translatable("text.nations.login_bonus").formatted(Formatting.GOLD))));
         profile.setLastLoginBonus(now);
      }
      profile.setLastOnline(now);
      
      IArcanaProfileComponent arcanaProfile = ArcanaNovum.data(player);
      if(playerNation != null){
         for(ArcanaItem arcanaItem : ArcanaRegistry.ARCANA_ITEMS){
            if(playerNation.canCraft(arcanaItem) || arcanaItem.getId().equals(ArcanaRegistry.ARCANE_TOME.getId())){
               arcanaProfile.addResearchedItem(arcanaItem.getId());
            }else{
               arcanaProfile.removeResearchedItem(arcanaItem.getId());
            }
         }
      }
      
      boolean teleported = false;
      if(profile.getRiftReturnPos() != null && player.getWorld().getRegistryKey().equals(World.NETHER) && (Nations.LAST_RIFT == null || !Nations.LAST_RIFT.isActive())){
         Nations.addTickTimerCallback( new GenericTimer(10, () -> {
            player.teleportTo(new TeleportTarget(server.getOverworld(), SpawnPile.makeSpawnLocations(1,10,server.getOverworld(),profile.getRiftReturnPos()).getFirst().toCenterPos(), Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
            player.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.MASTER, 0.5f, 1.2f);
            ParticleEffectUtils.netherRiftTeleport(server.getOverworld(),player.getPos(),0);
         }));
         teleported = true;
      }else if(player.getWorld().getRegistryKey().equals(NationsRegistry.CONTEST_DIM)){
         boolean inContest = WarManager.getActiveContests().stream().anyMatch(contest -> contest.hasBegun() && (contest.defender().equals(player.getUuid()) || contest.attacker().equals(player.getUuid())));
         if(!inContest){
            Vec3d playerPos = player.getPos().add(0,1,0);
            Nations.addTickTimerCallback( new GenericTimer(10, () -> {
               player.teleportTo(new TeleportTarget(server.getOverworld(), playerPos, Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
               player.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.MASTER, 0.5f, 1.2f);
               ParticleEffectUtils.netherRiftTeleport(server.getOverworld(),player.getPos(),0);
            }));
            teleported = true;
         }
      }
      
      UUID killerId = Nations.shouldKillOnRelog(player.getUuid());
      if(killerId != null){
         ServerPlayerEntity killer = server.getPlayerManager().getPlayer(killerId);
         Nations.addTickTimerCallback(new GenericTimer(teleported ? 45 : 20, () -> {
            if(killer != null){
               player.damage(player.getServerWorld(), ArcanaDamageTypes.of(player.getServerWorld(),NationsRegistry.CONTEST_DAMAGE,killer),player.getHealth()*100);
            }else{
               player.damage(player.getServerWorld(), ArcanaDamageTypes.of(player.getServerWorld(),NationsRegistry.CONTEST_DAMAGE),player.getHealth()*100);
            }
            Nations.removeKillOnRelog(player.getUuid());
         }));
      }
      
      LOGOUT_TRACKER.remove(player.getUuid());
   }
   
   public static void onPlayerLeave(ServerPlayNetworkHandler handler, MinecraftServer server) {
      ServerPlayerEntity player = handler.getPlayer();
      INationsProfileComponent profile = Nations.getPlayer(player);
      profile.removePlayerTeam(server);
      profile.setLastOnline(System.currentTimeMillis());
      boolean inCombat = profile.getCombatLog() > 0;
      boolean inContest = WarManager.getActiveContests().stream().anyMatch(contest -> contest.attacker().equals(player.getUuid()) || contest.defender().equals(player.getUuid()));
      
      WarManager.cancelPendingContestsFromPlayer(player);
      
      if(inCombat || inContest){
         int gracePeriod = NationsConfig.getInt(NationsRegistry.COMBAT_LOG_GRACE_PERIOD_CFG);
         if(gracePeriod == 0){
            onCombatLog(player.getUuid(),server);
         }else{
            LOGOUT_TRACKER.put(player.getUuid(),0);
         }
      }
   }
   
   public static void onCombatLog(UUID playerId, MinecraftServer server){
      ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
      if(player == null){ // Offline
         PlayerManager manager = server.getPlayerManager();
         GameProfile profile = server.getUserCache().getByUuid(playerId).orElse(null);
         if(profile == null) return;
         player = server.getPlayerManager().createPlayer(profile, SyncedClientOptions.createDefault());
         Optional<NbtCompound> optional = manager.loadPlayerData(player);
         player.getInventory().readNbt(optional.get().getList("Inventory", NbtElement.COMPOUND_TYPE));;
         RegistryKey<World> registryKey = optional.flatMap(nbt -> DimensionType.worldFromDimensionNbt(new Dynamic<>(NbtOps.INSTANCE, nbt.get("Dimension"))).resultOrPartial()).orElse(World.OVERWORLD);
         ServerWorld serverWorld = manager.getServer().getWorld(registryKey);
         player.setServerWorld(serverWorld);
         
         String combatLogId = Nations.getPlayer(player).getCombatLogPlayerId();
         ServerPlayerEntity killer = server.getPlayerManager().getPlayer(MiscUtils.getUUID(combatLogId));
         
         ((LivingEntityAccessor)player).dropEntityExperience(serverWorld,killer);
         
         boolean keepInventory = NationsLand.shouldKeepInventory(registryKey,player.getChunkPos(),player);
         if(!keepInventory){
            ((PlayerEntityAccessor)player).vanishingCurse();
            
            for(int ind = 0; ind < player.getInventory().size(); ind++){
               ItemStack stack = player.getInventory().getStack(ind);
               if(!stack.isEmpty() && EnchantmentHelper.getLevel(MiscUtils.getEnchantment(ArcanaRegistry.FATE_ANCHOR), stack) <= 0){
                  double d = player.getEyeY() - 0.3F;
                  ItemEntity itemEntity = new ItemEntity(player.getWorld(), player.getX(), d, player.getZ(), stack);
                  itemEntity.setPickupDelay(40);
                  float f = player.getRandom().nextFloat() * 0.1F;
                  float g = player.getRandom().nextFloat() * (float) (Math.PI * 2);
                  itemEntity.setVelocity(-MathHelper.sin(g) * f, 0.2F, MathHelper.cos(g) * f);
                  serverWorld.spawnEntity(itemEntity);
                  player.getInventory().setStack(ind,ItemStack.EMPTY);
               }
            }
            player.setExperienceLevel(0);
            player.setExperiencePoints(0);
         }
         ((PlayerManagerAccessor)manager).savePlayerNbtData(player);
         
         if(killer != null){
            server.getPlayerManager().broadcast(Text.translatable("text.nations.combat_log_expire_player",player.getStyledDisplayName(),killer.getStyledDisplayName()),false);
         }else{
            server.getPlayerManager().broadcast(Text.translatable("text.nations.combat_log_expire",player.getStyledDisplayName()),false);
         }
         Nations.addPlayerKillOnRelog(playerId,MiscUtils.getUUID(combatLogId));
         
         WarManager.Contest completedContest = null;
         UUID winner = null;
         for(WarManager.Contest contest : WarManager.getActiveContests()){
            if(contest.isProxy()) continue;
            UUID attacker = contest.attacker();
            UUID defender = contest.defender();
            if(!player.getUuid().equals(attacker) && !player.getUuid().equals(defender)) continue;
            completedContest = contest;
            winner = player.getUuid().equals(attacker) ? contest.defender() : contest.attacker();
         }
         if(completedContest != null){
            WarManager.concludeContest(server,completedContest,winner);
         }
      }else if(player.isAlive()){
         String combatLogId = Nations.getPlayer(player).getCombatLogPlayerId();
         ServerPlayerEntity killer = server.getPlayerManager().getPlayer(MiscUtils.getUUID(combatLogId));
         if(killer != null){
            player.damage(player.getServerWorld(), ArcanaDamageTypes.of(player.getServerWorld(),NationsRegistry.CONTEST_DAMAGE,killer),player.getHealth()*100);
         }else{
            player.damage(player.getServerWorld(), ArcanaDamageTypes.of(player.getServerWorld(),NationsRegistry.CONTEST_DAMAGE),player.getHealth()*100);
         }
      }
   }
   
   public static void tickLogoutTracker(MinecraftServer server){
      int gracePeriod = NationsConfig.getInt(NationsRegistry.COMBAT_LOG_GRACE_PERIOD_CFG);
      for(UUID player : new HashSet<>(LOGOUT_TRACKER.keySet())){
         int timer = LOGOUT_TRACKER.get(player) + 1;
         if(timer >= gracePeriod){
            LOGOUT_TRACKER.remove(player);
            onCombatLog(player,server);
         }else{
            LOGOUT_TRACKER.put(player,timer);
         }
      }
   }
}
