package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.borisshoes.nations.gameplay.CapturePoint;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.WarManager;
import net.borisshoes.nations.land.NationsLand;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
   
   @ModifyReturnValue(method = "getPlayerListName", at = @At("RETURN"))
   private Text nations_replaceListName(Text original){
      ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
      INationsProfileComponent data = Nations.getPlayer(player);
      Nation nation = data.getNation();
      String nick = data.getNickname();
      nick = nick.isBlank() ? (original == null ? null : original.getString()) : nick;
      
      if(nation == null){
         if(original == null){
            return nick == null ? null : Text.literal(nick);
         }else{
            return Text.literal(nick).setStyle(original.getStyle());
         }
      }else{
         if(nick == null) return nation.getFormattedNameTag(true).append(Text.empty().append(player.getName()).withColor(nation.getTextColorSub()));
         return nation.getFormattedNameTag(true).append(Text.literal(nick).withColor(nation.getTextColorSub()));
      }
   }
   
   @ModifyExpressionValue(method = "copyFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameRules;getBoolean(Lnet/minecraft/world/GameRules$Key;)Z"))
   private boolean nations_keepInventoryCopyFrom(boolean original, ServerPlayerEntity oldPlayer, boolean alive){
      if(original) return true;
      return NationsLand.shouldKeepInventory(oldPlayer.getServerWorld().getRegistryKey(),new ChunkPos(oldPlayer.getBlockPos()),oldPlayer);
   }
   
   
   @ModifyReturnValue(method = "getRespawnTarget", at = @At("RETURN"))
   private TeleportTarget nations_respawnAtMonument(TeleportTarget original){
      ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
      INationsProfileComponent data = Nations.getPlayer(player);
      Nation nation = data.getNation();
      if(original.world().getRegistryKey().equals(ServerWorld.OVERWORLD)){
         if(original.position().distanceTo(Vec3d.of(player.getWorldSpawnPos(original.world(), original.world().getSpawnPos()))) < 15.0){
            if(nation != null && nation.isFounded()){
               return new TeleportTarget(original.world(), data.getNation().getHologramPos(), Vec3d.ZERO, 0.0F, 0.0F, true, false, Set.of(), original.postTeleportTransition());
            }
         }
      }
      return original;
   }
   
   
   @Inject(method = "onDeath", at = @At(value = "HEAD"))
   private void nations_contestDeath(DamageSource damageSource, CallbackInfo ci){
      ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
      INationsProfileComponent profile = Nations.getPlayer(player);
      
      if(!NationsLand.shouldKeepInventory(player.getWorld().getRegistryKey(),new ChunkPos(BlockPos.ofFloored(player.getPos())),player)){
         if(profile.getNation() != null){
            profile.getNation().addVictoryPoints(-NationsConfig.getInt(NationsRegistry.VICTORY_POINTS_DEATH_PENALTY_CFG));
         }
         if(damageSource.getAttacker() instanceof ServerPlayerEntity otherPlayer){
            INationsProfileComponent otherProfile = Nations.getPlayer(otherPlayer);
            if(otherProfile.getNation() != null){
               otherProfile.getNation().addVictoryPoints(NationsConfig.getInt(NationsRegistry.VICTORY_POINTS_KILL_CFG));
            }
         }
      }
      
      for(Map.Entry<CapturePoint, ServerPlayerEntity> entry : WarManager.getPendingContests().entrySet()){
         if(entry.getValue().equals(player)){
            // TODO cancel
         }
      }
      
      WarManager.Contest completedContest = null;
      ServerPlayerEntity winner = null;
      for(WarManager.Contest contest : WarManager.getActiveContests()){
         ServerPlayerEntity attacker = contest.attacker();
         ServerPlayerEntity defender = contest.defender();
         if(!player.equals(attacker) && !player.equals(defender)) continue;
         Vec3d tpPos = contest.capturePoint().getBeaconPos().toCenterPos().add(0,2,0);
         player.teleportTo(new TeleportTarget(player.getServer().getOverworld(),tpPos,Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.NO_OP));
         completedContest = contest;
         winner = player.equals(attacker) ? contest.defender() : contest.attacker();
      }
      
      if(completedContest != null){
         WarManager.concludeContest(player.getServer(),completedContest,winner);
      }
   }
}
