package net.borisshoes.nations.cca;

import net.borisshoes.nations.gameplay.ChatChannel;
import net.borisshoes.nations.gameplay.Nation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.ladysnake.cca.api.v3.component.ComponentV3;

public interface INationsProfileComponent extends ComponentV3 {
   Nation getNation();
   boolean bypassesClaims();
   ChatChannel getChannel();
   String getNickname();
   BlockPos getRiftReturnPos();
   long getLastOnline();
   long lastLoginBonus();
   String lastTerritory();
   int titleCooldown();
   void resetTitleCooldown();
   boolean trespassAlerts();
   int getCombatLog();
   String getCombatLogPlayerId();
   
   void setNation(Nation nation);
   void setClaimBypass(boolean bypass);
   void setChannel(ChatChannel channel);
   void setNickname(String nickname);
   void setRiftReturnPos(BlockPos pos);
   void addPlayerTeam(MinecraftServer server);
   void removePlayerTeam(MinecraftServer server);
   void setLastOnline(long time);
   void setLastLoginBonus(long time);
   void setLastTerritory(String lastTerritory);
   void tick();
   void toggleTrespassAlerts();
   void resetCombatLog(PlayerEntity player);
   void removeCombatLog();
}
