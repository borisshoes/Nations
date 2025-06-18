package net.borisshoes.nations.gui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gui.core.GuiFilter;
import net.borisshoes.nations.gui.core.GuiHelper;
import net.borisshoes.nations.gui.core.GuiSort;
import net.borisshoes.nations.items.GraphicalItem;
import net.borisshoes.nations.items.ResourceBullionItem;
import net.borisshoes.nations.items.ResourceCoinItem;
import net.borisshoes.nations.utils.MiscUtils;
import net.borisshoes.nations.utils.NationsColors;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class ShopGui extends SimpleGui {
   
   private ShopSort sort;
   private ShopFilter filter;
   private int pageNum;
   private List<Pair<ItemStack,Pair<Item,Integer>>> itemList;
   private int barterLvl;
   
   public ShopGui(ServerPlayerEntity player){
      super(ScreenHandlerType.GENERIC_9X6,player,false);
      this.sort = ShopSort.getStaticDefault();
      this.filter = ShopFilter.getStaticDefault();
      if(Nations.getNation(player) != null){
         this.barterLvl = Nations.getNation(player).getBuffLevel(NationsRegistry.BARTERING);
         ShopSort.setBarterLevel(barterLvl);
      }
      setTitle(Text.translatable("gui.nations.shop_title"));
      build();
   }
   
   @Override
   public boolean onAnyClick(int index, ClickType type, SlotActionType action){
      ShopSort.setBarterLevel(barterLvl);
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
   
   private void purchaseCallback(Pair<ItemStack,Pair<Item,Integer>> offer, ClickType clickType){
      if(!clickType.isLeft) return;
      int cost = getDiscountedCost(offer,barterLvl);
      if(MiscUtils.removeItems(player,offer.getRight().getLeft(), cost)){
         MiscUtils.returnItems(new SimpleInventory(offer.getLeft().copy()),player);
         player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.MASTER,1,1.2f);
      }else{
         player.sendMessage(Text.translatable("text.nations.not_enough_items",Text.translatable(offer.getRight().getLeft().getTranslationKey()).formatted(Formatting.GOLD,Formatting.ITALIC)).formatted(Formatting.RED,Formatting.ITALIC));
      }
   }
   
   private static int getDiscountedCost(Pair<ItemStack,Pair<Item,Integer>> offer, int barterLvl){
      ItemStack merch = offer.getLeft();
      Item currency = offer.getRight().getLeft();
      int price = offer.getRight().getRight();
      boolean conversion = (currency instanceof ResourceCoinItem && merch.getItem() instanceof ResourceCoinItem) ||
            (currency instanceof ResourceBullionItem && merch.getItem() instanceof ResourceBullionItem);
      if(conversion){
         double conversionIncrease = NationsConfig.getDouble(NationsRegistry.BARTERING_CONVERSION_INCREASE_CFG);
         return Math.max(merch.getCount(),(int)Math.round(price * (1.0-barterLvl*conversionIncrease)));
      }else{
         double barterDecrease = NationsConfig.getDouble(NationsRegistry.BARTERING_DISCOUNT_CFG);
         return Math.max(1,(int)Math.round(price * (1.0-barterLvl*barterDecrease)));
      }
   }
   
   public void build(){
      ShopSort.setBarterLevel(barterLvl);
      GuiHelper.outlineGUI(9,6,this,0x07899c,Text.empty(),null);
      List<Pair<ItemStack,Pair<Item,Integer>>> offers = Nations.SHOP.getOffers();
      this.itemList = offers;
      
      Function<Pair<ItemStack,Pair<Item,Integer>>, GuiElementBuilder> builder = offer -> {
         int cost = getDiscountedCost(offer,barterLvl);
         GuiElementBuilder elem = GuiElementBuilder.from(offer.getLeft());
         elem.setName(offer.getLeft().getName().copy().formatted(Formatting.LIGHT_PURPLE));
         elem.addLoreLine(Text.translatable("text.nations.count").formatted(Formatting.BLUE)
               .append(Text.literal(String.format("%,d",offer.getLeft().getCount())).formatted(Formatting.DARK_GREEN))
         );
         elem.addLoreLine(Text.translatable("text.nations.cost").formatted(Formatting.GREEN)
               .append(Text.literal(String.format("%,d",cost)+" ").formatted(Formatting.AQUA))
               .append(Text.translatable(offer.getRight().getLeft().getTranslationKey()).formatted(Formatting.DARK_AQUA))
         );
         elem.addLoreLine(Text.empty());
         elem.addLoreLine(Text.literal("").append(Text.translatable("gui.nations.click").formatted(Formatting.AQUA)).append(Text.translatable("gui.nations.purchase_item").formatted(Formatting.DARK_GREEN)));
         return elem;
      };
      GuiElementBuilder blank = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.PAGE_BG, NationsColors.PAGE_COLOR)).hideTooltip();
      this.pageNum = GuiHelper.setupPageGui(this,pageNum,9,6,offers,builder,filter,sort,blank,this::purchaseCallback);
   }
   
   
   private static class ShopFilter extends GuiFilter<Pair<ItemStack,Pair<Item,Integer>>> {
      public static final List<ShopFilter> FILTERS = new ArrayList<>();
      
      public static final ShopFilter NONE = new ShopFilter("text.nations.none", Formatting.WHITE, entry -> true);
      public static final ShopFilter MATERIALS = new ShopFilter("text.nations.materials", Formatting.GOLD, entry -> entry.getRight().getLeft() == NationsRegistry.MATERIAL_COIN_ITEM && !(entry.getLeft().getItem() instanceof ResourceCoinItem));
      public static final ShopFilter CONVERSION = new ShopFilter("text.nations.conversion", Formatting.LIGHT_PURPLE, entry ->
            (entry.getRight().getLeft() instanceof ResourceCoinItem && entry.getLeft().getItem() instanceof ResourceCoinItem) ||
                  (entry.getRight().getLeft() instanceof ResourceBullionItem && entry.getLeft().getItem() instanceof ResourceBullionItem)
      );
      public static final ShopFilter SPAWNERS = new ShopFilter("text.nations.spawners", Formatting.DARK_AQUA, entry -> entry.getLeft().isOf(Items.SPAWNER));
      public static final ShopFilter BUYING = new ShopFilter("text.nations.buying", Formatting.GREEN, entry -> !(entry.getRight().getLeft() instanceof ResourceCoinItem) && entry.getLeft().getItem() instanceof ResourceCoinItem);
      
      private ShopFilter(String key, Formatting color, Predicate<Pair<ItemStack, Pair<Item,Integer>>> predicate){
         super(key, color, predicate);
         FILTERS.add(this);
      }
      
      @Override
      protected List<ShopFilter> getList(){
         return FILTERS;
      }
      
      public static ShopFilter getStaticDefault(){
         return NONE;
      }
   }
   
   private static class ShopSort extends GuiSort<Pair<ItemStack,Pair<Item,Integer>>> {
      public static final List<ShopSort> SORTS = new ArrayList<>();
      public static int barterLevel;
      
      public static final ShopSort RECOMMENDED = new ShopSort("text.nations.recommended", Formatting.LIGHT_PURPLE,
            Comparator.comparingInt(entry -> Nations.SHOP.getOffers().indexOf(entry)));
      public static final ShopSort ALPHABETICAL = new ShopSort("text.nations.alphabetical", Formatting.AQUA,
            Comparator.comparing(entry -> entry.getLeft().getName().getString()));
      public static final ShopSort PRICE_ASCENDING = new ShopSort("text.nations.price_asc", Formatting.GREEN,
            Comparator.comparingInt(entry -> getDiscountedCost(entry,getBarterLevel())));
      public static final ShopSort PRICE_DESCENDING = new ShopSort("text.nations.price_desc", Formatting.RED,
            Comparator.comparingInt(entry -> -getDiscountedCost(entry,getBarterLevel())));
      
      private ShopSort(String key, Formatting color, Comparator<Pair<ItemStack,Pair<Item,Integer>>> comparator){
         super(key, color, comparator);
         SORTS.add(this);
      }
      
      @Override
      protected List<ShopSort> getList(){
         return SORTS;
      }
      
      public static ShopSort getStaticDefault(){
         return RECOMMENDED;
      }
      
      private static int getBarterLevel(){
         return barterLevel;
      }
      
      public static void setBarterLevel(int level){
         barterLevel = level;
      }
   }
}
