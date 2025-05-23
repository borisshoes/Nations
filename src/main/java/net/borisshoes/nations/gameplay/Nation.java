package net.borisshoes.nations.gameplay;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import net.borisshoes.arcananovum.ArcanaNovum;
import net.borisshoes.arcananovum.ArcanaRegistry;
import net.borisshoes.arcananovum.cardinalcomponents.IArcanaProfileComponent;
import net.borisshoes.arcananovum.core.ArcanaItem;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.borisshoes.nations.gui.NationGui;
import net.borisshoes.nations.integration.DynmapCalls;
import net.borisshoes.nations.items.GraphicalItem;
import net.borisshoes.nations.land.NationsLand;
import net.borisshoes.nations.research.ResearchTech;
import net.borisshoes.nations.utils.MiscUtils;
import net.borisshoes.nations.utils.NationsColors;
import net.borisshoes.nations.utils.NationsUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;

import static net.borisshoes.nations.Nations.*;

public class Nation {
   
   private final Set<UUID> members;
   private final Set<UUID> executors;
   private final Set<UUID> leaders;
   private final Set<NationChunk> chunks;
   private ChunkPos foundLocation;
   private int foundHeight;
   private final String id;
   private String name;
   private DyeColor dyeColor;
   private int textColor;
   private int textColorSub;
   private final Map<ResearchTech, Integer> techs;
   private final ArrayDeque<ResearchTech> techQueue;
   private int victoryPoints;
   private final Map<ResourceType,Integer> storedCoins;
   private int bankedResearchCoins;
   private final ArrayList<ItemStack> mailbox;
   private ElementHolder hologram;
   private HolderAttachment attachment;
   private int interactCooldown = 0;
   private boolean updateHolo = false;
   
   public Nation(String id, String name){
      this.members = new HashSet<>();
      this.executors = new HashSet<>();
      this.leaders = new HashSet<>();
      this.chunks = new HashSet<>();
      this.foundLocation = null;
      this.foundHeight = 0;
      this.id = id;
      this.name = name;
      this.dyeColor = DyeColor.WHITE;
      this.textColor = Formatting.WHITE.getColorValue();
      this.textColorSub = Formatting.GRAY.getColorValue();
      this.techs = new HashMap<>();
      this.techQueue = new ArrayDeque<>();
      this.storedCoins = new HashMap<>();
      this.bankedResearchCoins = 0;
      this.mailbox = new ArrayList<>();
      
      for(ResourceType value : ResourceType.values()){
         this.storedCoins.put(value,0);
      }
   }
   
   private Nation(Set<UUID> members, Set<UUID> executors, Set<UUID> leaders, Set<NationChunk> chunks, ChunkPos foundLocation, int foundHeight,
                  String id, String name, DyeColor dyeColor, int textColor, int textColorSub,
                  Map<ResearchTech, Integer> techs, ArrayDeque<ResearchTech> techQueue, int victoryPoints,
                  Map<ResourceType,Integer> storedCoins, int bankedResearchCoins, ArrayList<ItemStack> mailbox){
      this.members = members;
      this.executors = executors;
      this.leaders = leaders;
      this.chunks = chunks;
      this.foundLocation = foundLocation;
      this.foundHeight = foundHeight;
      this.id = id;
      this.name = name;
      this.dyeColor = dyeColor;
      this.textColor = textColor;
      this.textColorSub = textColorSub;
      this.techs = techs;
      this.techQueue = techQueue;
      this.victoryPoints = victoryPoints;
      this.storedCoins = storedCoins;
      this.bankedResearchCoins = bankedResearchCoins;
      this.mailbox = mailbox;
   }
   
   public void tick(ServerWorld serverWorld){
      MinecraftServer server = serverWorld.getServer();
      if(!isFounded()) return;
      if(server.getTicks() % 20 == 0){
         Vec3d pos = getHologramPos();
         PlayerEntity player = serverWorld.getClosestPlayer(pos.getX(),pos.getY(),pos.getZ(), 64, entity -> !entity.isSpectator());
         if(player != null && hologram == null){
            hologram = getNewHologram(serverWorld);
            attachment = ChunkAttachment.ofTicking(this.hologram,serverWorld,pos);
         }else if(player == null && hologram != null){
            hologram.destroy();
            hologram = null;
         }
      }
   }
   
   public void hourlyTick(ServerWorld serverWorld){
      if(isFounded()){
         Map<ResourceType,Integer> coinYield = calculateCoinYield(serverWorld);
         coinYield.forEach((key, value) -> this.storedCoins.put(key, this.storedCoins.get(key) + (coinYield.get(key) / 24)));
         
         ResearchTech activeTech = getActiveTech();
         if(activeTech != null){
            int rate = activeTech.getConsumptionRate() / 24;
            int progress = getProgress(activeTech);
            int cost = activeTech.getCost();
            int remaining = cost - progress;
            int maxConsumable = Math.min(remaining,rate);
            int bankConsume = Math.min(maxConsumable,bankedResearchCoins);
            int consumed = bankConsume;
            if(bankConsume < maxConsumable){
               int diff = maxConsumable-bankConsume;
               int stored = this.storedCoins.get(ResourceType.RESEARCH);
               int storeConsume = Math.min(stored,diff);
               consumed += storeConsume;
               this.storedCoins.put(ResourceType.RESEARCH, stored - storeConsume);
            }
            bankedResearchCoins -= bankConsume;
            techs.put(activeTech,progress+consumed);
            
            if(consumed == 0){
               MutableText warning = Text.translatable("text.nations.no_research_budget").formatted(Formatting.BOLD,Formatting.RED);
               for(ServerPlayerEntity player : serverWorld.getServer().getPlayerManager().getPlayerList()){
                  if(hasPlayer(player)){
                     player.sendMessage(warning);
                  }
               }
            }else if(consumed == remaining){
               onTechComplete(activeTech);
            }
         }
         
         this.updateHolo = true;
      }
   }
   
   public void dailyTick(ServerWorld serverWorld){
      if(isFounded()){
         Map<ResourceType,Integer> coinYield = calculateCoinYield(serverWorld);
         coinYield.forEach((key, value) -> this.storedCoins.put(key, this.storedCoins.get(key) + (coinYield.get(key) % 24)));
         
         ResearchTech activeTech = getActiveTech();
         if(activeTech != null){
            int rate = activeTech.getConsumptionRate() / 24;
            int progress = getProgress(activeTech);
            int cost = activeTech.getCost();
            int remaining = cost - progress;
            int maxConsumable = Math.min(remaining,rate);
            int bankConsume = Math.min(maxConsumable,bankedResearchCoins);
            int consumed = bankConsume;
            if(bankConsume < maxConsumable){
               int diff = maxConsumable-bankConsume;
               int stored = this.storedCoins.get(ResourceType.RESEARCH);
               int storeConsume = Math.min(stored,diff);
               consumed += storeConsume;
               this.storedCoins.put(ResourceType.RESEARCH, stored - storeConsume);
            }
            bankedResearchCoins -= bankConsume;
            techs.put(activeTech,progress+consumed);
            
            if(consumed == remaining){
               onTechComplete(activeTech);
            }
         }
         
         this.updateHolo = true;
      }
   }
   
   public Map<ResourceType,Integer> calculateCoinYield(ServerWorld serverWorld){
      HashMap<ResourceType,Integer> coins = new HashMap<>();
      for(ResourceType value : ResourceType.values()){
         coins.put(value,0);
      }
      if(!isFounded()) return coins;
      for(NationChunk chunk : chunks){
         if(chunk.isInfluenced()){
            Triple<Integer, Integer, Integer> chunkValues = NationsUtils.calculateChunkCoinGeneration(serverWorld, chunk.getPos());
            coins.put(ResourceType.GROWTH,coins.get(ResourceType.GROWTH) + chunkValues.getLeft());
            coins.put(ResourceType.MATERIAL,coins.get(ResourceType.MATERIAL) + chunkValues.getMiddle());
            coins.put(ResourceType.RESEARCH,coins.get(ResourceType.RESEARCH) + chunkValues.getRight());
         }
      }
      for(ResourceType value : ResourceType.values()){
         coins.put(value, (int) (coins.get(value)*0.01));
      }
      return coins;
   }
   
   public Map<ResourceType, Integer> getStoredCoins(){
      return storedCoins;
   }
   
   public ArrayList<ItemStack> getMailbox(){
      return mailbox;
   }
   
   public void addMail(ItemStack item){
      this.mailbox.add(item);
   }
   
   public void collectCoins(ServerWorld world){
      Vec3d spawnPos = getHologramPos().add(0,2,0);
      this.storedCoins.forEach((type, amount) ->{
         int sum = amount;
         int maxStackSize = type.getCoin().getMaxCount();
         while(sum > 1000){
            sum -= 1000;
            ItemStack newStack = new ItemStack(type.getBullion(),1);
            ItemScatterer.spawn(world,spawnPos.x,spawnPos.y,spawnPos.z,newStack);
         }
         while(sum > 0){
            int size = Math.min(sum,world.getRandom().nextBetween((int) Math.min(maxStackSize,amount/32.0), maxStackSize+1));
            sum -= size;
            ItemStack newStack = new ItemStack(type.getCoin(),size);
            ItemScatterer.spawn(world,spawnPos.x,spawnPos.y,spawnPos.z,newStack);
         }
      });
      this.storedCoins.replaceAll((t, v) -> 0);
      
      for(ItemStack stack : mailbox){
         ItemScatterer.spawn(world,spawnPos.x,spawnPos.y,spawnPos.z,stack);
      }
      mailbox.clear();
      
      this.updateHolo = true;
   }
   
   public void alertTrespass(ServerPlayerEntity player){
      boolean allow = NationsConfig.getBoolean(NationsRegistry.TRESPASS_ALERTS_CFG);
      if(!allow || player.isSpectator() || Nations.getPlayer(player).bypassesClaims()) return;
      for(ServerPlayerEntity onlinePlayer : getOnlinePlayers()){
         INationsProfileComponent profile = Nations.getPlayer(onlinePlayer);
         if(profile.trespassAlerts()){
            onlinePlayer.sendMessage(Text.translatable("text.nations.trespass_alert",player.getStyledDisplayName()).formatted(Formatting.RED));
         }
      }
   }
   
   private void openNationGUI(ServerPlayerEntity player){
      if(!hasPlayer(player) && !Nations.getPlayer(player).bypassesClaims()){
         player.sendMessage(Text.translatable("text.nations.nation_interact_wrong_nation").formatted(Formatting.RED));
         return;
      }
      if(!hasPermissions(player)){
         player.sendMessage(Text.translatable("text.nations.nation_interact_no_perms").formatted(Formatting.RED));
         return;
      }
      NationGui gui = new NationGui(player,this, NationGui.Mode.MENU);
      gui.open();
   }
   
   private ElementHolder getNewHologram(ServerWorld serverWorld){
      ResearchTech activeTech = getActiveTech();
      TextDisplayElement line1 = new TextDisplayElement(getFormattedName().formatted(Formatting.BOLD));
      TextDisplayElement line2 = new TextDisplayElement(Text.literal(members.size()+" ").withColor(getTextColor()).formatted(Formatting.BOLD).append(Text.translatable("text.nations.members").withColor(getTextColorSub())));
      TextDisplayElement line3 = new TextDisplayElement(Text.literal(String.format("%,d",victoryPoints)+" ").withColor(getTextColor()).formatted(Formatting.BOLD).append(Text.translatable("text.nations.victory_points").withColor(getTextColorSub())));
      MutableText researchText = activeTech == null ? Text.translatable("text.nations.nothing") : activeTech.getName();
      float percentage = activeTech == null ? 0 : 100*((float) getProgress(activeTech) / activeTech.getCost());
      TextDisplayElement line4 = new TextDisplayElement(Text.translatable("text.nations.currently_researching",researchText.formatted(Formatting.LIGHT_PURPLE),Text.literal(String.format("%03.2f",percentage)).formatted(Formatting.DARK_AQUA,Formatting.BOLD)).formatted(Formatting.AQUA));
      
      ItemDisplayElement icon = new ItemDisplayElement(GraphicalItem.with(GraphicalItem.GraphicItems.MONUMENT));
      InteractionElement click = new InteractionElement(new VirtualElement.InteractionHandler(){
         public void click(ServerPlayerEntity player){
            if(interactCooldown == 0){
               if(player.isSneaking() && hasPermissions(player)){
                  collectCoins(player.getServerWorld());
               }else{
                  openNationGUI(player);
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
      click.setSize(1.25f,1.75f);
      
      ElementHolder holder = new ElementHolder(){
         ServerWorld world = serverWorld;
         private final TextDisplayElement line1Text = line1;
         private final TextDisplayElement line2Text = line2;
         private final TextDisplayElement line3Text = line3;
         private final TextDisplayElement line4Text = line4;
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
               Vec3d offsetDir = player.getPos().subtract(centerPos).multiply(1,0,1).normalize().multiply(0.85);
               
               for(VirtualElement element : this.getElements()){
                  double ey = element.getOffset().y;
                  element.setOffset(offsetDir.add(0,ey,0));
               }
            }
            
            if(tickCount == 200 || updateHolo){
               ResearchTech activeTech = getActiveTech();
               MutableText text2 = Text.literal(members.size()+" ").withColor(getTextColor()).formatted(Formatting.BOLD).append(Text.translatable("text.nations.members").withColor(getTextColorSub()));
               MutableText text3 = Text.literal(String.format("%,d",victoryPoints)+" ").withColor(getTextColor()).formatted(Formatting.BOLD).append(Text.translatable("text.nations.victory_points").withColor(getTextColorSub()));
               MutableText researchText = activeTech == null ? Text.translatable("text.nations.nothing") : activeTech.getName();
               float percentage = activeTech == null ? 0 : 100*((float) getProgress(activeTech) / activeTech.getCost());
               MutableText text4 = Text.translatable("text.nations.currently_researching",researchText.formatted(Formatting.LIGHT_PURPLE),Text.literal(String.format("%03.2f",percentage)).formatted(Formatting.DARK_AQUA,Formatting.BOLD)).formatted(Formatting.AQUA);
               
               line2Text.setText(text2);
               line3Text.setText(text3);
               line4Text.setText(text4);
               
               tickCount = 0;
               updateHolo = false;
            }
         }
      };
      line1.setOffset(new Vec3d(0,2.25,0));
      line2.setOffset(new Vec3d(0,2.0,0));
      line3.setOffset(new Vec3d(0,1.75,0));
      icon.setOffset(new Vec3d(0,1.0,0));
      line4.setOffset(new Vec3d(0,0,0));
      click.setOffset(new Vec3d(0,0.25,0));
      
      line1.setBillboardMode(DisplayEntity.BillboardMode.VERTICAL);
      line2.setBillboardMode(DisplayEntity.BillboardMode.VERTICAL);
      line3.setBillboardMode(DisplayEntity.BillboardMode.VERTICAL);
      icon.setBillboardMode(DisplayEntity.BillboardMode.VERTICAL);
      line4.setBillboardMode(DisplayEntity.BillboardMode.VERTICAL);
      icon.setScale(new Vector3f(1.5f,1.5f,1f));
      line4.setLineWidth(1000);
      
      holder.addElement(line1);
      holder.addElement(line2);
      holder.addElement(line3);
      holder.addElement(icon);
      holder.addElement(click);
      holder.addElement(line4);
      return holder;
   }
   
   public Vec3d getHologramPos(){
      return this.isFounded() ? this.foundLocation.getBlockPos(7,foundHeight,7).toCenterPos().add(0.5,4,0.5) : null;
   }
   
   public void queueTechFirst(ResearchTech tech){
      ArrayList<ResearchTech> techOrder = queueTechFirstHelper(tech,new ArrayList<>());
      for(ResearchTech researchTech : techOrder){
         dequeueTech(researchTech);
      }
      for(ResearchTech researchTech : techOrder){
         techQueue.addFirst(researchTech);
      }
      this.updateHolo = true;
   }
   
   private ArrayList<ResearchTech> queueTechFirstHelper(ResearchTech tech, ArrayList<ResearchTech> list){
      List<ResearchTech> completed = getCompletedTechs();
      List<ResearchTech> missing = tech.missingPrereqs(completed);
      if(list.contains(tech)) return list;
      list.add(tech);
      for(ResearchTech missingTech : missing){
         queueTechFirstHelper(missingTech, list);
      }
      return list;
   }
   
   public void queueTech(ResearchTech tech){
      List<ResearchTech> completed = getCompletedTechs();
      List<ResearchTech> missing = tech.missingPrereqs(completed);
      if(techQueue.contains(tech)) return;
      for(ResearchTech missingTech : missing){
         queueTech(missingTech);
      }
      techQueue.add(tech);
      this.updateHolo = true;
   }
   
   public void dequeueTech(ResearchTech tech){
      if(!techQueue.contains(tech)) return;
      Iterator<ResearchTech> iter = techQueue.iterator();
      boolean found = false;
      List<ResearchTech> postreqs = new ArrayList<>();
      while(iter.hasNext()){
         ResearchTech iterTech = iter.next();
         if(iterTech.equals(tech)){
            found = true;
            iter.remove();
         }else if(found){
            if(iterTech.hasPrereq(tech)){
               postreqs.add(iterTech);
            }
         }
      }
      for(ResearchTech postreq : postreqs){
         dequeueTech(postreq);
      }
      this.updateHolo = true;
   }
   
   public void moveQueuedTechUp(ResearchTech tech) {
      List<ResearchTech> list = new ArrayList<>(techQueue);
      int i = list.indexOf(tech);
      if (i > 0) {
         ResearchTech prevTech = list.get(i-1);
         if(!tech.hasPrereq(prevTech)){ // Can move up
            Collections.swap(list, i, i - 1);
            techQueue.clear();
            techQueue.addAll(list);
         }
      }
      this.updateHolo = true;
   }
   
   public void moveQueuedTechDown(ResearchTech tech) {
      List<ResearchTech> list = new ArrayList<>(techQueue);
      int i = list.indexOf(tech);
      if (i >= 0 && i < list.size() - 1) {
         ResearchTech nextTech = list.get(i+1);
         if(!nextTech.hasPrereq(tech)){ // Can move down
            Collections.swap(list, i, i + 1);
            techQueue.clear();
            techQueue.addAll(list);
         }
      }
      this.updateHolo = true;
   }
   
   public ArrayDeque<ResearchTech> getTechQueue(){
      return techQueue;
   }
   
   public String getId(){
      return id;
   }
   
   public String getName(){
      return name;
   }
   
   public boolean promote(UUID player){
      boolean isMember = members.contains(player);
      boolean isExecutor = executors.contains(player);
      boolean isLeader = leaders.contains(player);

      if(!isMember || isLeader) return false;
      if(!isExecutor){
         executors.add(player);
      }else{
         leaders.add(player);
      }
      return true;
   }
   
   public boolean promote(ServerPlayerEntity player){
      return promote(player.getUuid());
   }
   
   public boolean demote(ServerPlayerEntity player){
      return demote(player.getUuid());
   }
   
   public boolean demote(UUID player){
      boolean isMember = members.contains(player);
      boolean isExecutor = executors.contains(player);
      boolean isLeader = leaders.contains(player);

      if(!isMember) return false;
      if(isLeader){
         leaders.remove(player);
         executors.add(player);
         return true;
      }else if(isExecutor){
         executors.remove(player);
         return true;
      }
      return false;
   }
   
   public boolean hasPermissions(ServerPlayerEntity player){
      return executors.contains(player.getUuid()) || leaders.contains(player.getUuid()) || Nations.getPlayer(player).bypassesClaims();
   }
   
   public boolean hasPlayer(ServerPlayerEntity player){
      return members.contains(player.getUuid());
   }
   
   public boolean isLeader(ServerPlayerEntity player){
      return members.contains(player.getUuid()) && leaders.contains(player.getUuid());
   }
   
   public boolean isExecutor(ServerPlayerEntity player){
      return members.contains(player.getUuid()) && !leaders.contains(player.getUuid()) && executors.contains(player.getUuid());
   }
   
   public boolean hasPermissions(UUID uuid){
      return executors.contains(uuid) || leaders.contains(uuid);
   }
   
   public boolean hasPlayer(UUID uuid){
      return members.contains(uuid);
   }
   
   public boolean isLeader(UUID uuid){
      return members.contains(uuid) && leaders.contains(uuid);
   }
   
   public boolean isExecutor(UUID uuid){
      return members.contains(uuid) && !leaders.contains(uuid) && executors.contains(uuid);
   }
   
   public void changeName(String newName){
      this.name = newName;
      for(ServerPlayerEntity player : getOnlinePlayers()){
         for(ServerPlayerEntity p : player.getServer().getPlayerManager().getPlayerList()){
            p.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(player.getServer().getPlayerManager().getPlayerList()));
         }
         Nations.getPlayer(player).removePlayerTeam(player.getServer());
         Nations.getPlayer(player).addPlayerTeam(player.getServer());
      }
      
      for(CapturePoint cap : getCapturePoints()){
         if(cap.getControllingNation() != null && cap.getControllingNation().equals(this)){
            DynmapCalls.updateCapturePointMarker(cap);
         }
      }
      DynmapCalls.redrawNationBorder(this);
      if(hologram != null){
         hologram.destroy();
         hologram = null;
      }
   }
   
   public void changeColors(int newTextColor, int newTextColorSub, DyeColor newDyeColor){
      this.textColor = newTextColor;
      this.textColorSub = newTextColorSub;
      this.dyeColor = newDyeColor;
      
      for(ServerPlayerEntity player : getOnlinePlayers()){
         for(ServerPlayerEntity p : player.getServer().getPlayerManager().getPlayerList()){
            p.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(player.getServer().getPlayerManager().getPlayerList()));
         }
         Nations.getPlayer(player).removePlayerTeam(player.getServer());
         Nations.getPlayer(player).addPlayerTeam(player.getServer());
      }
      
      for(CapturePoint cap : getCapturePoints()){
         if(cap.getControllingNation() != null && cap.getControllingNation().equals(this)){
            cap.transferOwnership(SERVER.getOverworld(),this);
         }
      }
      recolorMonument(SERVER.getOverworld());
      DynmapCalls.redrawNationBorder(this);
      if(hologram != null){
         hologram.destroy();
         hologram = null;
      }
   }
   
   private void recolorMonument(ServerWorld world){
      DyeColor newColor = getDyeColor();
      StructurePlacer.Structure structure = NATION_STRUCTURE;
      BlockPos origin = getFoundingPos();
      int[][][] pattern = structure.statePattern();
      for(int i = 0; i < pattern.length; i++){
         for(int j = 0; j < pattern[0].length; j++){
            for(int k = 0; k < pattern[0][0].length; k++){
               if(pattern[i][j][k] == -1) continue;
               BlockPos pos = origin.add(i,j,k);
               BlockState curState = world.getBlockState(pos);
               BlockState newState = NationsColors.redyeBlock(curState, newColor);
               if(curState.isOf(newState.getBlock())) continue;
               world.setBlockState(pos,newState, Block.NOTIFY_ALL_AND_REDRAW);
            }
         }
      }
   }
   
   public List<ServerPlayerEntity> getOnlinePlayers(){
      ArrayList<ServerPlayerEntity> players = new ArrayList<>();
      for(ServerPlayerEntity player : SERVER.getPlayerManager().getPlayerList()){
         if(hasPlayer(player)){
            players.add(player);
         }
      }
      return players;
   }
   
   public void addPlayer(ServerPlayerEntity player){
      for(Nation nation : getNations()){
         if(!nation.equals(this) && nation.hasPlayer(player)){
            nation.removePlayer(player);
         }
      }
      members.add(player.getUuid());
      Nations.getPlayer(player).setNation(this);
      Nations.getPlayer(player).addPlayerTeam(player.getServer());
      
      for(ServerPlayerEntity p : player.getServer().getPlayerManager().getPlayerList()){
         p.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(player.getServer().getPlayerManager().getPlayerList()));
      }
   }
   
   public void removePlayer(ServerPlayerEntity player){
      members.remove(player.getUuid());
      executors.remove(player.getUuid());
      leaders.remove(player.getUuid());
      Nations.getPlayer(player).setNation(null);
      Nations.getPlayer(player).setChannel(ChatChannel.GLOBAL);
      Nations.getPlayer(player).removePlayerTeam(player.getServer());
      
      for(ServerPlayerEntity p : player.getServer().getPlayerManager().getPlayerList()){
         p.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(player.getServer().getPlayerManager().getPlayerList()));
      }
   }
   
   public Set<UUID> getMembers(){
      return members;
   }
   
   public List<ResearchTech> getCompletedTechs(){
      List<ResearchTech> completed = new ArrayList<>();
      techs.forEach((tech, progress) -> {
         if(progress >= tech.getCost()){
            completed.add(tech);
         }
      });
      return completed;
   }
   
   public boolean hasCompletedTech(RegistryKey<ResearchTech> techKey){
      ResearchTech tech = NationsRegistry.RESEARCH.get(techKey);
      if(tech == null) return false;
      int progress = techs.getOrDefault(tech,-1);
      return progress >= tech.getCost();
   }
   
   public void setActiveTech(ResearchTech activeTech){
      queueTechFirst(activeTech);
   }
   
   public ResearchTech getActiveTech(){
      if(techQueue.isEmpty()){
         return null;
      }else{
         return techQueue.getFirst();
      }
   }
   
   public int getProgress(ResearchTech tech){
      return techs.getOrDefault(tech,0);
   }
   
   public boolean hasStartedTech(ResearchTech tech){
      return techs.getOrDefault(tech,-1) > 0;
   }
   
   public List<ResearchTech> availableTechs(){
      List<ResearchTech> available = new ArrayList<>();
      List<ResearchTech> completed = getCompletedTechs();
      int curTier = NationsConfig.getInt(NationsRegistry.RESEARCH_TIER_CFG);
      for(ResearchTech research : NationsRegistry.RESEARCH){
         if(!hasCompletedTech(research.getKey()) && curTier >= research.getTier()){
            available.add(research);
         }
      }
      return available;
   }
   
   public void storeResearchCoins(int count){
      bankedResearchCoins += count;
   }
   
   public int getResearchBudget(){
      return bankedResearchCoins;
   }
   
   public int getFoundHeight(){
      return foundHeight;
   }
   
   public BlockPos getFoundingPos(){
      if(!isFounded()) return null;
      return this.foundLocation.getBlockPos(0,this.getFoundHeight(),0);
   }
   
   public Set<NationChunk> getChunks(){
      return chunks;
   }
   
   public int getVictoryPoints(){
      return victoryPoints;
   }
   
   public void addVictoryPoints(int points){
      this.victoryPoints = Math.max(0,victoryPoints+points);
   }
   
   public boolean isFounded(){
      return foundLocation != null;
   }
   
   public int getTextColor(){
      return textColor;
   }
   
   public int getTextColorSub(){
      return textColorSub;
   }
   
   public void setTextColorSub(int textColorSub){
      this.textColorSub = textColorSub;
   }
   
   public void setTextColor(int textColor){
      this.textColor = textColor;
   }
   
   public DyeColor getDyeColor(){
      return dyeColor;
   }
   
   public void setDyeColor(DyeColor dyeColor){
      this.dyeColor = dyeColor;
   }
   
   public void settleNation(ServerWorld world, ChunkPos pos){
      if(foundLocation != null) return;
      this.foundLocation = pos;
      
      Heightmap heightmap = world.getChunk(pos.x,pos.z).getHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES);
      double sum = 0;
      double blockCount = 0;
      for(int i = 0; i < 16; i++){
         for(int j = 0; j < 16; j++){
            sum += heightmap.get(i,j);
            blockCount++;
         }
      }
      int height = (int) (Math.round(sum/blockCount)-1);
      BlockPos origin = pos.getBlockPos(0,height,0);
      BlockPos corner = pos.getBlockPos(15,height+NATION_STRUCTURE.statePattern()[0].length,15);
      
      Vec3d tpPos = pos.getBlockPos(8,height+6,8).toBottomCenterPos().subtract(0.5,0,0.5);
      for(ServerPlayerEntity player : world.getEntitiesByClass(ServerPlayerEntity.class, new Box(origin.toBottomCenterPos(), corner.toBottomCenterPos()), p -> true)){
         player.requestTeleport(tpPos.x,tpPos.y,tpPos.z);
      }
      
      this.foundHeight = height;
      
      StructurePlacer.placeStructure(StructurePlacer.copyStructureWithColor(NATION_STRUCTURE,getDyeColor()), world, pos.getBlockPos(0,height,0));
      
      int radius = NationsConfig.getInt(NationsRegistry.SETTLE_RADIUS_CFG);
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            if (Math.abs(dx) + Math.abs(dz) <= radius) {
               ChunkPos chunkPos = new ChunkPos(pos.x + dx, pos.z + dz);
               NationChunk nationChunk = Nations.getChunk(chunkPos);
               if(nationChunk == null) continue;
               if(!nationChunk.isInfluenced()){
                  nationChunk.setControllingNationId(getId());
                  nationChunk.setInfluenced(true);
                  if(dx == 0 && dz == 0){
                     nationChunk.setClaimed(true);
                  }
                  
                  CapturePoint cap = Nations.getCapturePoint(chunkPos);
                  if(cap != null){
                     cap.transferOwnership(world,this);
                  }
               }
            }
         }
      }
      
      DynmapCalls.redrawNationBorder(this);
      
      MutableText announceText = Text.translatable("text.nations.founding_announcement",
            getFormattedNameTag(false),
            Text.literal(getFoundingPos().toShortString()).withColor(getTextColor()),
            Text.literal(pos.toString()).withColor(getTextColor())
      ).withColor(getTextColorSub());
      Nations.announce(announceText);
   }
   
   public void onTechComplete(ResearchTech tech){
      MutableText announceText = Text.translatable("text.nations.finish_tech_announcement",
            getFormattedNameTag(false),
            NationsColors.withColor(tech.getName(),getTextColor())
      ).withColor(getTextColorSub());
      Nations.announce(announceText);
      
      techQueue.removeIf(iterTech -> iterTech.equals(tech));
      
      addVictoryPoints(NationsConfig.getInt(NationsRegistry.VICTORY_POINTS_RESEARCH_CFG));
      if(TECH_TRACKER.containsKey(this)){
         boolean found = TECH_TRACKER.entrySet().stream().anyMatch(entry -> entry.getValue().stream().anyMatch(key -> tech.getKey().equals(key)));
         if(!found){
            addVictoryPoints(NationsConfig.getInt(NationsRegistry.VICTORY_POINTS_RESEARCH_BONUS_CFG));
         }
      }else{
         long count = Nations.getNations().stream().filter(nation -> nation.hasCompletedTech(tech.getKey())).count();
         if(count == 1){
            addVictoryPoints(NationsConfig.getInt(NationsRegistry.VICTORY_POINTS_RESEARCH_BONUS_CFG));
         }
      }
      
      for(ServerPlayerEntity player : getOnlinePlayers()){
         IArcanaProfileComponent arcanaProfile = ArcanaNovum.data(player);
         for(ArcanaItem arcanaItem : ArcanaRegistry.ARCANA_ITEMS){
            if(canCraft(arcanaItem) || arcanaItem.getId().equals(ArcanaRegistry.ARCANE_TOME.getId())){
               arcanaProfile.addResearchedItem(arcanaItem.getId());
            }else{
               arcanaProfile.removeResearchedItem(arcanaItem.getId());
            }
         }
      }
      
      if(tech.isArcanaItem()){
         ArcanaItem item = NationsRegistry.ARCANA_TECHS.entrySet().stream().filter(entry -> entry.getValue().equals(tech.getKey())).map(Map.Entry::getKey).findFirst().orElse(null);
         if(item != null && item.getId().contains("altar")){
            // Give Altar
            addMail(item.addCrafter(item.getNewItem(),JERALD_UUID,false,SERVER));
         }
      }else if(tech.getKey().equals(NationsRegistry.ARCANA)){
         // Give Forge
         addMail(ArcanaRegistry.STARLIGHT_FORGE.addCrafter(ArcanaRegistry.STARLIGHT_FORGE.getNewItem(),JERALD_UUID,false,SERVER));
         addMail(ArcanaRegistry.ARCANE_TOME.addCrafter(ArcanaRegistry.ARCANE_TOME.getNewItem(),JERALD_UUID,false,SERVER));
      }else if(tech.getKey().equals(NationsRegistry.ENCHANTING)){
         // Give Enchanter
         addMail(ArcanaRegistry.MIDNIGHT_ENCHANTER.addCrafter(ArcanaRegistry.MIDNIGHT_ENCHANTER.getNewItem(),JERALD_UUID,false,SERVER));
      }else if(tech.getKey().equals(NationsRegistry.SMITHING)){
         // Give Anvil
         addMail(ArcanaRegistry.TWILIGHT_ANVIL.addCrafter(ArcanaRegistry.TWILIGHT_ANVIL.getNewItem(),JERALD_UUID,false,SERVER));
      }else if(tech.getKey().equals(NationsRegistry.FLETCHING)){
         // Give Fletchery
         addMail(ArcanaRegistry.RADIANT_FLETCHERY.addCrafter(ArcanaRegistry.RADIANT_FLETCHERY.getNewItem(),JERALD_UUID,false,SERVER));
      }else if(tech.getKey().equals(NationsRegistry.FORGING)){
         // Give Core
         addMail(ArcanaRegistry.STELLAR_CORE.addCrafter(ArcanaRegistry.STELLAR_CORE.getNewItem(),JERALD_UUID,false,SERVER));
      }else if(tech.getKey().equals(NationsRegistry.ADVANCED_ARCANA)){
         // Give Singularity
         addMail(ArcanaRegistry.ARCANE_SINGULARITY.addCrafter(ArcanaRegistry.ARCANE_SINGULARITY.getNewItem(),JERALD_UUID,false,SERVER));
      }
   }
   
   public boolean canClaimOrInfluenceChunk(ChunkPos pos){
      NationChunk chunk = Nations.getChunk(pos);
      if(chunk == null || !NationsLand.unclaimedOrSameNation(pos,this) || chunk.isClaimed() || NationsLand.isSpawnChunk(pos.getBlockPos(0,0,0))){
         return false;
      }
      
      boolean claim = chunk.isInfluenced();
      int surrounded = 0;
      for(int i = -1; i <= 1; i++){
         for(int j = -1; j <= 1; j++){
            NationChunk nChunk = Nations.getChunk(pos.x + i, pos.z + j);
            if((i == 0 && j == 0) || nChunk == null || !chunks.contains(nChunk)) continue;
            if(nChunk.isInfluenced()) surrounded++;
         }
      }
      if(claim && surrounded < 8){
         return false;
      }else if(!claim && surrounded < 3){
         return false;
      }
      return true;
   }
   
   public void updateChunk(NationChunk nationChunk){
      if(nationChunk.getControllingNation() != null && nationChunk.getControllingNation().equals(this)){
         chunks.add(nationChunk);
         nationChunk.setControllingNationId(getId());
      }else{
         chunks.remove(nationChunk);
         nationChunk.setControllingNationId(null);
      }
   }
   
   public boolean removeTech(ResearchTech newTech, boolean withPostReqs){
      List<ResearchTech> postreqs = new ArrayList<>();
      techs.forEach((tech, prog) -> {
         if(tech.hasPrereq(newTech)){
            postreqs.add(tech);
         }
      });
      if(techs.containsKey(newTech)){
         techs.remove(newTech);
         if(withPostReqs){
            for(ResearchTech postreq : postreqs){
               removeTech(postreq, true);
            }
         }
         return true;
      }else{
         return false;
      }
   }
   
   public boolean addTechAndPrereqs(ResearchTech newTech){
      RegistryKey<ResearchTech>[] prereqs = newTech.getPrereqs();
      if(techs.getOrDefault(newTech,-1) >= newTech.getCost()) return false;
      
      for(RegistryKey<ResearchTech> prereq : prereqs){
         if(!hasCompletedTech(prereq)){
            ResearchTech prereqValue = NationsRegistry.RESEARCH.get(prereq);
            if(prereqValue == null) continue;
            addTechAndPrereqs(prereqValue);
         }
      }
      
      techs.put(newTech,newTech.getCost());
      onTechComplete(newTech);
      return true;
   }
   
   public MutableText getFormattedName(){
      return NationsColors.withColor(Text.literal(name),textColor);
   }
   
   public MutableText getFormattedNameTag(boolean withSpace){
      MutableText text = Text.literal("[").withColor(textColorSub).append(getFormattedName()).append(Text.literal("]").withColor(textColorSub));
      if(withSpace) text = text.append(Text.literal(" "));
      return text;
   }
   
   private boolean isInfluenceContinuous(){
      return MiscUtils.getConnectedSections(chunks.stream().filter(nc -> nc.getControllingNation().equals(this) && nc.isInfluenced()).map(NationChunk::getPos).toList()).size() == 1;
   }
   
   public List<BlockPos> getInfluenceCorners(){
      List<List<ChunkPos>> sections = MiscUtils.getConnectedSections(chunks.stream().filter(nc -> nc.getControllingNation().equals(this) && nc.isInfluenced()).map(NationChunk::getPos).toList());
      if(sections.size() != 1) return new ArrayList<>();
      return MiscUtils.getOrderedPerimeter(sections.getFirst().stream().map(c -> new Vector2i(c.x,c.z)).toList()).stream().map(v -> new BlockPos(v.x,64,v.y)).toList();
   }
   
   public List<List<BlockPos>> getClaimCorners(){
      List<List<ChunkPos>> sections = MiscUtils.getConnectedSections(chunks.stream().filter(nc -> nc.getControllingNation().equals(this) && nc.isClaimed()).map(NationChunk::getPos).toList());
      List<List<BlockPos>> cornerSets = new ArrayList<>();
      for(List<ChunkPos> section : sections){
         cornerSets.add(MiscUtils.getOrderedPerimeter(section.stream().map(c -> new Vector2i(c.x,c.z)).toList()).stream().map(v -> new BlockPos(v.x,65,v.y)).toList());
      }
      return cornerSets;
   }
   
   public boolean canCraft(Item item){
      if(!NationsRegistry.LOCKED_ITEMS.containsKey(item)) return true;
      
      for(ResearchTech completedTech : getCompletedTechs()){
         if(completedTech.getCraftLocked().contains(item)) return true;
      }
      return false;
   }
   
   public boolean canBrew(Item item, Potion potion){
      if(!NationsRegistry.LOCKED_POTIONS.containsKey(potion)){
         return true;
      }
      String potionEntryName = SERVER.getRegistryManager().getOrThrow(RegistryKeys.POTION).getEntry(potion).getIdAsString().toLowerCase(Locale.ROOT);
      boolean strong = false;
      boolean extended = false;
      boolean thrown = false;
      boolean found = false;
      RegistryKey<ResearchTech> neededTech = NationsRegistry.POTION_TECHS.get(potion);
      if(neededTech == null) return false;
      for(ResearchTech tech : getCompletedTechs()){
         if(tech.getKey().equals(NationsRegistry.POTENT_ALCHEMY)){
            strong = true;
         }
         if(tech.getKey().equals(NationsRegistry.ENDURING_ALCHEMY)){
            extended = true;
         }
         if(tech.getKey().equals(NationsRegistry.PROLIFIC_ALCHEMY)){
            thrown = true;
         }
         if(tech.getKey().equals(neededTech)){
            found = true;
         }
      }
      if(!found) return false;
      if(potionEntryName.contains("long") && !extended) return false;
      if(potionEntryName.contains("strong") && !strong) return false;
      if((item == Items.LINGERING_POTION || item == Items.SPLASH_POTION) && !thrown) return false;
      return true;
   }
   
   public boolean canEnchant(RegistryKey<Enchantment> enchant, int level){
      RegistryKey<ResearchTech> neededTech = null;
      for(Map.Entry<Pair<RegistryKey<Enchantment>, Integer>, RegistryKey<ResearchTech>> entry : NationsRegistry.ENCHANT_TECHS.entrySet()){
         Pair<RegistryKey<Enchantment>, Integer> pair = entry.getKey();
         if(pair.getLeft().toString().equals(enchant.toString()) && level == pair.getRight()){
            neededTech = entry.getValue();
            break;
         }
      }
      if(neededTech == null){
         return false;
      }
      for(ResearchTech tech : getCompletedTechs()){
         if(tech.getKey().equals(neededTech)){
            return true;
         }
      }
      return false;
   }
   
   public boolean canCraft(ArcanaItem item){
      RegistryKey<ResearchTech> neededTech = NationsRegistry.ARCANA_TECHS.get(item);
      if(item.getId().equals(ArcanaRegistry.ARCANE_TOME.getId())) return hasCompletedTech(NationsRegistry.ARCANA);
      if(item.getId().equals(ArcanaRegistry.STARLIGHT_FORGE.getId())) return hasCompletedTech(NationsRegistry.ARCANA);
      if(item.getId().equals(ArcanaRegistry.RADIANT_FLETCHERY.getId())) return hasCompletedTech(NationsRegistry.FLETCHING);
      if(item.getId().equals(ArcanaRegistry.MIDNIGHT_ENCHANTER.getId())) return hasCompletedTech(NationsRegistry.ENCHANTING);
      if(item.getId().equals(ArcanaRegistry.TWILIGHT_ANVIL.getId())) return hasCompletedTech(NationsRegistry.SMITHING);
      if(item.getId().equals(ArcanaRegistry.STELLAR_CORE.getId())) return hasCompletedTech(NationsRegistry.FORGING);
      if(item.getId().equals(ArcanaRegistry.ARCANE_SINGULARITY.getId())) return hasCompletedTech(NationsRegistry.ADVANCED_ARCANA);
      if(item.getId().equals(ArcanaRegistry.SPEAR_OF_TENBROUS.getId())) return true;
      if(item.getId().equals(ArcanaRegistry.GREAVES_OF_GAIALTUS.getId())) return true;
      if(item.getId().equals(ArcanaRegistry.PICKAXE_OF_CEPTYUS.getId())) return true;
      
      if(neededTech == null) return false;
      for(ResearchTech tech : getCompletedTechs()){
         if(tech.getKey().equals(neededTech)){
            return true;
         }
      }
      return false;
   }
   
   public Set<Pair<RegistryKey<Enchantment>, Integer>> getPossibleEnchantments(){
      Set<Pair<RegistryKey<Enchantment>, Integer>> enchants = new HashSet<>();
      for(Map.Entry<Pair<RegistryKey<Enchantment>, Integer>, RegistryKey<ResearchTech>> entry : NationsRegistry.ENCHANT_TECHS.entrySet()){
         if(hasCompletedTech(entry.getValue())){
            enchants.add(entry.getKey());
         }
      }
      return enchants;
   }
   
   public int getArcanaLevels(){
      int levels = 1;
      for(ResearchTech tech : getCompletedTechs()){
         if(tech.isArcanaItem()){
            levels += 2;
         }else if(tech.isEnchant()){
            for(Map.Entry<Pair<RegistryKey<Enchantment>, Integer>, RegistryKey<ResearchTech>> entry : NationsRegistry.ENCHANT_TECHS.entrySet()){
               Pair<RegistryKey<Enchantment>, Integer> pair = entry.getKey();
               if(entry.getValue().equals(tech.getKey())){
                  if(MiscUtils.getEnchantment(pair.getLeft()).value().getMaxLevel() == pair.getRight()){
                     levels += 1;
                  }
               }
            }
         }
      }
      return Math.min(100,levels);
   }
   
   public int getArcanaSkillPoints(){
      int skillPoints = 1;
      for(ResearchTech tech : getCompletedTechs()){
         if(tech.isArcanaItem()){
            skillPoints += 5;
         }else if(tech.isEnchant()){
            skillPoints += 1;
         }else if(tech.isPotion()){
            skillPoints += 1;
         }
      }
      return skillPoints;
   }
   
   @Override
   public boolean equals(Object obj){
      if(this == obj) return true;
      return obj instanceof Nation nat && nat.getId().equals(this.id);
   }
   
   public NbtCompound saveToNbt(NbtCompound compound, RegistryWrapper.WrapperLookup wrapperLookup){
      NbtList memberList = new NbtList();
      NbtList executorList = new NbtList();
      NbtList leaderList = new NbtList();
      NbtList chunkList = new NbtList();
      NbtList mailList = new NbtList();
      NbtCompound techList = new NbtCompound();
      NbtCompound research = new NbtCompound();
      NbtCompound posComp = new NbtCompound();
      NbtCompound coins = new NbtCompound();
      memberList.addAll(members.stream().map(uuid -> NbtString.of(uuid.toString())).collect(Collectors.toSet()));
      executorList.addAll(executors.stream().map(uuid -> NbtString.of(uuid.toString())).collect(Collectors.toSet()));
      leaderList.addAll(leaders.stream().map(uuid -> NbtString.of(uuid.toString())).collect(Collectors.toSet()));
      chunkList.addAll(chunks.stream().map(NationChunk::getPosNbt).collect(Collectors.toSet()));
      mailList.addAll(mailbox.stream().map(stack -> stack.toNbt(wrapperLookup)).collect(Collectors.toSet()));
      
      ResearchTech[] techQList = techQueue.toArray(new ResearchTech[]{});
      for(int i = 0; i < techQueue.size(); i++){
         techList.putInt(techQList[i].getId(),i);
      }
      
      techs.forEach((key, value) -> research.putInt(key.getId(), value));
      for(ResourceType value : ResourceType.values()){
         coins.putInt(value.asString(),storedCoins.getOrDefault(value,0));
      }
      if(foundLocation != null){
         posComp.putInt("x",foundLocation.x);
         posComp.putInt("y",foundHeight);
         posComp.putInt("z",foundLocation.z);
      }
      
      compound.put("members",memberList);
      compound.put("executors",executorList);
      compound.put("leaders",leaderList);
      compound.put("chunks",chunkList);
      compound.put("research",research);
      compound.put("techQueue",techList);
      compound.put("foundLocation",posComp);
      compound.put("coins",coins);
      compound.put("mailbox",mailList);
      compound.putString("id",id);
      compound.putString("name",name);
      compound.putString("dyeColor",dyeColor.getName());
      compound.putInt("textColor",textColor);
      compound.putInt("textColorSub",textColorSub);
      compound.putInt("victoryPoints",victoryPoints);
      compound.putInt("bankedResearchCoins",bankedResearchCoins);
      return compound;
   }
   
   public static Nation loadFromNbt(NbtCompound compound, HashMap<ChunkPos,NationChunk> nationChunks, RegistryWrapper.WrapperLookup wrapperLookup){
      try{
         Set<UUID> members = new HashSet<>();
         Set<UUID> executors = new HashSet<>();
         Set<UUID> leaders = new HashSet<>();
         Set<NationChunk> chunks = new HashSet<>();
         Map<ResearchTech, Integer> research = new HashMap<>();
         Map<ResourceType, Integer> coins = new HashMap<>();
         ArrayList<ItemStack> mail = new ArrayList<>();
         compound.getList("members", NbtElement.STRING_TYPE).forEach(uuid -> members.add(MiscUtils.getUUID(uuid.asString())));
         compound.getList("executors", NbtElement.STRING_TYPE).forEach(uuid -> executors.add(MiscUtils.getUUID(uuid.asString())));
         compound.getList("leaders", NbtElement.STRING_TYPE).forEach(uuid -> leaders.add(MiscUtils.getUUID(uuid.asString())));
         compound.getList("chunks", NbtElement.COMPOUND_TYPE).forEach(comp -> chunks.add(nationChunks.get(new ChunkPos(((NbtCompound)comp).getInt("x"),((NbtCompound)comp).getInt("z")))));
         compound.getList("mailbox",NbtElement.COMPOUND_TYPE).forEach(comp -> mail.add(ItemStack.fromNbtOrEmpty(wrapperLookup, (NbtCompound) comp)));
         
         NbtCompound techComp = compound.getCompound("techQueue");
         Map<Integer,ResearchTech> map = new TreeMap<>();
         for (String key : techComp.getKeys()) {
            int idx = techComp.getInt(key);
            ResearchTech tech = NationsRegistry.RESEARCH.get(Identifier.of(MOD_ID, key));
            if (tech != null) {
               map.put(idx, tech);
            }
         }
         ArrayDeque<ResearchTech> techQueue = new ArrayDeque<>(map.values());
         
         NbtCompound researchComp = compound.getCompound("research");
         for(String key : researchComp.getKeys()){
            research.put(NationsRegistry.RESEARCH.get(Identifier.of(MOD_ID,key)),researchComp.getInt(key));
         }
         
         NbtCompound coinComp = compound.getCompound("coins");
         for(ResourceType value : ResourceType.values()){
            if(coinComp.contains(value.asString())){
               coins.put(value,coinComp.getInt(value.asString()));
            }else{
               coins.put(value,0);
            }
         }
         
         NbtCompound posComp = compound.getCompound("foundLocation");
         ChunkPos foundLocation = null;
         int foundY = 0;
         if(posComp.contains("x") && posComp.contains("y") && posComp.contains("z")){
            foundLocation = new ChunkPos(posComp.getInt("x"), posComp.getInt("z"));
            foundY = posComp.getInt("y");
         }
         
         return new Nation(
               members,
               executors,
               leaders,
               chunks,
               foundLocation,
               foundY,
               compound.getString("id"),
               compound.getString("name"),
               DyeColor.byName(compound.getString("dyeColor"),DyeColor.WHITE),
               compound.getInt("textColor"),
               compound.getInt("textColorSub"),
               research,
               techQueue,
               compound.getInt("victoryPoints"),
               coins,
               compound.getInt("bankedResearchCoins"),
               mail
         );
      }catch(Exception e){
         log(3,e.toString());
         return null;
      }
   }
}
