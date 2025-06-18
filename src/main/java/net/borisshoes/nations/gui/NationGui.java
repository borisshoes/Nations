package net.borisshoes.nations.gui;

import com.mojang.authlib.GameProfile;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.borisshoes.nations.gameplay.CapturePoint;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.NationChunk;
import net.borisshoes.nations.gameplay.ResourceType;
import net.borisshoes.nations.gui.core.GuiFilter;
import net.borisshoes.nations.gui.core.GuiHelper;
import net.borisshoes.nations.gui.core.GuiSort;
import net.borisshoes.nations.items.GraphicalItem;
import net.borisshoes.nations.items.ResourceBullionItem;
import net.borisshoes.nations.research.ResearchTech;
import net.borisshoes.nations.utils.MiscUtils;
import net.borisshoes.nations.utils.NationsColors;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class NationGui extends SimpleGui implements InventoryChangedListener {
   
   private final Nation nation;
   private Mode mode;
   private final SimpleInventory inventory = new SimpleInventory(1);
   private boolean updating = false;
   
   private int pageNum;
   private GuiSort sort;
   private GuiFilter filter;
   private List itemList;
   
   public NationGui(ServerPlayerEntity player, Nation nation, Mode mode){
      super(getTypeFromMode(mode), player, false);
      this.nation = nation;
      this.mode = mode;
      if(mode == Mode.RESEARCH){
         inventory.addListener(this);
         sort = ResearchSort.getStaticDefault();
         filter = ResearchFilter.getStaticDefault();
      }else if(mode == Mode.MEMBERS){
         sort = PlayerSort.getStaticDefault();
         filter = PlayerFilter.getStaticDefault();
      }else if(mode == Mode.CAPTURE_POINTS){
         sort = CapturePointSort.getStaticDefault();
         filter = CapturePointFilter.getStaticDefault();
      }
      setTitle(Text.literal(nation.getName()));
      build();
   }
   
   @Override
   public boolean onAnyClick(int index, ClickType type, SlotActionType action){
      ResearchFilter.setNation(nation);
      CapturePointSort.setNation(nation);
      
      if(mode != Mode.MENU){
         int sortInd = 0;
         int filterInd = 8;
         int prevInd = mode == Mode.RESEARCH ? 36 : 45;
         int nextInd = mode == Mode.RESEARCH ? 44 : 53;
         int itemsPerPage = mode == Mode.RESEARCH ? 21 : 28;
         if(index == sortInd){
            sort = sort.cycle(sort,type.isRight);
            build();
         }else if(index == filterInd){
            filter = filter.cycle(filter,type.isRight);
            build();
         }else if(index == prevInd){
            if(pageNum > 1){
               pageNum -= 1;
               build();
            }
         }else if(index == nextInd){
            int numPages = (int) (Math.ceil((float)itemList.size()/itemsPerPage));
            if(pageNum < numPages){
               pageNum += 1;
               build();
            }
         }
      }else{
         if(index == 1){
            nation.collectCoins(player.getServer().getOverworld());
            close();
         }else if(index == 3){
            NationGui newGui = new NationGui(player,nation,Mode.CAPTURE_POINTS);
            newGui.open();
         }else if(index == 5){
            NationGui newGui = new NationGui(player,nation,Mode.MEMBERS);
            newGui.open();
         }else if(index == 7){
            NationGui newGui = new NationGui(player,nation,Mode.RESEARCH);
            newGui.open();
         }
      }
      return true;
   }
   
   private void build(){
      ResearchFilter.setNation(nation);
      CapturePointSort.setNation(nation);
      switch(mode){
         case MENU -> buildMenu();
         case MEMBERS -> buildMembers();
         case RESEARCH -> buildResearch();
         case CAPTURE_POINTS -> buildCapturePoints();
      }
   }
   
   private void manageMemberCallback(UUID member, ClickType clickType){
      if(clickType.isLeft){
         if(!nation.isExecutor(member) && !nation.isLeader(member)){
            nation.promote(member);
         }
      }else if(clickType.isRight){
         if(nation.isExecutor(member)){
            nation.demote(member);
         }
      }
      build();
   }
   
   private void transferCapCallback(CapturePoint capturePoint, ClickType clickType){
      NationChunk capChunk = Nations.getChunk(capturePoint.getChunkPos());
      if(capChunk != null && capChunk.getControllingNation() == null){
         TransferCapturePointGui newGui = new TransferCapturePointGui(player,nation,capturePoint);
         newGui.open();
      }
   }
   
   private void queueTech(ResearchTech tech, ClickType clickType){
      if(clickType.isRight){
         nation.queueTechFirst(tech);
      }else{
         nation.queueTech(tech);
      }
      build();
   }
   
   private void modifyTechQueue(ResearchTech tech, ClickType clickType){
      if(clickType.shift && clickType.isLeft){
         nation.dequeueTech(tech);
      }else if(clickType.isRight){
         nation.moveQueuedTechDown(tech);
      }else{
         nation.moveQueuedTechUp(tech);
      }
      build();
   }
   
   private void buildMenu(){
      GuiElementBuilder blank = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.PAGE_BG, nation.getTextColor())).hideTooltip();
      for(int i = 0; i < getSize(); i++){
         setSlot(i,blank);
      }
      int mailSize = nation.getMailbox().size();
      
      GuiElementBuilder collectItem = new GuiElementBuilder(NationsRegistry.GROWTH_COIN_ITEM).hideDefaultTooltip();
      Map<ResourceType,Integer> storedCoins = nation.getStoredCoins();
      Map<ResourceType,Integer> dailyYield = nation.calculateCoinYield(Nations.SERVER.getOverworld());
      collectItem.setName(Text.translatable(mailSize > 0 ? "gui.nations.nation_collect_with_mail" : "gui.nations.nation_collect_title").formatted(Formatting.YELLOW,Formatting.BOLD));
      collectItem.addLoreLine(Text.translatable("gui.nations.nation_yield",
            Text.literal(String.format("%,d", storedCoins.get(ResourceType.GROWTH))).formatted(Formatting.GREEN,Formatting.BOLD),
            Text.translatable(ResourceType.GROWTH.getTranslation()).formatted(Formatting.DARK_GREEN),
            Text.translatable("text.nations.coins").formatted(Formatting.DARK_GREEN),
            Text.literal(String.format("%,d", dailyYield.get(ResourceType.GROWTH))).formatted(Formatting.GREEN,Formatting.BOLD)
      ).formatted(Formatting.DARK_GREEN));
      
      collectItem.addLoreLine(Text.translatable("gui.nations.nation_yield",
            Text.literal(String.format("%,d", storedCoins.get(ResourceType.MATERIAL))).formatted(Formatting.GOLD,Formatting.BOLD),
            Text.translatable(ResourceType.MATERIAL.getTranslation()).formatted(Formatting.RED),
            Text.translatable("text.nations.coins").formatted(Formatting.RED),
            Text.literal(String.format("%,d", dailyYield.get(ResourceType.MATERIAL))).formatted(Formatting.GOLD,Formatting.BOLD)
      ).formatted(Formatting.RED));
      
      collectItem.addLoreLine(Text.translatable("gui.nations.nation_yield",
            Text.literal(String.format("%,d", storedCoins.get(ResourceType.RESEARCH))).formatted(Formatting.AQUA,Formatting.BOLD),
            Text.translatable(ResourceType.RESEARCH.getTranslation()).formatted(Formatting.DARK_AQUA),
            Text.translatable("text.nations.coins").formatted(Formatting.DARK_AQUA),
            Text.literal(String.format("%,d", dailyYield.get(ResourceType.RESEARCH))).formatted(Formatting.AQUA,Formatting.BOLD)
      ).formatted(Formatting.DARK_AQUA));

      collectItem.addLoreLine(Text.empty());
      if(mailSize > 0){
         collectItem.addLoreLine(Text.translatable("gui.nations.items_in_mail",Text.literal(""+mailSize).formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD)).formatted(Formatting.BLUE));
         collectItem.addLoreLine(Text.empty());
      }
      collectItem.addLoreLine(Text.literal("")
            .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE))
            .append(Text.translatable("gui.nations.nation_collect_sub").formatted(Formatting.DARK_PURPLE)));
      setSlot(1, collectItem);
      
      GuiElementBuilder researchItem = new GuiElementBuilder(NationsRegistry.RESEARCH_COIN_ITEM).hideDefaultTooltip();
      researchItem.setName(Text.translatable("gui.nations.nation_research_title").formatted(Formatting.AQUA,Formatting.BOLD));
      researchItem.addLoreLine(Text.literal("")
            .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE))
            .append(Text.translatable("gui.nations.nation_research_sub").formatted(Formatting.DARK_PURPLE)));
      setSlot(7, researchItem);
      
      GuiElementBuilder membersItem = new GuiElementBuilder(Items.PLAYER_HEAD).hideDefaultTooltip().setSkullOwner(player.getGameProfile(),player.getServer());
      membersItem.setName(Text.translatable("gui.nations.nation_members_title").formatted(Formatting.RED,Formatting.BOLD));
      membersItem.addLoreLine(Text.literal("")
            .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE))
            .append(Text.translatable("gui.nations.nation_members_sub").formatted(Formatting.DARK_PURPLE)));
      setSlot(5, membersItem);
      
      GuiElementBuilder capItem = GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.GraphicItems.GROWTH_CAPTURE_POINT)).hideDefaultTooltip();
      capItem.setName(Text.translatable("gui.nations.nation_caps_title").formatted(Formatting.AQUA,Formatting.BOLD));
      capItem.addLoreLine(Text.literal("")
            .append(Text.translatable("gui.nations.click").formatted(Formatting.LIGHT_PURPLE))
            .append(Text.translatable("gui.nations.nation_caps_sub").formatted(Formatting.DARK_PURPLE)));
      setSlot(3, capItem);
   }
   
   private void buildMembers(){
      GuiHelper.outlineGUI(9,6,this,nation.getTextColorSub(),Text.empty(),null);
      List<UUID> members = new ArrayList<>(nation.getMembers());
      this.itemList = members;
      
      Function<UUID, GuiElementBuilder> builder = id -> {
         String permissionLevel = nation.isLeader(id) ? "text.nations.leader" : nation.isExecutor(id) ? "text.nations.executor" : "text.nations.member";
         GameProfile playerGameProf;
         GuiElementBuilder elem;
         try{
            playerGameProf = player.getServer().getUserCache().getByUuid(id).orElseThrow();
            elem = new GuiElementBuilder(Items.PLAYER_HEAD).setSkullOwner(playerGameProf,player.getServer()).hideDefaultTooltip();
            elem.setName(Text.literal(playerGameProf.getName()).withColor(nation.getTextColorSub()));
         }catch(Exception e){
            elem = new GuiElementBuilder(Items.SKELETON_SKULL).hideDefaultTooltip();
            elem.setName(Text.literal("<UNKNOWN>").withColor(nation.getTextColorSub()));
         }
         INationsProfileComponent data = Nations.getPlayerOrOffline(id);
         long lastOnline = data == null ? 0 : data.getLastOnline();
         MutableText time = MiscUtils.getTimeDiff(System.currentTimeMillis() - lastOnline);
         elem.addLoreLine(Text.translatable("text.nations.permission_level",Text.translatable(permissionLevel).withColor(nation.getTextColor())).withColor(nation.getTextColorSub()));
         elem.addLoreLine(Text.translatable("text.nations.last_online_time",time.withColor(nation.getTextColorSub())).withColor(nation.getTextColor()));
         elem.addLoreLine(Text.empty());
         if(!nation.isExecutor(id) && !nation.isLeader(id)){
            elem.addLoreLine(Text.literal("").append(Text.translatable("gui.nations.click").withColor(nation.getTextColorSub())).append(Text.translatable("gui.nations.nation_promote").formatted(Formatting.GREEN)));
         }else if(nation.isExecutor(id)){
            elem.addLoreLine(Text.literal("").append(Text.translatable("gui.nations.right_click").withColor(nation.getTextColor())).append(Text.translatable("gui.nations.nation_demote").formatted(Formatting.RED)));
         }
         return elem;
      };
      GuiElementBuilder blank = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.PAGE_BG, NationsColors.PAGE_COLOR)).hideTooltip();
      
      this.pageNum = GuiHelper.setupPageGui(this,pageNum,9,6,members,builder,filter,sort,blank,this::manageMemberCallback);
   }
   
   private void buildResearch(){
      final int color = 0x2694c8;
      GuiHelper.outlineGUI(9,5,this, color,Text.empty(),null);
      List<ResearchTech> techs = nation.availableTechs();
      this.itemList = techs;
      double increase = NationsConfig.getDouble(NationsRegistry.SCHOLARSHIP_INCREASE_CFG);
      int scholarshipLvl = nation.getBuffLevel(NationsRegistry.SCHOLARSHIP);
      
      Function<ResearchTech, GuiElementBuilder> builder = tech -> {
         GuiElementBuilder elem = GuiElementBuilder.from(tech.getShowItem()).hideDefaultTooltip();
         int rate = (int)(tech.getConsumptionRate()*(1+increase*scholarshipLvl));
         elem.setName(tech.getName().formatted(Formatting.BOLD,Formatting.AQUA));
         elem.addLoreLine(Text.empty()
               .append(Text.translatable("text.nations.cost").formatted(Formatting.BLUE))
               .append(Text.literal(String.format("%,d",tech.getCost())+" ").formatted(Formatting.AQUA))
               .append(Text.translatable("text.nations.coins").formatted(Formatting.DARK_AQUA))
               .append(Text.literal("  |  ").formatted(Formatting.BLUE))
               .append(Text.translatable("text.nations.rate").formatted(Formatting.BLUE))
               .append(Text.literal(String.format("%,d",rate)+" ").formatted(Formatting.AQUA))
               .append(Text.translatable("text.nations.coins_per_day").formatted(Formatting.DARK_AQUA))
         );
         int prog = nation.getProgress(tech);
         if(prog > 0){
            float percent = 100 * (float) prog / tech.getCost();
            elem.addLoreLine(Text.empty()
                  .append(Text.translatable("text.nations.progress").formatted(Formatting.BLUE))
                  .append(Text.literal(String.format("%,d",prog)+"/"+String.format("%,d",tech.getCost())+" ").formatted(Formatting.AQUA))
                  .append(Text.translatable("text.nations.coins").formatted(Formatting.DARK_AQUA))
                  .append(Text.literal(" ("+String.format("%03.2f",percent)+"%)").formatted(Formatting.AQUA))
            );
         }
         elem.addLoreLine(Text.empty());
         elem.addLoreLine(Text.literal("").append(Text.translatable("gui.nations.click").formatted(Formatting.DARK_AQUA)).append(Text.translatable("gui.nations.research_queue").formatted(Formatting.DARK_PURPLE)));
         elem.addLoreLine(Text.literal("").append(Text.translatable("gui.nations.right_click").formatted(Formatting.AQUA)).append(Text.translatable("gui.nations.research_start").formatted(Formatting.DARK_PURPLE)));
         return elem;
      };
      GuiElementBuilder blank = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.PAGE_BG, NationsColors.PAGE_COLOR)).hideTooltip();
      
      GuiElementBuilder leftArrow = GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.GraphicItems.LEFT_ARROW)).hideDefaultTooltip();
      GuiElementBuilder rightArrow = GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.GraphicItems.RIGHT_ARROW)).hideDefaultTooltip();
      Text arrowText = Text.translatable("gui.nations.research_insert_coins",
            Text.literal("").formatted(ResourceType.RESEARCH.getTextColor(),Formatting.BOLD)
                  .append(Text.translatable(ResourceType.RESEARCH.getTranslation()))
                  .append(Text.literal(" "))
                  .append(Text.translatable("text.nations.coins"))
      ).formatted(Formatting.DARK_PURPLE);
      leftArrow.setName(arrowText);
      rightArrow.setName(arrowText);
      setSlot(39,rightArrow);
      setSlot(41,leftArrow);
      clearSlot(40);
      setSlotRedirect(40,new ResearchCoinSlot(this.inventory,0,0,0));
      GuiElementBuilder leftCorner = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_BOTTOM_LEFT,color)).hideTooltip();
      GuiElementBuilder rightCorner = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_BOTTOM_RIGHT,color)).hideTooltip();
      setSlot(45,leftCorner);
      setSlot(53,rightCorner);
      
      int researchBudget = nation.getResearchBudget();
      GuiElementBuilder managerItem = new GuiElementBuilder(NationsRegistry.RESEARCH_COIN_ITEM).hideDefaultTooltip();
      managerItem.setName(Text.translatable("gui.nations.research_manager_title").formatted(Formatting.BOLD,Formatting.AQUA));
      managerItem.addLoreLine(Text.translatable("gui.nations.research_budget",
            Text.literal(String.format("%,d", researchBudget)).formatted(Formatting.AQUA,Formatting.BOLD),
            Text.translatable(ResourceType.RESEARCH.getTranslation()).formatted(Formatting.DARK_AQUA),
            Text.translatable("text.nations.coins").formatted(Formatting.DARK_AQUA)
      ).formatted(Formatting.BLUE));
      ResearchTech activeTech = nation.getActiveTech();
      MutableText researchText = activeTech == null ? Text.translatable("text.nations.nothing") : activeTech.getName();
      float percentage = activeTech == null ? 0 : 100*((float) nation.getProgress(activeTech) / activeTech.getCost());
      managerItem.addLoreLine(Text.translatable("text.nations.currently_researching",
            researchText.formatted(Formatting.LIGHT_PURPLE),
            Text.literal(String.format("%03.2f",percentage)).formatted(Formatting.AQUA,Formatting.BOLD)
      ).formatted(Formatting.DARK_AQUA));
      setSlot(4,managerItem);
      
      List<ResearchTech> techQueue = nation.getTechQueue().stream().toList();
      
      int queueTicks = 0;
      for(int i = 0; i < 7; i++){
         int index = 46+i;
         
         if(i < techQueue.size()){
            ResearchTech tech = techQueue.get(i);
            int rate = (int)(tech.getConsumptionRate()*(1+increase*scholarshipLvl));
            GuiElementBuilder elem = GuiElementBuilder.from(tech.getShowItem()).hideDefaultTooltip();
            elem.setName(tech.getName().formatted(Formatting.BOLD,Formatting.AQUA));
            elem.addLoreLine(Text.empty()
                  .append(Text.translatable("text.nations.cost").formatted(Formatting.BLUE))
                  .append(Text.literal(String.format("%,d",tech.getCost())+" ").formatted(Formatting.AQUA))
                  .append(Text.translatable("text.nations.coins").formatted(Formatting.DARK_AQUA))
                  .append(Text.literal("  |  ").formatted(Formatting.BLUE))
                  .append(Text.translatable("text.nations.rate").formatted(Formatting.BLUE))
                  .append(Text.literal(String.format("%,d",rate)+" ").formatted(Formatting.AQUA))
                  .append(Text.translatable("text.nations.coins_per_day").formatted(Formatting.DARK_AQUA))
            );
            int prog = nation.getProgress(tech);
            int cost = tech.getCost();
            if(prog > 0){
               float percent = 100 * (float) prog / cost;
               elem.addLoreLine(Text.empty()
                     .append(Text.translatable("text.nations.progress").formatted(Formatting.BLUE))
                     .append(Text.literal(String.format("%,d",prog)+"/"+String.format("%,d",tech.getCost())+" ").formatted(Formatting.AQUA))
                     .append(Text.translatable("text.nations.coins").formatted(Formatting.DARK_AQUA))
                     .append(Text.literal(" ("+String.format("%03.2f",percent)+"%)").formatted(Formatting.AQUA))
               );
            }
            int completion = cost - prog;
            int hourRate = rate / 24;
            int remRate = rate % 24;
            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            ZonedDateTime startOfNextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
            int tickCount = 0;
            while(completion > 0){
               ZonedDateTime noon = startOfNextHour.withHour(12).withMinute(0).withSecond(0).withNano(0);
               if(noon.isEqual(startOfNextHour)){
                  completion -= remRate;
               }
               completion -= hourRate;
               tickCount++;
               startOfNextHour = startOfNextHour.plusHours(1);
            }
            queueTicks += tickCount;
            elem.addLoreLine(Text.translatable("text.nations.estimated_completion",
                  Text.literal(String.format("%,d",queueTicks)+" ").formatted(Formatting.AQUA)
                        .append(Text.translatable("text.nations.hours").formatted(Formatting.DARK_AQUA))
            ).formatted(Formatting.BLUE));
            
            elem.addLoreLine(Text.empty());
            
            if(i > 0){
               ResearchTech prevTech = techQueue.get(i-1);
               if(!tech.hasPrereq(prevTech)){ // Can move up
                  elem.addLoreLine(Text.literal("").append(Text.translatable("gui.nations.click").formatted(Formatting.AQUA)).append(Text.translatable("gui.nations.research_queue_up").formatted(Formatting.DARK_PURPLE)));
               }
            }
            if(i != techQueue.size()-1){
               ResearchTech nextTech = techQueue.get(i+1);
               if(!nextTech.hasPrereq(tech)){ // Can move down
                  elem.addLoreLine(Text.literal("").append(Text.translatable("gui.nations.right_click").formatted(Formatting.GREEN)).append(Text.translatable("gui.nations.research_queue_down").formatted(Formatting.DARK_PURPLE)));
               }
            }
            elem.addLoreLine(Text.literal("").append(Text.translatable("gui.nations.shift_click").formatted(Formatting.RED)).append(Text.translatable("gui.nations.research_dequeue").formatted(Formatting.YELLOW)));
            elem.setCallback(clickType -> modifyTechQueue(tech, clickType));
            
            setSlot(index,elem);
         }else{
            setSlot(46+i,blank);
         }
      }
      
      this.pageNum = GuiHelper.setupPageGui(this,pageNum,9,5,techs,builder,filter,sort,blank,this::queueTech);
   }
   
   private void buildCapturePoints(){
      GuiHelper.outlineGUI(9,6,this,0x758b78,Text.empty(),null);
      List<CapturePoint> caps = Nations.getCapturePoints().stream().filter(cap -> cap.getControllingNation() != null && cap.getControllingNation().equals(nation)).toList();
      this.itemList = caps;
      Function<CapturePoint, GuiElementBuilder> builder = cap -> {
         GuiElementBuilder elem = GuiElementBuilder.from(GraphicalItem.with(cap.getType().getGraphicItem())).hideDefaultTooltip();
         elem.setName(Text.translatable("text.nations.capture_point_header",Text.translatable(cap.getType().getTranslation())).formatted(cap.getType().getTextColor(), Formatting.BOLD));
         elem.addLoreLine(Text.translatable("text.nations.capture_point_yield",Text.literal(cap.getYield()+"").formatted(cap.getType().getTextColor())).formatted(Formatting.DARK_AQUA));
         elem.addLoreLine(Text.translatable("text.nations.stored_coins",Text.literal(cap.getStoredCoins()+"").formatted(cap.getType().getTextColor())).formatted(Formatting.YELLOW));
         elem.addLoreLine(Text.empty());
         elem.addLoreLine(Text.translatable("text.nations.location",
               Text.literal(cap.getBeaconPos().toShortString()).formatted(Formatting.DARK_AQUA),
               Text.literal(cap.getChunkPos().toString()).formatted(Formatting.AQUA)
         ).formatted(Formatting.BLUE));
         int distance = (int)Math.sqrt(nation.getFoundingPos().getSquaredDistance(cap.getBeaconPos()));
         elem.addLoreLine(Text.translatable("text.nations.distance_to",Text.literal(""+distance).formatted(Formatting.GREEN)).formatted(Formatting.DARK_GREEN));
         NationChunk capChunk = Nations.getChunk(cap.getChunkPos());
         if(capChunk != null && capChunk.getControllingNation() == null){
            elem.addLoreLine(Text.empty());
            elem.addLoreLine(Text.literal("").append(Text.translatable("gui.nations.click").formatted(Formatting.RED)).append(Text.translatable("gui.nations.transfer_cap").formatted(Formatting.DARK_RED)));
         }
         return elem;
      };
      GuiElementBuilder blank = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.PAGE_BG, NationsColors.PAGE_COLOR)).hideTooltip();
      
      this.pageNum = GuiHelper.setupPageGui(this,pageNum,9,6,caps,builder,filter,sort,blank,this::transferCapCallback);
   }
   
   @Override
   public void onInventoryChanged(Inventory sender){
      if(!updating){
         updating = true;
         ItemStack stack = inventory.getStack(0);
         if(!stack.isEmpty()){
            int count = stack.getCount();
            if(stack.getItem() instanceof ResourceBullionItem){
               nation.storeResearchCoins(count*1000);
            }else{
               nation.storeResearchCoins(count);
            }
            inventory.setStack(0,ItemStack.EMPTY);
            build();
         }
      }
      updating = false;
   }
   
   @Override
   public void onClose(){
      MiscUtils.returnItems(inventory,player);
   }
   
   private static ScreenHandlerType<?> getTypeFromMode(Mode mode){
      return switch(mode){
         case MENU -> ScreenHandlerType.GENERIC_3X3;
         case MEMBERS, RESEARCH, CAPTURE_POINTS -> ScreenHandlerType.GENERIC_9X6;
      };
   }
   
   public enum Mode{
      MENU,
      MEMBERS,
      RESEARCH,
      CAPTURE_POINTS
   }
   
   private static class CapturePointFilter extends GuiFilter<CapturePoint>{
      public static final List<CapturePointFilter> FILTERS = new ArrayList<>();
      
      public static final CapturePointFilter NONE = new CapturePointFilter("text.nations.none", Formatting.WHITE, cap -> true);
      public static final CapturePointFilter GROWTH = new CapturePointFilter("text.nations.growth", Formatting.GREEN, cap -> cap.getType() == ResourceType.GROWTH);
      public static final CapturePointFilter MATERIAL = new CapturePointFilter("text.nations.material", Formatting.GOLD, cap -> cap.getType() == ResourceType.MATERIAL);
      public static final CapturePointFilter RESEARCH = new CapturePointFilter("text.nations.research", Formatting.AQUA, cap -> cap.getType() == ResourceType.RESEARCH);
      
      private CapturePointFilter(String key, Formatting color, Predicate<CapturePoint> predicate){
         super(key, color, predicate);
         FILTERS.add(this);
      }
      
      @Override
      protected List<CapturePointFilter> getList(){
         return FILTERS;
      }
      
      public static CapturePointFilter getStaticDefault(){
         return NONE;
      }
   }
   
   private static class ResearchFilter extends GuiFilter<ResearchTech>{
      public static Nation nation;
      public static final List<ResearchFilter> FILTERS = new ArrayList<>();
      
      public static final ResearchFilter NONE = new ResearchFilter("text.nations.none", Formatting.WHITE, tech -> true);
      public static final ResearchFilter MAIN_TREE = new ResearchFilter("text.nations.main_tree", Formatting.BLUE, ResearchTech::isMainTree);
      public static final ResearchFilter ARCANA = new ResearchFilter("text.nations.arcana_item", Formatting.LIGHT_PURPLE, ResearchTech::isArcanaItem);
      public static final ResearchFilter ENCHANT = new ResearchFilter("text.nations.enchant", Formatting.DARK_AQUA, ResearchTech::isEnchant);
      public static final ResearchFilter POTION = new ResearchFilter("text.nations.potion", Formatting.GOLD, ResearchTech::isPotion);
      public static final ResearchFilter BUFF = new ResearchFilter("text.nations.buff", Formatting.GREEN, ResearchTech::isBuff);
      public static final ResearchFilter STARTED = new ResearchFilter("text.nations.started", Formatting.AQUA, tech -> getNation().hasStartedTech(tech));
      
      private ResearchFilter(String key, Formatting color, Predicate<ResearchTech> predicate){
         super(key, color, predicate);
         FILTERS.add(this);
      }
      
      @Override
      protected List<ResearchFilter> getList(){
         return FILTERS;
      }
      
      public static ResearchFilter getStaticDefault(){
         return NONE;
      }
      
      private static Nation getNation(){
         return nation;
      }
      
      public static void setNation(Nation nation){
         ResearchFilter.nation = nation;
      }
   }
   
   private static class PlayerFilter extends GuiFilter<UUID>{
      public static final List<PlayerFilter> FILTERS = new ArrayList<>();
      
      public static final PlayerFilter NONE = new PlayerFilter("text.nations.none", Formatting.WHITE, id -> true);
      public static final PlayerFilter MEMBER = new PlayerFilter("text.nations.member", Formatting.YELLOW, id -> {
         INationsProfileComponent data = Nations.getPlayerOrOffline(id);
         if(data == null || data.getNation() == null) return false;
         return data.getNation().hasPlayer(id) && !data.getNation().hasPermissions(id);
      });
      public static final PlayerFilter EXECUTOR = new PlayerFilter("text.nations.executor", Formatting.GREEN, id -> {
         INationsProfileComponent data = Nations.getPlayerOrOffline(id);
         if(data == null || data.getNation() == null) return false;
         return data.getNation().isExecutor(id);
      });
      public static final PlayerFilter LEADER = new PlayerFilter("text.nations.leader", Formatting.LIGHT_PURPLE, id -> {
         INationsProfileComponent data = Nations.getPlayerOrOffline(id);
         if(data == null || data.getNation() == null) return false;
         return data.getNation().isLeader(id);
      });
      public static final PlayerFilter ONLINE = new PlayerFilter("text.nations.online", Formatting.AQUA, id -> Nations.SERVER.getPlayerManager().getPlayer(id) != null);
      public static final PlayerFilter NOT_ONLINE = new PlayerFilter("text.nations.not_online", Formatting.BLUE, id -> Nations.SERVER.getPlayerManager().getPlayer(id) == null);
      
      private PlayerFilter(String key, Formatting color, Predicate<UUID> predicate){
         super(key, color, predicate);
         FILTERS.add(this);
      }
      
      @Override
      protected List<PlayerFilter> getList(){
         return FILTERS;
      }
      
      public static PlayerFilter getStaticDefault(){
         return NONE;
      }
   }
   
   private static class CapturePointSort extends GuiSort<CapturePoint>{
      public static Nation nation;
      public static final List<CapturePointSort> SORTS = new ArrayList<>();
      
      public static final CapturePointSort DISTANCE = new CapturePointSort("text.nations.distance", Formatting.AQUA,
            Comparator.comparing(cap -> getNation().getFoundingPos().getSquaredDistance(cap.getBeaconPos())));
      public static final CapturePointSort STORED_COINS = new CapturePointSort("text.nations.stored_coins_raw", Formatting.GREEN,
            Comparator.comparing(cap -> -cap.getStoredCoins()));
      public static final CapturePointSort YIELD = new CapturePointSort("text.nations.yield", Formatting.YELLOW,
            Comparator.comparing(cap -> -cap.getYield()));
      
      private CapturePointSort(String key, Formatting color, Comparator<CapturePoint> comparator){
         super(key, color, comparator);
         SORTS.add(this);
      }
      
      @Override
      protected List<CapturePointSort> getList(){
         return SORTS;
      }
      
      public static CapturePointSort getStaticDefault(){
         return DISTANCE;
      }
      
      private static Nation getNation(){
         return nation;
      }
      
      public static void setNation(Nation nation){
         CapturePointSort.nation = nation;
      }
   }
   
   private static class ResearchSort extends GuiSort<ResearchTech>{
      public static final List<ResearchSort> SORTS = new ArrayList<>();
      public static final ResearchSort RECOMMENDED = new ResearchSort("text.nations.recommended", Formatting.LIGHT_PURPLE,
            ResearchTech.COMPARATOR);
      public static final ResearchSort TIER = new ResearchSort("text.nations.tier", Formatting.GOLD,
            Comparator.comparingInt(ResearchTech::getTier));
      public static final ResearchSort COST = new ResearchSort("text.nations.cost_raw", Formatting.AQUA,
            Comparator.comparingInt(ResearchTech::getCost));
      public static final ResearchSort ALPHABETICAL = new ResearchSort("text.nations.alphabetical", Formatting.GREEN,
            Comparator.comparing(tech -> tech.getName().getString()));
      
      private ResearchSort(String key, Formatting color, Comparator<ResearchTech> comparator){
         super(key, color, comparator);
         SORTS.add(this);
      }
      
      @Override
      protected List<ResearchSort> getList(){
         return SORTS;
      }
      
      public static ResearchSort getStaticDefault(){
         return TIER;
      }
   }
   
   private static class PlayerSort extends GuiSort<UUID>{
      public static final List<PlayerSort> SORTS = new ArrayList<>();
      public static final PlayerSort LAST_ONLINE = new PlayerSort("text.nations.last_online", Formatting.LIGHT_PURPLE,
            Comparator.comparingLong(playerID -> {
               INationsProfileComponent data = Nations.getPlayerOrOffline(playerID);
               if(data == null) return System.currentTimeMillis();
               return System.currentTimeMillis() - data.getLastOnline();
            }));
      public static final PlayerSort ALPHABETICAL = new PlayerSort("text.nations.alphabetical", Formatting.GREEN,
            Comparator.comparing(playerID -> {
               GameProfile profile = Nations.SERVER.getUserCache().getByUuid(playerID).orElse(null);
               return profile == null ? "" : profile.getName();
            }));
      
      private PlayerSort(String key, Formatting color, Comparator<UUID> comparator){
         super(key, color, comparator);
         SORTS.add(this);
      }
      
      @Override
      protected List<PlayerSort> getList(){
         return SORTS;
      }
      
      public static PlayerSort getStaticDefault(){
         return LAST_ONLINE;
      }
   }
}
