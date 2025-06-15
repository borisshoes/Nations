package net.borisshoes.nations.gameplay;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gui.CapturePointGui;
import net.borisshoes.nations.integration.DynmapCalls;
import net.borisshoes.nations.items.GraphicalItem;
import net.borisshoes.nations.utils.MiscUtils;
import net.borisshoes.nations.utils.NationsColors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class CapturePoint {
   
   private final ChunkPos chunkPos;
   private final ResourceType type;
   private int yield;
   private String controllingNationId;
   private final UUID id;
   private int y;
   private int storedCoins;
   private long auctionStartTime;
   private HashMap<String,Double> influence = new HashMap<>();
   private ElementHolder hologram;
   private HolderAttachment attachment;
   private int interactCooldown = 0;
   private boolean updateHolo = false;
   private List<Pair<Double,Integer>> yieldModifiers = new ArrayList<>();
   
   public CapturePoint(ChunkPos pos, int y, ResourceType type, int yield){
      this.chunkPos = pos;
      this.y = y;
      this.type = type;
      this.yield = yield;
      this.controllingNationId = null;
      this.id = UUID.randomUUID();
   }
   
   private CapturePoint(ChunkPos chunkPos, int y, ResourceType type, int yield, String controllingNationId, UUID id, int storedCoins, long auctionStartTime, HashMap<String,Double> influence, List<Pair<Double,Integer>> yieldModifiers){
      this.chunkPos = chunkPos;
      this.type = type;
      this.y = y;
      this.yield = yield;
      this.controllingNationId = controllingNationId;
      this.id = id;
      this.storedCoins = storedCoins;
      this.auctionStartTime = auctionStartTime;
      this.influence = influence;
      this.yieldModifiers = yieldModifiers;
   }
   
   public int getRawYield(){
      return this.yield;
   }
   
   public int getYield(){
      return (int) Math.round(yield * getOutputModifier());
   }
   
   public double getOutputModifier(){
      double modifier = 1.0;
      for(Pair<Double, Integer> pair : yieldModifiers){
         modifier *= pair.getLeft();
      }
      return modifier;
   }
   
   public Nation getControllingNation(){
      if(controllingNationId == null) return null;
      return Nations.getNation(controllingNationId);
   }
   
   public void setControllingNationId(String controllingNationId){
      this.controllingNationId = controllingNationId;
   }
   
   public void tick(ServerWorld serverWorld){
      MinecraftServer server = serverWorld.getServer();
      if(server.getTicks() % 20 == 0){
         Vec3d pos = getHologramPos();
         PlayerEntity player = serverWorld.getClosestPlayer(pos.getX(),pos.getY(),pos.getZ(), 32, entity -> !entity.isSpectator());
         if(player != null && hologram == null){
            hologram = getNewHologram(serverWorld);
            attachment = ChunkAttachment.ofTicking(this.hologram,serverWorld,pos);
         }else if(player == null && hologram != null){
            hologram.destroy();
            hologram = null;
         }
      }
      
      if(auctionStartTime != 0){
         long now = System.currentTimeMillis();
         if(now > getAuctionEndTime()){
            endAuction(serverWorld);
         }
         if(server.getTicks() % 20 == 0){
            this.updateHolo = true;
         }
         
         if(server.getTicks() % 2400 == 0){
            DynmapCalls.updateCapturePointMarker(this);
         }
      }
   }
   
   public void hourlyTick(ServerWorld serverWorld){
      if(getControllingNation() != null){
         boolean doCoinYield = NationsConfig.getBoolean(NationsRegistry.CAPTURE_POINT_COIN_GEN_CFG);
         
         if(doCoinYield){
            this.storedCoins += this.getYield() / 24;
         }
      }
      
      List<Pair<Double,Integer>> newModifiers = new ArrayList<>();
      for(Pair<Double, Integer> mod : yieldModifiers){
         if(mod.getRight() > 1){
            newModifiers.add(new Pair<>(mod.getLeft(), mod.getRight()-1));
         }
      }
      this.yieldModifiers = newModifiers;
      this.updateHolo = true;
   }
   
   public void dailyTick(ServerWorld serverWorld){
      if(getControllingNation() != null){
         boolean doCoinYield = NationsConfig.getBoolean(NationsRegistry.CAPTURE_POINT_COIN_GEN_CFG);
         
         if(doCoinYield){
            this.storedCoins += this.getYield() % 24;
         }
         
         if(getYield() != 0) getControllingNation().addVictoryPoints(NationsConfig.getInt(NationsRegistry.VICTORY_POINTS_CAP_CFG));
         this.updateHolo = true;
      }
   }
   
   public void buffOutput(){
      int buffDuration = NationsConfig.getInt(NationsRegistry.WAR_DEFEND_WIN_DURATION);
      double buffModifier = NationsConfig.getDouble(NationsRegistry.WAR_DEFEND_WIN_MULTIPLIER_CFG);
      yieldModifiers.add(new Pair<>(buffModifier,buffDuration));
   }
   
   public void blockadeOutput(){
      int duration = NationsConfig.getInt(NationsRegistry.WAR_BLOCKADE_DURATION_CFG);
      yieldModifiers.add(new Pair<>(0.0,duration));
   }
   
   public void addOutputModifier(double modifier, int duration){
      yieldModifiers.add(new Pair<>(modifier,duration));
   }
   
   public void clearOutputModifier(){
      yieldModifiers.clear();
   }
   
   public void collectCoins(ServerWorld world){
      if(this.storedCoins > 0){
         int sum = storedCoins;
         int maxStackSize = getType().getCoin().getMaxCount();
         Vec3d spawnPos = getHologramPos().add(0,2,0);
         
         while(sum > 1000){
            sum -= 1000;
            ItemStack newStack = new ItemStack(type.getBullion(),1);
            ItemScatterer.spawn(world,spawnPos.x,spawnPos.y,spawnPos.z,newStack);
         }
         while(sum > 0){
            int size = Math.min(sum,world.getRandom().nextBetween((int) Math.min(maxStackSize,storedCoins/32.0), maxStackSize+1));
            sum -= size;
            ItemStack newStack = new ItemStack(type.getCoin(),size);
            ItemScatterer.spawn(world,spawnPos.x,spawnPos.y,spawnPos.z,newStack);
         }
         this.storedCoins = 0;
      }
      this.updateHolo = true;
   }
   
   public long getAuctionStartTime(){
      return auctionStartTime;
   }
   
   public long getAuctionEndTime(){
      return auctionStartTime + (NationsConfig.getInt(NationsRegistry.CAPTURE_POINT_AUCTION_DURATION_CFG)*60L*1000L);
   }
   
   public void endAuction(ServerWorld serverWorld){
      Nation winningNation = getHighestInfluencingNation();
      MutableText announcement = Text.translatable("text.nations.auction_end",
            getType().getText().formatted(Formatting.BOLD),
            Text.translatable("text.nations.capture_point").formatted(Formatting.BOLD,getType().getTextColor()),
            Text.literal(getChunkPos().toString()).formatted(Formatting.YELLOW,Formatting.BOLD),
            winningNation.getFormattedName().formatted(Formatting.BOLD)
      ).formatted(Formatting.DARK_AQUA);
      Nations.announce(announcement);
      auctionStartTime = 0;
      transferOwnership(serverWorld,winningNation);
      this.updateHolo = true;
   }
   
   public void startAuction(){
      MutableText announcement = Text.translatable("text.nations.auction_start",
            getType().getText().formatted(Formatting.BOLD),
            Text.translatable("text.nations.capture_point").formatted(Formatting.BOLD,getType().getTextColor()),
            Text.literal(getChunkPos().toString()).formatted(Formatting.YELLOW,Formatting.BOLD)
            ).formatted(Formatting.DARK_AQUA);
      Nations.announce(announcement);
      auctionStartTime = System.currentTimeMillis();
      DynmapCalls.updateCapturePointMarker(this);
   }
   
   public void cancelAuction(){
      auctionStartTime = 0;
      this.updateHolo = true;
   }
   
   public void addCoins(Nation nation, int coinCount){
      Pair<ChunkPos,Double> nearestInf = calculateNearestInfluence(nation);
      double coinToInfluence = Math.max(0.001,nearestInf.getRight());
      double curInfluence = influence.getOrDefault(nation.getId(),0.0);
      influence.put(nation.getId(),curInfluence+(coinCount*coinToInfluence));
      if(auctionStartTime == 0){
         startAuction();
      }
   }
   
   private Nation getHighestInfluencingNation(){
      List<Nation> stream = influence.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .map(e -> Nations.getNation(e.getKey()))
            .filter(Objects::nonNull).toList();
      return stream.isEmpty() ? null : stream.getFirst();
   }
   
   public Pair<ChunkPos,Double> calculateNearestInfluence(Nation nation){
      ChunkPos closest = null;
      int closestDist = Integer.MAX_VALUE;
      
      for(NationChunk chunk : nation.getChunks()){
         int dist = chunk.getPos().getSquaredDistance(getChunkPos());
         if(dist < closestDist){
            closest = new ChunkPos(chunk.getPos().x, chunk.getPos().z);
            closestDist = dist;
         }
      }
      
      for(CapturePoint cap : Nations.getCapturePoints()){
         if(cap.getControllingNation() != null && cap.getControllingNation().equals(nation)){
            int dist = cap.getChunkPos().getSquaredDistance(getChunkPos());
            if(dist < closestDist){
               closest = new ChunkPos(cap.getChunkPos().x, cap.getChunkPos().z);
               closestDist = dist;
            }
         }
      }
      
      return new Pair<>(closest,calculateInfluenceMod(closestDist));
   }
   
   public static double calculateInfluenceMod(double distSqr){
      int border = NationsConfig.getInt(NationsRegistry.WORLD_BORDER_RADIUS_OVERWORLD_CFG);
      double a = NationsConfig.getDouble(NationsRegistry.CAPTURE_POINT_INFLUENCE_DISTANCE_MOD_CFG);
      double maxDist = 8*border*border;
      return -(distSqr - maxDist) / (a*distSqr + 0.5*maxDist);
   }
   
   public HashMap<String, Double> getInfluence(){
      return influence;
   }
   
   private Vec3d getHologramPos(){
      return this.chunkPos.getBlockPos(8,y+5,8).toCenterPos();
   }
   
   public int calculateAttackCost(Nation attacking){
      double attackCost = NationsConfig.getDouble(NationsRegistry.WAR_ATTACK_COST_CFG);
      if(this.getControllingNation() == null) return (int) (attackCost*getRawYield());
      Nation owner = getControllingNation();
      Pair<ChunkPos,Double> attackerMod = calculateNearestInfluence(attacking);
      Pair<ChunkPos,Double> defenderMod = calculateNearestInfluence(owner);
      if(Nations.getChunk(getChunkPos()).getControllingNation() != null) return (int) (attackCost*getRawYield());
      double costModifier = Math.max(0.25,(defenderMod.getRight() - attackerMod.getRight()) / 2.0 + 1);
      return (int) (costModifier*attackCost*getRawYield());
   }
   
   private void openCapGUI(ServerPlayerEntity player){
      Nation playerNation = Nations.getNation(player);
      if(playerNation == null){
         player.sendMessage(Text.translatable("text.nations.cap_interact_no_nation").formatted(Formatting.RED));
         return;
      }
      if(!playerNation.isFounded()){
         player.sendMessage(Text.translatable("text.nations.nation_not_founded_error").formatted(Formatting.RED));
         return;
      }
      
      CapturePointGui gui;
      if(getControllingNation() == null){
         gui = new CapturePointGui(player,this, CapturePointGui.Mode.AUCTION);
      }else{
         if(getControllingNation().equals(playerNation)){
            if(WarManager.capIsContested(this)){
               gui = new CapturePointGui(player,this, CapturePointGui.Mode.DEFEND);
            }else{
               gui = new CapturePointGui(player,this, CapturePointGui.Mode.COLLECT);
            }
         }else{
            boolean isWar = Nations.isWartime();
            if(!isWar){
               player.sendMessage(Text.translatable("text.nations.cap_interact_wrong_nation").formatted(Formatting.RED));
               return;
            }else{
               boolean canContest = WarManager.canContestCap(this, player);
               if(!canContest){
                  player.sendMessage(Text.translatable("text.nations.war_cannot_contest").formatted(Formatting.RED));
                  return;
               }else{
                  gui = new CapturePointGui(player,this, CapturePointGui.Mode.CONTEST);
               }
            }
         }
      }
      gui.open();
   }
   
   private ElementHolder getNewHologram(ServerWorld serverWorld){
      TextDisplayElement line1 = new TextDisplayElement(Text.translatable("text.nations.capture_point_header",Text.translatable(getType().getTranslation())).formatted(getType().getTextColor(), Formatting.BOLD));
      Text yieldText = getOutputModifier() != 1.0 ? Text.translatable("text.nations.capture_point_yield_modified",getYield(),String.format("%03.2f",getOutputModifier())).formatted(getType().getTextColor()) : Text.translatable("text.nations.capture_point_yield",getYield()).formatted(getType().getTextColor());
      TextDisplayElement line2 = new TextDisplayElement(yieldText);
      ItemDisplayElement icon = new ItemDisplayElement(GraphicalItem.with(getType().getGraphicItem()));
      InteractionElement click = new InteractionElement(new VirtualElement.InteractionHandler(){
         public void click(ServerPlayerEntity player){
            if(interactCooldown == 0){
               if(player.isSneaking()){
                  Nation playerNation = Nations.getNation(player);
                  if(getControllingNation() != null && getControllingNation().equals(playerNation) && storedCoins > 0){
                     collectCoins(player.getServerWorld());
                  }
               }else{
                  openCapGUI(player);
               }
               interactCooldown = 5;
            }
         }
         
         @Override
         public void interact(ServerPlayerEntity player, Hand hand){
            click(player);
         }
         
         @Override
         public void interactAt(ServerPlayerEntity player, Hand hand, Vec3d pos){
            click(player);
         }
         
         @Override
         public void attack(ServerPlayerEntity player){
            click(player);
         }
      });
      click.setSize(1f,1f);
      MutableText controlText = getControllingNation() == null ?
            Text.translatable("text.nations.uncontrolled") :
            Text.translatable("text.nations.controlled_by",getControllingNation().getFormattedName());
      TextDisplayElement controlLine = new TextDisplayElement(controlText);
      
      TextDisplayElement storedLine = new TextDisplayElement(getStoredLineText());
      
      
      ElementHolder holder = new ElementHolder(){
         ServerWorld world = serverWorld;
         private final TextDisplayElement ctrlText = controlLine;
         private final TextDisplayElement line1Text = line1;
         private final TextDisplayElement line2Text = line2;
         private final TextDisplayElement coinsText = storedLine;
         private final ItemDisplayElement iconElem = icon;
         private final InteractionElement clickElem = click;
         private int tickCount = 0;
         
         @Override
         protected void onTick(){
            super.onTick();
            
            tickCount++;
            if(interactCooldown > 0) interactCooldown--;
            
            Vec3d centerPos = getHologramPos();
            PlayerEntity player = serverWorld.getClosestPlayer(centerPos.getX(),centerPos.getY(),centerPos.getZ(), 32, entity -> !entity.isSpectator());
            if(player != null){
               Vec3d offsetDir = player.getPos().subtract(centerPos).multiply(1,0,1).normalize().multiply(0.3);
               
               for(VirtualElement element : this.getElements()){
                  double ey = element.getOffset().y;
                  element.setOffset(offsetDir.add(0,ey,0));
               }
            }
            
            if(tickCount == 200 || updateHolo){
               MutableText controlText = getControllingNation() == null ?
                     Text.translatable("text.nations.uncontrolled") :
                     Text.translatable("text.nations.controlled_by",getControllingNation().getFormattedName());
               Text yieldText = getOutputModifier() != 1.0 ? Text.translatable("text.nations.capture_point_yield_modified",getYield(),String.format("%03.2f",getOutputModifier())).formatted(getType().getTextColor()) : Text.translatable("text.nations.capture_point_yield",getYield()).formatted(getType().getTextColor());
               line2Text.setText(yieldText);
               ctrlText.setText(controlText);
               coinsText.setText(getStoredLineText());
               
               tickCount = 0;
               updateHolo = false;
            }
         }
      };
      line1.setOffset(new Vec3d(0,1.75,0));
      line2.setOffset(new Vec3d(0,1.5,0));
      icon.setOffset(new Vec3d(0,1,0));
      controlLine.setOffset(new Vec3d(0,0.25,0));
      storedLine.setOffset(new Vec3d(0,0,0));
      click.setOffset(new Vec3d(0,0.5,0));
      
      line1.setBillboardMode(DisplayEntity.BillboardMode.VERTICAL);
      line2.setBillboardMode(DisplayEntity.BillboardMode.VERTICAL);
      icon.setBillboardMode(DisplayEntity.BillboardMode.VERTICAL);
      controlLine.setBillboardMode(DisplayEntity.BillboardMode.VERTICAL);
      storedLine.setBillboardMode(DisplayEntity.BillboardMode.VERTICAL);
      storedLine.setLineWidth(1000);
      
      holder.addElement(line1);
      holder.addElement(line2);
      holder.addElement(icon);
      holder.addElement(click);
      holder.addElement(controlLine);
      holder.addElement(storedLine);
      return holder;
   }
   
   private MutableText getStoredLineText(){
      MutableText storedText;
      if(getControllingNation() != null){
         storedText = Text.translatable("text.nations.stored_coins",Text.literal(storedCoins+"").formatted(Formatting.BOLD, getType().getTextColor())).formatted(Formatting.YELLOW);
      }else{
         if(auctionStartTime == 0){
            storedText = Text.empty();
         }else{
            long aucEnd = getAuctionEndTime();
            long now = System.currentTimeMillis();
            long tillEnd = aucEnd - now;
            storedText = Text.translatable("text.nations.cap_auction_active",MiscUtils.getTimeDiff(tillEnd).formatted(Formatting.YELLOW)).formatted(Formatting.RED);
         }
      }
      return storedText;
   }
   
   public void transferOwnership(World world, Nation newOwner){
      if(newOwner == null){
         this.controllingNationId = null;
         this.storedCoins = 0;
         this.auctionStartTime = 0;
         this.influence.clear();
      }else{
         this.controllingNationId = newOwner.getId();
      }
      
      DyeColor newColor = newOwner == null ? DyeColor.WHITE : newOwner.getDyeColor();
      StructurePlacer.Structure structure = Nations.CAPTURE_POINT_STRUCTURES.get(this.type);
      BlockPos origin = chunkPos.getBlockPos(3,this.y,3);
      int[][][] pattern = structure.statePattern();
      for(int i = 0; i < pattern.length; i++){
         for(int j = 0; j < pattern[0].length; j++){
            for(int k = 0; k < pattern[0][0].length; k++){
               if(pattern[i][j][k] == -1) continue;
               BlockPos pos = origin.add(i,j,k);
               BlockState curState = world.getBlockState(pos);
               BlockState newState = NationsColors.redyeBlock(curState, newColor);
               if(curState.isOf(newState.getBlock())) continue;
               world.setBlockState(pos,newState,Block.NOTIFY_ALL_AND_REDRAW);
            }
         }
      }
      
      this.updateHolo = true;
      DynmapCalls.updateCapturePointMarker(this);
   }
   
   public BlockPos getBeaconPos(){
      return chunkPos.getBlockPos(8,y+4,8);
   }
   
   public String getMarkerLabel(){
      String controlled = controllingNationId == null ? "Uncontrolled" : Nations.getNation(controllingNationId).getName();
      if(auctionStartTime != 0){
         Nation highestInfluencingNation = getHighestInfluencingNation();
         long aucEnd = getAuctionEndTime();
         long now = System.currentTimeMillis();
         long tillEnd = aucEnd - now;
         controlled = "Auctioning";
         if(highestInfluencingNation != null){
            controlled += " - "+highestInfluencingNation.getName();
         }
         controlled += " - "+MiscUtils.getTimeDiff(tillEnd).getString();
      }
      return Text.translatable(getType().getTranslation()).getString() +" Capture Point "+getChunkPos().toString()+" (Yield: "+yield+") - " + controlled;
   }
   
   public ResourceType getType(){
      return type;
   }
   
   public int getY(){
      return y;
   }
   
   public UUID getId(){
      return id;
   }
   
   public int getStoredCoins(){
      return storedCoins;
   }
   
   public ChunkPos getChunkPos(){
      return chunkPos;
   }
   
   public NbtCompound getPosNbt(){
      NbtCompound pos = new NbtCompound();
      pos.putInt("x",chunkPos.x);
      pos.putInt("y",y);
      pos.putInt("z",chunkPos.z);
      return pos;
   }
   
   public NbtCompound saveToNbt(NbtCompound compound){
      compound.put("pos",getPosNbt());
      compound.putInt("yield",yield);
      compound.putInt("storedCoins",storedCoins);
      compound.putString("id",id.toString());
      compound.putString("nation",controllingNationId == null ? "" : controllingNationId);
      compound.putString("type",type.asString());
      compound.putLong("auctionTime",auctionStartTime);
      NbtCompound infComp = new NbtCompound();
      influence.forEach(infComp::putDouble);
      compound.put("influence",infComp);
      NbtList mods = new NbtList();
      for(Pair<Double, Integer> pair : yieldModifiers){
         NbtCompound mod = new NbtCompound();
         mod.putInt("duration",pair.getRight());
         mod.putDouble("modifier",pair.getLeft());
         mods.add(mod);
      }
      compound.put("yieldModifiers",mods);
      return compound;
   }
   
   public static CapturePoint loadFromNbt(NbtCompound compound){
      NbtCompound pos = compound.getCompound("pos");
      String nationId = compound.getString("nation");
      HashMap<String,Double> infMap = new HashMap<>();
      if(compound.contains("influence")){
         NbtCompound influence = compound.getCompound("influence");
         for(String key : influence.getKeys()){
            infMap.put(key,influence.getDouble(key));
         }
      }
      List<Pair<Double,Integer>> mods = new ArrayList<>();
      if(compound.contains("yieldModifiers")){
         NbtList modList = compound.getList("yieldModifiers", NbtElement.COMPOUND_TYPE);
         for(NbtElement e : modList){
            NbtCompound comp = (NbtCompound) e;
            mods.add(new Pair<>(comp.getDouble("modifier"),comp.getInt("duration")));
         }
      }
      
      return new CapturePoint(
            new ChunkPos(pos.getInt("x"), pos.getInt("z")),
            pos.getInt("y"),
            ResourceType.byName(compound.getString("type"), null),
            compound.getInt("yield"),
            nationId.isEmpty() ? null : nationId,
            MiscUtils.getUUID(compound.getString("id")),
            compound.getInt("storedCoins"),
            compound.getLong("auctionTime"),
            infMap,
            mods
      );
   }
}
