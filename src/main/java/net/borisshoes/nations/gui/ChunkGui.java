package net.borisshoes.nations.gui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.CapturePoint;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.NationChunk;
import net.borisshoes.nations.gameplay.ResourceType;
import net.borisshoes.nations.integration.DynmapCalls;
import net.borisshoes.nations.items.GraphicalItem;
import net.borisshoes.nations.land.NationsLand;
import net.borisshoes.nations.utils.MiscUtils;
import net.borisshoes.nations.utils.NationsColors;
import net.borisshoes.nations.utils.NationsUtils;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.apache.commons.lang3.tuple.Triple;

public class ChunkGui extends SimpleGui {
   
   private final ChunkPos pos;
   
   public ChunkGui(ServerPlayerEntity player, ChunkPos pos){
      super(ScreenHandlerType.HOPPER, player, false);
      this.pos = pos;
      this.setTitle(Text.translatable("gui.nations.chunk_menu_title",pos.x,pos.z));
      build();
   }
   
   @Override
   public boolean onAnyClick(int index, ClickType type, SlotActionType action){
      Nation playerNation = Nations.getNation(player);
      NationChunk nationChunk = Nations.getChunk(pos);
      if(nationChunk == null){
         close();
         return false;
      }
      Nation nation = nationChunk.getControllingNation();
      ServerWorld world = player.getServer().getOverworld();
      boolean executor = (playerNation != null && playerNation.hasPermissions(player) && NationsLand.unclaimedOrSameNation(pos,playerNation)) || Nations.getPlayer(player).bypassesClaims();
      if(!executor || type != ClickType.MOUSE_LEFT) return true;
      boolean claimed = nationChunk.isClaimed();
      int farmLvl = nationChunk.getFarmlandLvl();
      boolean machinery = nationChunk.hasMachinery();
      boolean anchored = nationChunk.isAnchored();
      boolean explosions = nationChunk.areExplosionsOverridden();
      CapturePoint cap = Nations.getCapturePoint(pos);
      
      if(index == 1 && claimed && farmLvl < 5){ // Farmland
         int cost = NationsConfig.getInt(NationsRegistry.IMPROVEMENT_FARMLAND_COST_CFG);
         if(MiscUtils.removeItems(player,NationsRegistry.GROWTH_COIN_ITEM,cost)){
            nationChunk.setFarmlandLvl(farmLvl+1);
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.MASTER,1,0.7f+(farmLvl*0.2f));
            playerNation.updateChunk(nationChunk);
            build();
         }else{
            player.sendMessage(Text.translatable("text.nations.not_enough_growth_coins").formatted(Formatting.RED,Formatting.ITALIC));
         }
      }else if(index == 2){ // Machinery / Explosions / Claim / Influence
         if(claimed){
            if(machinery){
               nationChunk.setExplosionsOverridden(!explosions);
               player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.MASTER,1,explosions ? 0.5f : 1.5f);
               playerNation.updateChunk(nationChunk);
               build();
            }else{
               int cost = NationsConfig.getInt(NationsRegistry.IMPROVEMENT_MACHINERY_COST_CFG);
               if(MiscUtils.removeItems(player,NationsRegistry.GROWTH_COIN_ITEM,cost)){
                  nationChunk.setMachinery(true);
                  playerNation.updateChunk(nationChunk);
                  player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.MASTER,1,1.5f);
                  build();
               }else{
                  player.sendMessage(Text.translatable("text.nations.not_enough_growth_coins").formatted(Formatting.RED,Formatting.ITALIC));
               }
            }
         }else if(playerNation.canClaimOrInfluenceChunk(pos)){
            boolean claim = nationChunk.isInfluenced();
            int cost = NationsUtils.calculateClaimOrInfluenceCost(pos,playerNation,claim);
            if(MiscUtils.removeItems(player,NationsRegistry.GROWTH_COIN_ITEM,cost)){
               if(claim){
                  nationChunk.setClaimed(true);
               }else{
                  nationChunk.setInfluenced(true);
                  nationChunk.setControllingNationId(playerNation.getId());
               }
               playerNation.updateChunk(nationChunk);
               DynmapCalls.redrawNationBorder(playerNation);
               player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.MASTER,1,claim ? 1.5f : 1.2f);
               build();
            }else{
               player.sendMessage(Text.translatable("text.nations.not_enough_growth_coins").formatted(Formatting.RED,Formatting.ITALIC));
            }
         }
      }else if(index == 3 && claimed && !anchored){ // Anchored
         int cost = NationsConfig.getInt(NationsRegistry.IMPROVEMENT_ANCHORED_COST_CFG);
         if(MiscUtils.removeItems(player,NationsRegistry.GROWTH_COIN_ITEM,cost)){
            nationChunk.setAnchored(true);
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.MASTER,1,1.5f);
            build();
         }else{
            player.sendMessage(Text.translatable("text.nations.not_enough_growth_coins").formatted(Formatting.RED,Formatting.ITALIC));
         }
      }else if(index == 4){
         if(cap != null && cap.getControllingNation() != null && cap.getControllingNation().equals(playerNation) && nationChunk.getControllingNation() == null){ // Transfer capture point
            TransferCapturePointGui newGui = new TransferCapturePointGui(player, cap.getControllingNation(), cap);
            newGui.open();
         }else if(cap == null && player.isCreativeLevelTwoOp()){
            nationChunk.setArena(!nationChunk.isArena());
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.MASTER,1,!nationChunk.isArena() ? 0.5f : 1.5f);
            build();
         }
      }
      
      return true;
   }
   
   private void build(){
      Nation playerNation = Nations.getNation(player);
      NationChunk nationChunk = Nations.getChunk(pos);
      if(nationChunk == null){
         close();
         return;
      }
      Nation nation = nationChunk.getControllingNation();
      ServerWorld world = player.getServer().getOverworld();
      boolean executor = (playerNation != null && NationsLand.unclaimedOrSameNation(pos,playerNation) && playerNation.hasPermissions(player)) || !Nations.getPlayer(player).bypassesClaims();
      
      GuiElementBuilder empty = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.PAGE_BG, NationsColors.DARK_COLOR)).hideTooltip();
      for(int i = 0; i < size; i++){
         setSlot(i,empty);
      }
      
      Triple<Double,Double,Double> yields = nationChunk.getYield();
      GuiElementBuilder yieldItem = new GuiElementBuilder(NationsRegistry.RESEARCH_COIN_ITEM).hideDefaultTooltip();
      yieldItem.setName(Text.translatable("gui.nations.chunk_yield_title").formatted(Formatting.BOLD,Formatting.YELLOW));
      yieldItem.addLoreLine(Text.empty().append(Text.literal(String.format("%,03.2f", yields.getLeft())+" ").formatted(Formatting.GREEN,Formatting.BOLD)).append(Text.translatable(ResourceType.GROWTH.getTranslation()).append(Text.literal(" ")).append(Text.translatable("text.nations.coins")).formatted(Formatting.DARK_GREEN)));
      yieldItem.addLoreLine(Text.empty().append(Text.literal(String.format("%,03.2f", yields.getMiddle())+" ").formatted(Formatting.GOLD,Formatting.BOLD)).append(Text.translatable(ResourceType.MATERIAL.getTranslation()).append(Text.literal(" ")).append(Text.translatable("text.nations.coins")).formatted(Formatting.RED)));
      yieldItem.addLoreLine(Text.empty().append(Text.literal(String.format("%,03.2f", yields.getRight())+" ").formatted(Formatting.AQUA,Formatting.BOLD)).append(Text.translatable(ResourceType.RESEARCH.getTranslation()).append(Text.literal(" ")).append(Text.translatable("text.nations.coins")).formatted(Formatting.DARK_AQUA)));
      
      if(nation != null && nationChunk.isClaimed()){
         int farmLvl = nationChunk.getFarmlandLvl();
         boolean machinery = nationChunk.hasMachinery();
         boolean anchored = nationChunk.isAnchored();
         boolean explosions = nationChunk.areExplosionsOverridden();
         
         GuiElementBuilder farmItem = new GuiElementBuilder(Items.CARROT).hideDefaultTooltip();
         farmItem.setName(Text.translatable("gui.nations.farmland_title",farmLvl).formatted((farmLvl > 0 ? Formatting.GREEN : Formatting.RED),Formatting.BOLD));
         farmItem.addLoreLine(Text.translatable("gui.nations.farmland_rate", Text.literal(""+NationsLand.getRandomTickCount(world.getRegistryKey(),pos)).formatted(Formatting.GREEN)).formatted(Formatting.DARK_GREEN));
         if(farmLvl == 5){
            farmItem.glow();
         }else if(executor){
            int cost = NationsConfig.getInt(NationsRegistry.IMPROVEMENT_FARMLAND_COST_CFG);
            farmItem.addLoreLine(Text.empty());
            Text lore1 = Text.literal("")
                  .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD))
                  .append(Text.translatable("gui.nations.purchase_farmland").formatted(Formatting.DARK_PURPLE));
            Text lore2 = Text.literal("")
                  .append(Text.translatable("text.nations.cost").formatted(Formatting.AQUA))
                  .append(Text.literal(String.format("%,d", cost)+" ").formatted(Formatting.DARK_GREEN,Formatting.BOLD))
                  .append(ResourceType.GROWTH.getText())
                  .append(Text.literal(" "))
                  .append(Text.translatable("text.nations.coins").formatted(ResourceType.GROWTH.getTextColor()));
            farmItem.addLoreLine(lore1);
            farmItem.addLoreLine(lore2);
         }
         
         GuiElementBuilder machineryItem = new GuiElementBuilder(Items.REDSTONE).hideDefaultTooltip();
         machineryItem.setName(Text.translatable(machinery ? "gui.nations.machinery_title_enabled" : "gui.nations.machinery_title_disabled").formatted(machinery ? Formatting.RED : Formatting.DARK_RED,Formatting.BOLD));
         if(machinery){
            machineryItem.glow();
            if(executor){
               machineryItem.addLoreLine(Text.empty());
               Text lore1 = Text.literal("")
                     .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD))
                     .append(Text.translatable("gui.nations.toggle_explosions").formatted(Formatting.DARK_PURPLE));
               Text lore2 = Text.translatable(explosions ? "text.nations.explosions_enabled" : "text.nations.explosions_disabled").formatted(explosions ? Formatting.RED : Formatting.DARK_RED);
               machineryItem.addLoreLine(lore1);
               machineryItem.addLoreLine(lore2);
            }
         }else if(executor){
            int cost = NationsConfig.getInt(NationsRegistry.IMPROVEMENT_MACHINERY_COST_CFG);
            machineryItem.addLoreLine(Text.empty());
            Text lore1 = Text.literal("")
                  .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD))
                  .append(Text.translatable("gui.nations.purchase_machinery").formatted(Formatting.DARK_PURPLE));
            Text lore2 = Text.literal("")
                  .append(Text.translatable("text.nations.cost").formatted(Formatting.AQUA))
                  .append(Text.literal(String.format("%,d", cost)+" ").formatted(Formatting.DARK_GREEN,Formatting.BOLD))
                  .append(ResourceType.GROWTH.getText())
                  .append(Text.literal(" "))
                  .append(Text.translatable("text.nations.coins").formatted(ResourceType.GROWTH.getTextColor()));
            machineryItem.addLoreLine(lore1);
            machineryItem.addLoreLine(lore2);
         }
         
         GuiElementBuilder anchorItem = new GuiElementBuilder(Items.RESPAWN_ANCHOR).hideDefaultTooltip();
         anchorItem.setName(Text.translatable(anchored ? "gui.nations.anchored_title_enabled" : "gui.nations.anchored_title_disabled").formatted(anchored ? Formatting.LIGHT_PURPLE : Formatting.DARK_PURPLE,Formatting.BOLD));
         if(anchored){
            anchorItem.glow();
         }else if(executor){
            int cost = NationsConfig.getInt(NationsRegistry.IMPROVEMENT_ANCHORED_COST_CFG);
            anchorItem.addLoreLine(Text.empty());
            Text lore1 = Text.literal("")
                  .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD))
                  .append(Text.translatable("gui.nations.purchase_anchor").formatted(Formatting.DARK_PURPLE));
            Text lore2 = Text.literal("")
                  .append(Text.translatable("text.nations.cost").formatted(Formatting.AQUA))
                  .append(Text.literal(String.format("%,d", cost)+" ").formatted(Formatting.DARK_GREEN,Formatting.BOLD))
                  .append(ResourceType.GROWTH.getText())
                  .append(Text.literal(" "))
                  .append(Text.translatable("text.nations.coins").formatted(ResourceType.GROWTH.getTextColor()));
            anchorItem.addLoreLine(lore1);
            anchorItem.addLoreLine(lore2);
         }
         
         setSlot(1,farmItem);
         setSlot(2,machineryItem);
         setSlot(3,anchorItem);
      }else if(playerNation != null && playerNation.canClaimOrInfluenceChunk(pos)){
         boolean claim = nationChunk.isInfluenced();
         int cost = NationsUtils.calculateClaimOrInfluenceCost(pos,playerNation,claim);
         
         GuiElementBuilder claimItem = new GuiElementBuilder(NationsRegistry.GROWTH_COIN_ITEM).hideDefaultTooltip();
         claimItem.setName(Text.translatable(claim ? "gui.nations.claim_chunk_title" : "gui.nations.influence_chunk_title").formatted(Formatting.DARK_GREEN,Formatting.BOLD));
         claimItem.addLoreLine(Text.translatable(claim ? "gui.nations.claim_chunk_cost" : "gui.nations.influence_chunk_cost",
               Text.literal(String.format("%,d", cost)).formatted(Formatting.DARK_GREEN,Formatting.BOLD),
               ResourceType.GROWTH.getText(),
               Text.translatable("text.nations.coins").formatted(ResourceType.GROWTH.getTextColor())).formatted(Formatting.AQUA));
         
         if(executor){
            claimItem.addLoreLine(Text.empty());
            Text lore1 = Text.literal("")
                  .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD))
                  .append(Text.translatable(claim ? "gui.nations.purchase_claim" : "gui.nations.purchase_influence").formatted(Formatting.DARK_PURPLE));
            claimItem.addLoreLine(lore1);
         }
         
         setSlot(2,claimItem);
      }
      
      CapturePoint cap = Nations.getCapturePoint(pos);
      if(cap != null){
         GuiElementBuilder capItem = GuiElementBuilder.from(GraphicalItem.with(cap.getType().getGraphicItem())).hideDefaultTooltip();
         capItem.setName(Text.translatable("text.nations.capture_point_header",Text.translatable(cap.getType().getTranslation())).formatted(cap.getType().getTextColor(), Formatting.BOLD));
         capItem.addLoreLine(Text.translatable("text.nations.capture_point_yield",cap.getYield()).formatted(cap.getType().getTextColor()));
         MutableText controlText = cap.getControllingNation() == null ?
               Text.translatable("text.nations.uncontrolled") :
               Text.translatable("text.nations.controlled_by",cap.getControllingNation().getFormattedName()).withColor(cap.getControllingNation().getTextColorSub());
         capItem.addLoreLine(controlText);
         
         if(executor && cap.getControllingNation() != null && cap.getControllingNation().equals(playerNation) && nationChunk.getControllingNation() == null){
            capItem.addLoreLine(Text.empty());
            Text lore1 = Text.literal("")
                  .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD))
                  .append(Text.translatable("gui.nations.transfer_cap").formatted(Formatting.DARK_PURPLE));
            capItem.addLoreLine(lore1);
         }
         
         setSlot(4,capItem);
      }else if(nation != null){
         GuiElementBuilder nationItem = GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.GraphicItems.MONUMENT)).hideDefaultTooltip();
         nationItem.setName(nation.getFormattedName().formatted(Formatting.BOLD));
         if(nationChunk.isClaimed()){
            nationItem.addLoreLine(Text.translatable("text.nations.chunk_claimed_by",nation.getFormattedName()).withColor(nation.getTextColorSub()));
         }else{
            nationItem.addLoreLine(Text.translatable("text.nations.chunk_influenced_by",nation.getFormattedName()).withColor(nation.getTextColorSub()));
         }
         if(player.isCreativeLevelTwoOp()){
            boolean arena = nationChunk.isArena();
            nationItem.addLoreLine(Text.empty());
            nationItem.addLoreLine(Text.translatable(arena ? "gui.nations.arena_chunk_true" : "gui.nations.arena_chunk_false").formatted(Formatting.GOLD));
            Text lore = Text.literal("")
                  .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD))
                  .append(Text.translatable("gui.nations.toggle_arena_chunk").formatted(Formatting.DARK_PURPLE));
            nationItem.addLoreLine(lore);
         }
         setSlot(4,nationItem);
      }else{
         if(player.isCreativeLevelTwoOp()){
            boolean arena = nationChunk.isArena();
            GuiElementBuilder arenaItem = new GuiElementBuilder(Items.DIAMOND_SWORD).hideDefaultTooltip();
            arenaItem.setName(Text.translatable(arena ? "gui.nations.arena_chunk_true" : "gui.nations.arena_chunk_false").formatted(Formatting.BOLD,Formatting.GOLD));
            Text lore = Text.literal("")
                  .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD))
                  .append(Text.translatable("gui.nations.toggle_arena_chunk").formatted(Formatting.DARK_PURPLE));
            arenaItem.addLoreLine(lore);
            setSlot(4,arenaItem);
         }
      }
      
      setSlot(0,yieldItem);
   }
}
