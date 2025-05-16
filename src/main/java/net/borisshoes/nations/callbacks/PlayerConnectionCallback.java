package net.borisshoes.nations.callbacks;

import net.borisshoes.arcananovum.damage.ArcanaDamageTypes;
import net.borisshoes.arcananovum.utils.SpawnPile;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.borisshoes.nations.gameplay.WarManager;
import net.borisshoes.nations.utils.GenericTimer;
import net.borisshoes.nations.utils.ParticleEffectUtils;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

public class PlayerConnectionCallback {
   
   public static void onPlayerJoin(ServerPlayNetworkHandler netHandler, PacketSender sender, MinecraftServer server) {
      ServerPlayerEntity player = netHandler.getPlayer();
      INationsProfileComponent profile = Nations.getPlayer(player);
      profile.addPlayerTeam(server);
      long lastOnline = profile.getLastOnline();
      long lastLoginBonus = profile.lastLoginBonus();
      long now = System.currentTimeMillis();
      if((now - lastLoginBonus) > 86400000 && profile.getNation() != null){
         profile.getNation().addVictoryPoints(NationsConfig.getInt(NationsRegistry.VICTORY_POINTS_LOGIN_CFG));
         Nations.addTickTimerCallback( new GenericTimer(100, () -> player.sendMessage(Text.translatable("text.nations.login_bonus").formatted(Formatting.GOLD))));
         profile.setLastLoginBonus(now);
      }
      profile.setLastOnline(now);
      
      if(profile.getRiftReturnPos() != null && player.getWorld().getRegistryKey().equals(World.NETHER) && (Nations.LAST_RIFT == null || !Nations.LAST_RIFT.isActive())){
         Nations.addTickTimerCallback( new GenericTimer(10, () -> {
            player.teleportTo(new TeleportTarget(server.getOverworld(), SpawnPile.makeSpawnLocations(1,10,server.getOverworld(),profile.getRiftReturnPos()).getFirst().toCenterPos(), Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
            player.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.MASTER, 0.5f, 1.2f);
            ParticleEffectUtils.netherRiftTeleport(server.getOverworld(),player.getPos(),0);
         }));
      }else if(player.getWorld().getRegistryKey().equals(NationsRegistry.CONTEST_DIM)){
         Vec3d playerPos = player.getPos().add(0,1,0);
         Nations.addTickTimerCallback( new GenericTimer(10, () -> {
            player.teleportTo(new TeleportTarget(server.getOverworld(), playerPos, Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
            player.playSoundToPlayer(SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.MASTER, 0.5f, 1.2f);
            ParticleEffectUtils.netherRiftTeleport(server.getOverworld(),player.getPos(),0);
         }));
      }
   }
   
   public static void onPlayerLeave(ServerPlayNetworkHandler handler, MinecraftServer server) {
      ServerPlayerEntity player = handler.getPlayer();
      INationsProfileComponent profile = Nations.getPlayer(player);
      profile.removePlayerTeam(server);
      profile.setLastOnline(System.currentTimeMillis());
      
      for(WarManager.Contest contest : WarManager.getActiveContests()){
         if(contest.attacker().equals(player)){
            player.damage(player.getServerWorld(), ArcanaDamageTypes.of(player.getServerWorld(),NationsRegistry.CONTEST_DAMAGE, contest.defender(), contest.defender()),player.getHealth()*100);
         }else if(contest.defender().equals(player)){
            player.damage(player.getServerWorld(), ArcanaDamageTypes.of(player.getServerWorld(),NationsRegistry.CONTEST_DAMAGE, contest.attacker(), contest.attacker()),player.getHealth()*100);
         }
      }
   }
}
