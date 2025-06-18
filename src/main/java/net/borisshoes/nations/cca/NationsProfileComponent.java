package net.borisshoes.nations.cca;

import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.ChatChannel;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.utils.NationsColors;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;

public class NationsProfileComponent implements INationsProfileComponent{
   private final PlayerEntity player;
   private String nickname = "";
   private String nationId = "";
   private ChatChannel channel = ChatChannel.GLOBAL;
   private boolean bypassClaims = false;
   private BlockPos riftReturnPos = null;
   private long lastOnline;
   private long lastLoginBonus;
   private String lastTerritory = "";
   private int titleCooldown = 0;
   private boolean trespassAlerts = true;
   private int combatLog = 0;
   private String combatLogPlayerId = "";
   
   public NationsProfileComponent(PlayerEntity player){
      this.player = player;
   }
   
   @Override
   public void readFromNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup){
      nationId = nbtCompound.getString("nation");
      channel = ChatChannel.byName(nbtCompound.getString("channel"),ChatChannel.GLOBAL);
      nickname = nbtCompound.getString("nickname");
      bypassClaims = nbtCompound.getBoolean("bypassClaims");
      trespassAlerts = nbtCompound.getBoolean("trespassAlerts");
      lastOnline = nbtCompound.getLong("lastOnline");
      lastLoginBonus = nbtCompound.getLong("lastLoginBonus");
      lastTerritory = nbtCompound.getString("lastTerritory");
      titleCooldown = nbtCompound.getInt("titleCooldown");
      combatLog = nbtCompound.getInt("combatLog");
      combatLogPlayerId = nbtCompound.getString("combatLogPlayerId");
      
      if(nbtCompound.contains("riftReturnPos")){
         NbtCompound riftPos = nbtCompound.getCompound("riftReturnPos");
         riftReturnPos = new BlockPos(riftPos.getInt("x"),riftPos.getInt("y"),riftPos.getInt("z"));
      }
   }
   
   @Override
   public void writeToNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup){
      nbtCompound.putString("nation",nationId);
      nbtCompound.putString("channel", channel.asString());
      nbtCompound.putString("nickname", nickname);
      nbtCompound.putBoolean("bypassClaims",bypassClaims);
      nbtCompound.putBoolean("trespassAlerts",trespassAlerts);
      nbtCompound.putLong("lastOnline",lastOnline);
      nbtCompound.putLong("lastLoginBonus",lastLoginBonus);
      nbtCompound.putString("lastTerritory",lastTerritory);
      nbtCompound.putInt("titleCooldown",titleCooldown);
      nbtCompound.putInt("combatLog",combatLog);
      nbtCompound.putString("combatLogPlayerId",combatLogPlayerId);
      
      if(riftReturnPos != null){
         NbtCompound riftPos = new NbtCompound();
         riftPos.putInt("x",riftReturnPos.getX());
         riftPos.putInt("y",riftReturnPos.getY());
         riftPos.putInt("z",riftReturnPos.getZ());
         nbtCompound.put("riftReturnPos",riftPos);
      }
   }
   
   @Override
   public Nation getNation(){
      return Nations.getNation(nationId);
   }
   
   @Override
   public boolean bypassesClaims(){
      return bypassClaims;
   }
   
   @Override
   public ChatChannel getChannel(){
      return this.channel;
   }
   
   @Override
   public void setNation(Nation nation){
      if(nation == null){
         this.nationId = "";
      }else{
         this.nationId = nation.getId();
      }
   }
   
   @Override
   public void setClaimBypass(boolean bypass){
      bypassClaims = bypass;
   }
   
   @Override
   public void setChannel(ChatChannel channel){
      this.channel = channel;
   }
   
   @Override
   public String getNickname(){
      return nickname;
   }
   
   @Override
   public void setNickname(String nickname){
      this.nickname = nickname == null ? "" : nickname;
   }
   
   @Override
   public BlockPos getRiftReturnPos(){
      return riftReturnPos;
   }
   
   @Override
   public void setRiftReturnPos(BlockPos riftReturnPos){
      this.riftReturnPos = riftReturnPos;
   }
   
   @Override
   public long getLastOnline(){
      return lastOnline;
   }
   
   @Override
   public void setLastOnline(long lastOnline){
      this.lastOnline = lastOnline;
   }
   
   @Override
   public long lastLoginBonus(){
      return this.lastLoginBonus;
   }
   
   @Override
   public boolean trespassAlerts(){
      return trespassAlerts;
   }
   
   @Override
   public void toggleTrespassAlerts(){
      this.trespassAlerts = !trespassAlerts;
   }
   
   @Override
   public void setLastLoginBonus(long lastLoginBonus){
      this.lastLoginBonus = lastLoginBonus;
   }
   
   @Override
   public String lastTerritory(){
      return lastTerritory;
   }
   
   @Override
   public int titleCooldown(){
      return titleCooldown;
   }
   
   @Override
   public void setLastTerritory(String lastTerritory){
      this.lastTerritory = lastTerritory;
   }
   
   @Override
   public void resetTitleCooldown(){
      this.titleCooldown = 20;
   }
   
   @Override
   public void tick(){
      if(this.titleCooldown > 0){
         titleCooldown--;
      }
      if(this.combatLog > 0){
         combatLog--;
         
         if(combatLog == 0){
            player.sendMessage(Text.translatable("text.nations.combat_exit").formatted(Formatting.GREEN),false);
         }
      }
   }
   
   public void removeCombatLog(){
      this.combatLog = 0;
   }
   
   @Override
   public int getCombatLog(){
      return combatLog;
   }
   
   @Override
   public String getCombatLogPlayerId(){
      return combatLogPlayerId;
   }
   
   @Override
   public void resetCombatLog(PlayerEntity attacker){
      if(player.isCreative() || player.isSpectator() || attacker.equals(player)) return;
      boolean inCombat = this.combatLog > 0;
      int duration = NationsConfig.getInt(NationsRegistry.COMBAT_LOG_DURATION_CFG);
      this.combatLog = duration * 20;
      this.combatLogPlayerId = attacker.getUuidAsString();
      if(!inCombat && combatLog > 0){
         player.sendMessage(Text.translatable("text.nations.combat_warning",duration).formatted(Formatting.BOLD,Formatting.RED),false);
      }
   }
   
   public void addPlayerTeam(MinecraftServer server){
      Nation nation = Nations.getNation(nationId);
      if(nation != null){
         Team team = server.getScoreboard().addTeam("nations."+player.getUuidAsString());
         team.setDisplayName(nation.getFormattedName());
         team.setColor(NationsColors.getClosestFormatting(nation.getTextColorSub()));
         team.setPrefix(nation.getFormattedNameTag(true));
         server.getScoreboard().addScoreHolderToTeam(player.getNameForScoreboard(),team);
      }
   }
   
   public void removePlayerTeam(MinecraftServer server){
      for(Team team : server.getScoreboard().getTeams()){
         if(team.getName().contains("nations") && team.getName().contains(player.getUuidAsString())){
            server.getScoreboard().removeTeam(team);
         }
      }
   }
}
