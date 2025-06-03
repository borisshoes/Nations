package net.borisshoes.nations.gui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.CapturePoint;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.ResourceType;
import net.borisshoes.nations.gameplay.WarManager;
import net.borisshoes.nations.gui.core.GuiHelper;
import net.borisshoes.nations.integration.DynmapCalls;
import net.borisshoes.nations.items.GraphicalItem;
import net.borisshoes.nations.items.ResourceBullionItem;
import net.borisshoes.nations.utils.MiscUtils;
import net.borisshoes.nations.utils.NationsColors;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;

import java.util.*;
import java.util.stream.Stream;

public class CapturePointGui extends SimpleGui implements InventoryChangedListener {
   
   private final CapturePoint capturePoint;
   private Mode mode;
   private final SimpleInventory inventory = new SimpleInventory(1);
   private boolean updating = false;
   
   public CapturePointGui(ServerPlayerEntity player, CapturePoint capturePoint, Mode mode){
      super(mode == Mode.AUCTION ? ScreenHandlerType.GENERIC_9X6 : ScreenHandlerType.GENERIC_3X3, player, false);
      this.capturePoint = capturePoint;
      this.mode = mode;
      this.inventory.addListener(this);
      setTitle(Text.translatable("text.nations.capture_point_header",Text.translatable(capturePoint.getType().getTranslation())));
      this.build();
   }
   
   @Override
   public boolean onAnyClick(int index, ClickType type, SlotActionType action){
      if(index == 4){
         if(this.mode == Mode.DEFEND){
            if(WarManager.capIsContested(capturePoint)){
               WarManager.defendContest(capturePoint,player);
            }else{
               player.sendMessage(Text.translatable("text.nations.war_not_contested").formatted(Formatting.RED));
               close();
            }
         }else if(this.mode == Mode.COLLECT){
            capturePoint.collectCoins(player.getServer().getOverworld());
            close();
         }else if(this.mode == Mode.CONTEST){
            boolean canContest = WarManager.canContestCap(capturePoint,player);
            if(canContest){
               int cost = capturePoint.calculateAttackCost(Nations.getNation(player));
               if(MiscUtils.removeItems(player,NationsRegistry.GROWTH_COIN_ITEM,cost)){
                  WarManager.addPendingContest(capturePoint,player);
                  close();
               }else{
                  player.sendMessage(Text.translatable("text.nations.not_enough_growth_coins").formatted(Formatting.RED,Formatting.ITALIC));
               }
            }else{
               player.sendMessage(Text.translatable("text.nations.war_cannot_contest").formatted(Formatting.RED));
               close();
            }
         }
      }
      
      
      return super.onAnyClick(index, type, action);
   }
   
   @Override
   public void onTick(){
      if(player.getServer().getTicks() % 20 == 0){
         build();
      }
   }
   
   public void build(){
      switch(mode){
         case DEFEND -> buildDefendGui();
         case COLLECT -> buildCollectionGui();
         case CONTEST -> buildContestGui();
         case AUCTION -> buildAuctionGui();
      }
   }
   
   private void buildCollectionGui(){
      GuiHelper.outlineGUI(this, getGuiColor(), Text.translatable("gui.nations.cap_collect_border").formatted(capturePoint.getType().getTextColor()));
      GuiElementBuilder center = new GuiElementBuilder(capturePoint.getType().getCoin()).hideDefaultTooltip();
      int toCollect = capturePoint.getStoredCoins();
      if(toCollect == 0){
         center.setName(Text.translatable("gui.nations.cap_empty").formatted(capturePoint.getType().getTextColor()));
      }else{
         center.setName(Text.translatable("gui.nations.cap_not_empty").formatted(capturePoint.getType().getTextColor()));
         Text lore = Text.literal("")
               .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE))
               .append(Text.translatable("gui.nations.cap_collect_sub").formatted(Formatting.DARK_PURPLE))
               .append(Text.literal(toCollect+" ").formatted(capturePoint.getType().getTextColor(), Formatting.BOLD))
               .append(Text.translatable(capturePoint.getType().getTranslation()).formatted(capturePoint.getType().getTextColor()))
               .append(Text.literal(" "))
               .append(Text.translatable(toCollect > 1 ? "text.nations.coins" : "text.nations.coin").formatted(capturePoint.getType().getTextColor()));
         center.addLoreLine(lore);
      }
      setSlot(4,center);
   }
   
   private void buildContestGui(){
      GuiHelper.outlineGUI(this, getGuiColor(), Text.translatable("gui.nations.cap_contest_border").formatted(capturePoint.getType().getTextColor()));
      GuiElementBuilder center = new GuiElementBuilder(Items.DIAMOND_SWORD).hideDefaultTooltip();
      center.setName(Text.translatable("gui.nations.cap_contest").formatted(capturePoint.getType().getTextColor()));
      boolean canContest = WarManager.canContestCap(capturePoint,player);
      Text lore;
      if(canContest){
         int cost = capturePoint.calculateAttackCost(Nations.getNation(player));
         lore = Text.literal("")
               .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE))
               .append(Text.translatable("gui.nations_cap_contest_sub",Text.literal(String.format("%,d",cost)).formatted(Formatting.GREEN)).formatted(Formatting.DARK_PURPLE));
      }else{
         lore = Text.translatable("text.nations.war_cannot_contest").formatted(Formatting.RED);
      }
      
      center.addLoreLine(lore);
      setSlot(4,center);
   }
   
   private void buildDefendGui(){
      GuiHelper.outlineGUI(this, getGuiColor(), Text.translatable("gui.nations.cap_defend_border").formatted(capturePoint.getType().getTextColor()));
      GuiElementBuilder center = new GuiElementBuilder(Items.SHIELD).hideDefaultTooltip();
      center.setName(Text.translatable("gui.nations.cap_defend").formatted(capturePoint.getType().getTextColor()));
      Text lore = Text.literal("")
            .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE))
            .append(Text.translatable("gui.nations_cap_defend_sub").formatted(Formatting.DARK_PURPLE));
      center.addLoreLine(lore);
      setSlot(4,center);
   }
   
   private void buildAuctionGui(){
      if(capturePoint.getControllingNation() != null) close();
      HashMap<String,Double> influence = capturePoint.getInfluence();
      List<Nation> topNations = new ArrayList<>(Stream.concat(influence.entrySet().stream()
                  .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                  .map(e -> Nations.getNation(e.getKey()))
                  .filter(Objects::nonNull),
            Stream.generate(() -> null)
      ).limit(3).toList());
      Nation playerNation = Nations.getNation(player);
      if(playerNation == null){
         close();
         return;
      }
      double playerNationInf = influence.getOrDefault(playerNation.getId(),0.0);
      if(!topNations.contains(playerNation)){
         topNations.set(2,playerNation);
      }
      
      int i = 18;
      double maxInf = topNations.stream().map(n -> n == null ? 0 : influence.getOrDefault(n.getId(),0.0)).reduce(Double::max).orElse(0.0);
      for(Nation nation : topNations){
         double inf = nation == null ? 0 : influence.getOrDefault(nation.getId(),0.0);
         int num = maxInf == 0 ? 0 : (int) Math.ceil(inf*7.0 / maxInf);
         
         for(int j = 0; j < 9; j++){
            GraphicalItem.GraphicItems id = GraphicalItem.GraphicItems.MENU_HORIZONTAL;
            int color;
            if(j == 0){
               id = GraphicalItem.GraphicItems.MENU_LEFT_CONNECTOR;
               color = nation != null ? nation.getTextColor() : NationsColors.DARK_COLOR;
            }else if(j == 8){
               id = GraphicalItem.GraphicItems.MENU_RIGHT_CONNECTOR;
               color = nation != null ? nation.getTextColor() : NationsColors.DARK_COLOR;
            }else{
               color = nation != null && num >= j ? nation.getTextColor() : NationsColors.DARK_COLOR;
            }
            GuiElementBuilder elem = GuiElementBuilder.from(GraphicalItem.withColor(id,color)).hideDefaultTooltip();
            if(color != NationsColors.DARK_COLOR && id != GraphicalItem.GraphicItems.MENU_HORIZONTAL){
               elem.setItem(NationsRegistry.GROWTH_COIN_ITEM);
               elem.setComponent(DataComponentTypes.DYED_COLOR,new DyedColorComponent(color,false));
            }
            if(playerNation == nation) elem.glow();
            if(nation == null){
               elem.hideTooltip();
            }else{
               Text text = Text.literal("")
                     .append(nation.getFormattedName())
                     .append(Text.translatable("gui.nations.cap_auction_contributed",Text.literal(String.format("%,.3f", inf)).withColor(nation.getTextColor())).formatted(Formatting.DARK_PURPLE));
               elem.setName(text);
            }
            
            setSlot(i+j,elem);
         }
         i+=9;
      }
      
      GuiElementBuilder leftArrow = GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.GraphicItems.LEFT_ARROW)).hideDefaultTooltip();
      GuiElementBuilder rightArrow = GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.GraphicItems.RIGHT_ARROW)).hideDefaultTooltip();
      Text arrowText = Text.translatable("gui.nations.cap_insert_coins",
            Text.literal("").formatted(ResourceType.GROWTH.getTextColor(),Formatting.BOLD)
                  .append(Text.translatable(ResourceType.GROWTH.getTranslation()))
                  .append(Text.literal(" "))
                  .append(Text.translatable("text.nations.coins"))
      ).formatted(Formatting.DARK_PURPLE);
      leftArrow.setName(arrowText);
      rightArrow.setName(arrowText);
      
      setSlot(3,rightArrow);
      setSlot(5,leftArrow);
      
      long aucStart = capturePoint.getAuctionStartTime();
      long aucEnd = capturePoint.getAuctionEndTime();
      long duration = aucEnd - aucStart;
      long now = System.currentTimeMillis();
      long tillEnd = aucEnd - now;
      float percentage = MathHelper.clamp((float) (now-aucStart) / duration,0.0f,1.0f);
      
      MutableText auctionNotActiveText = Text.translatable("gui.nations.cap_auction_not_started").formatted(Formatting.YELLOW);
      MutableText auctionDurationLore = Text.translatable("gui.nations.cap_auction_not_started_duration",MiscUtils.getTimeDiff(duration).formatted(Formatting.LIGHT_PURPLE)).formatted(Formatting.DARK_PURPLE);
      MutableText durationText = Text.translatable("gui.nations.cap_auction_duration").formatted(Formatting.YELLOW);
      MutableText durationLore = Text.translatable("gui.nations.cap_auction_duration_sub",MiscUtils.getTimeDiff(tillEnd).formatted(Formatting.LIGHT_PURPLE)).formatted(Formatting.DARK_PURPLE);
      GuiElementBuilder clock = new GuiElementBuilder(Items.CLOCK).hideDefaultTooltip();
      if(aucStart == 0){
         clock.setName(auctionNotActiveText);
         clock.addLoreLine(auctionDurationLore);
      }else{
         clock.setName(durationText);
         clock.addLoreLine(durationLore);
      }
      setSlot(45,clock);
      setSlot(53,clock);
      
      Pair<ChunkPos,Double> infData = capturePoint.calculateNearestInfluence(playerNation);
      double minInf = NationsConfig.getDouble(NationsRegistry.CAPTURE_POINT_AUCTION_MOD_MIN_CFG);
      if(infData.getRight() < minInf && aucStart == 0){
         GuiElementBuilder error = GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.GraphicItems.CANCEL)).hideDefaultTooltip();
         error.setName(Text.translatable("gui.nations.cap_too_far").formatted(Formatting.RED,Formatting.BOLD));
         setSlot(4,error);
      }else{
         setSlotRedirect(4,new GrowthCoinSlot(this.inventory,0,0,0));
      }
      
      
      int num = (int) Math.ceil(percentage*7.0);
      for(int j = 1; j <= 7; j++){
         int color = num >= j ? NationsColors.GOLD_COLOR : NationsColors.DARK_GOLD_COLOR;
         GuiElementBuilder elem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_BOTTOM,color)).hideDefaultTooltip();
         if(aucStart == 0){
            elem.setName(auctionNotActiveText);
            elem.addLoreLine(auctionDurationLore);
         }else{
            elem.setName(durationText);
            elem.addLoreLine(durationLore);
         }
         setSlot(45+j,elem);
      }
      
      MutableText spacerText = Text.translatable("gui.nations.cap_auction_spacer").formatted(capturePoint.getType().getTextColor());
      int color = capturePoint.getType().getCoin().getColor();
      setSlot(0, GuiElementBuilder.from((GraphicalItem.withColor(GraphicalItem.GraphicItems.PAGE_BG,color))).hideDefaultTooltip().setName(spacerText));
      setSlot(1, GuiElementBuilder.from((GraphicalItem.withColor(GraphicalItem.GraphicItems.PAGE_BG,color))).hideDefaultTooltip().setName(spacerText));
      setSlot(7, GuiElementBuilder.from((GraphicalItem.withColor(GraphicalItem.GraphicItems.PAGE_BG,color))).hideDefaultTooltip().setName(spacerText));
      setSlot(8, GuiElementBuilder.from((GraphicalItem.withColor(GraphicalItem.GraphicItems.PAGE_BG,color))).hideDefaultTooltip().setName(spacerText));
      setSlot(2, GuiElementBuilder.from((GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_VERTICAL,color))).hideDefaultTooltip().setName(spacerText));
      setSlot(6, GuiElementBuilder.from((GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_VERTICAL,color))).hideDefaultTooltip().setName(spacerText));
      setSlot(9, GuiElementBuilder.from((GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP_LEFT,color))).hideDefaultTooltip().setName(spacerText));
      setSlot(17, GuiElementBuilder.from((GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP_RIGHT,color))).hideDefaultTooltip().setName(spacerText));
      setSlot(10, GuiElementBuilder.from((GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP,color))).hideDefaultTooltip().setName(spacerText));
      setSlot(16, GuiElementBuilder.from((GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP,color))).hideDefaultTooltip().setName(spacerText));
      setSlot(11, GuiElementBuilder.from((GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_BOTTOM_CONNECTOR,color))).hideDefaultTooltip().setName(spacerText));
      setSlot(15, GuiElementBuilder.from((GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_BOTTOM_CONNECTOR,color))).hideDefaultTooltip().setName(spacerText));
      setSlot(12, GuiElementBuilder.from((GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_HORIZONTAL,color))).hideDefaultTooltip().setName(spacerText));
      setSlot(14, GuiElementBuilder.from((GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_HORIZONTAL,color))).hideDefaultTooltip().setName(spacerText));
      
      GuiElementBuilder coin = new GuiElementBuilder(capturePoint.getType().getCoin()).hideDefaultTooltip();
      coin.setName(spacerText);
      MutableText coinLore1 = Text.translatable("gui.nations.cap_auction_coin_sub_1",Text.literal(String.format("%,.3f", playerNationInf)).withColor(playerNation.getTextColor())).formatted(Formatting.LIGHT_PURPLE);
      MutableText coinLore3 = Text.translatable("gui.nations.cap_auction_coin_sub_3",Text.literal(infData.getLeft().toString()).withColor(playerNation.getTextColor())).formatted(Formatting.DARK_PURPLE);
      MutableText coinLore2 = Text.translatable("gui.nations.cap_auction_coin_sub_2",Text.literal(String.format("%.3f", infData.getRight())+"x").withColor(playerNation.getTextColor())).formatted(Formatting.DARK_PURPLE);
      coin.addLoreLine(coinLore1);
      coin.addLoreLine(coinLore3);
      coin.addLoreLine(coinLore2);
      setSlot(13,coin);
   }
   
   private int getGuiColor(){
      return switch(capturePoint.getType()){
         case GROWTH -> NationsColors.GROWTH_CAP_COLOR;
         case MATERIAL -> NationsColors.MATERIAL_CAP_COLOR;
         case RESEARCH -> NationsColors.RESEARCH_CAP_COLOR;
      };
   }
   
   @Override
   public void onInventoryChanged(Inventory inv){
      if(!updating){
         updating = true;
         Nation playerNation = Nations.getNation(player);
         if(playerNation == null){
            updating = false;
            close();
            return;
         }
         ItemStack stack = inv.getStack(0);
         if(!stack.isEmpty()){
            int count = stack.getCount();
            if(stack.getItem() instanceof ResourceBullionItem){
               capturePoint.addCoins(playerNation,count*1000);
            }else{
               capturePoint.addCoins(playerNation,count);
            }
            inv.setStack(0,ItemStack.EMPTY);
            build();
         }
      }
      updating = false;
   }
   
   @Override
   public void onClose(){
      MiscUtils.returnItems(inventory,player);
   }
   
   public enum Mode {
      AUCTION,
      COLLECT,
      CONTEST,
      DEFEND
   }
}
