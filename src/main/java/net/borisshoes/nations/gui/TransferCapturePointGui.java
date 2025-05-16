package net.borisshoes.nations.gui;

import com.mojang.authlib.GameProfile;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.borisshoes.nations.gameplay.CapturePoint;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.NationChunk;
import net.borisshoes.nations.gui.core.GuiFilter;
import net.borisshoes.nations.gui.core.GuiHelper;
import net.borisshoes.nations.gui.core.GuiSort;
import net.borisshoes.nations.items.GraphicalItem;
import net.borisshoes.nations.utils.MiscUtils;
import net.borisshoes.nations.utils.NationsColors;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public class TransferCapturePointGui extends SimpleGui {
   private final Nation owningNation;
   private final CapturePoint capturePoint;
   
   private NationSort sort;
   private NationFilter filter;
   private int pageNum;
   private List<Nation> itemList;
   
   public TransferCapturePointGui(ServerPlayerEntity player, Nation owningNation, CapturePoint capturePoint){
      super(ScreenHandlerType.GENERIC_9X6,player,false);
      this.owningNation = owningNation;
      this.capturePoint = capturePoint;
      this.sort = NationSort.getStaticDefault();
      this.filter = NationFilter.getStaticDefault();
      setTitle(Text.translatable("gui.nations.transfer_cap_gui_title_short"));
      build();
   }
   
   @Override
   public boolean onAnyClick(int index, ClickType type, SlotActionType action){
      int sortInd = 0;
      int filterInd = 8;
      int prevInd = 45;
      int nextInd = 53;
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
         int numPages = (int) (Math.ceil((float)itemList.size()/28.0));
         if(pageNum < numPages){
            pageNum += 1;
            build();
         }
      }
      return true;
   }
   
   private void transferCallback(Nation nation, ClickType clickType){
      NationChunk chunk = Nations.getChunk(capturePoint.getChunkPos());
      if(chunk != null && chunk.getControllingNation() != null){
         close();
         return;
      }
      capturePoint.transferOwnership(player.getServer().getOverworld(),nation);
      MutableText announcement = Text.translatable("text.nations.cap_transfer",
            capturePoint.getType().getText().formatted(Formatting.BOLD),
            Text.translatable("text.nations.capture_point").formatted(Formatting.BOLD,capturePoint.getType().getTextColor()),
            Text.literal(capturePoint.getChunkPos().toString()).formatted(Formatting.YELLOW,Formatting.BOLD),
            nation.getFormattedName().formatted(Formatting.BOLD)
      ).formatted(Formatting.DARK_AQUA);
      Nations.announce(announcement);
      close();
   }
   
   private void build(){
      GuiHelper.outlineGUI(9,6,this,owningNation.getTextColorSub(),Text.empty(),null);
      List<Nation> nations = Nations.getNations().stream().filter(nation -> !nation.equals(owningNation)).toList();
      this.itemList = nations;
      
      Function<Nation, GuiElementBuilder> builder = nation -> {
         GuiElementBuilder elem = new GuiElementBuilder(NationsRegistry.GROWTH_COIN_ITEM).hideDefaultTooltip();
         elem.setComponent(DataComponentTypes.DYED_COLOR,new DyedColorComponent(nation.getTextColor(),false));
         elem.setName(nation.getFormattedName().formatted(Formatting.BOLD));
         elem.addLoreLine(Text.literal(nation.getMembers().size()+" ").withColor(nation.getTextColor()).append(Text.translatable("text.nations.members").withColor(nation.getTextColorSub())));
         elem.addLoreLine(Text.empty());
         elem.addLoreLine(Text.literal("").append(Text.translatable("gui.nations.click").formatted(Formatting.RED)).append(Text.translatable("gui.nations.transfer_cap").formatted(Formatting.DARK_RED)));
         return elem;
      };
      GuiElementBuilder blank = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.PAGE_BG, NationsColors.PAGE_COLOR)).hideTooltip();
      
      GuiElementBuilder elem = GuiElementBuilder.from(GraphicalItem.with(capturePoint.getType().getGraphicItem())).hideDefaultTooltip();
      elem.setName(Text.translatable("text.nations.capture_point_header",Text.translatable(capturePoint.getType().getTranslation())).formatted(capturePoint.getType().getTextColor(), Formatting.BOLD));
      elem.addLoreLine(Text.translatable("text.nations.location",
            Text.literal(capturePoint.getBeaconPos().toShortString()).formatted(Formatting.DARK_AQUA),
            Text.literal(capturePoint.getChunkPos().toString()).formatted(Formatting.AQUA)
      ).formatted(Formatting.BLUE));
      elem.addLoreLine(Text.translatable("text.nations.capture_point_yield",Text.literal(capturePoint.getYield()+"").formatted(capturePoint.getType().getTextColor())).formatted(Formatting.DARK_AQUA));
      elem.addLoreLine(Text.translatable("text.nations.stored_coins",Text.literal(capturePoint.getStoredCoins()+"").formatted(capturePoint.getType().getTextColor())).formatted(Formatting.YELLOW));
      setSlot(4,elem);
      
      this.pageNum = GuiHelper.setupPageGui(this,pageNum,9,6,nations,builder,filter,sort,blank,this::transferCallback);
   }
   
   private static class NationFilter extends GuiFilter<Nation> {
      public static final List<NationFilter> FILTERS = new ArrayList<>();
      
      public static final NationFilter NONE = new NationFilter("text.nations.none", Formatting.WHITE, nation -> true);
      
      private NationFilter(String key, Formatting color, Predicate<Nation> predicate){
         super(key, color, predicate);
         FILTERS.add(this);
      }
      
      @Override
      protected List<NationFilter> getList(){
         return FILTERS;
      }
      
      public static NationFilter getStaticDefault(){
         return NONE;
      }
   }
   
   private static class NationSort extends GuiSort<Nation> {
      public static final List<NationSort> SORTS = new ArrayList<>();
      
      public static final NationSort ALPHABETICAL = new NationSort("text.nations.alphabetical", Formatting.AQUA,
            Comparator.comparing(Nation::getName));
      public static final NationSort PLAYER_COUNT = new NationSort("text.nations.player_count", Formatting.LIGHT_PURPLE,
            Comparator.comparingInt(nation -> -nation.getMembers().size()));
      
      private NationSort(String key, Formatting color, Comparator<Nation> comparator){
         super(key, color, comparator);
         SORTS.add(this);
      }
      
      @Override
      protected List<NationSort> getList(){
         return SORTS;
      }
      
      public static NationSort getStaticDefault(){
         return ALPHABETICAL;
      }
   }
}
