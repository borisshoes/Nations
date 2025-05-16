package net.borisshoes.nations.gui.core;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.nations.items.GraphicalItem;
import net.borisshoes.nations.utils.MiscUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class GuiHelper {
   
   public static <T> int setupPageGui(SimpleGui gui, int pageNum, int width, int height, List<T> objects, Function<T, GuiElementBuilder> elemBuilder, GuiFilter<T> filter, GuiSort<T> sort, GuiElementBuilder blank){
      return setupPageGui(gui,pageNum,width,height,objects,elemBuilder,filter,sort,blank,(a,b) -> {});
   }
   
   public static <T> int setupPageGui(SimpleGui gui, int pageNum, int width, int height, List<T> objects, Function<T, GuiElementBuilder> elemBuilder, GuiFilter<T> filter, GuiSort<T> sort, GuiElementBuilder blank, BiConsumer<T,ClickType> callback){
      if(height < 3 || width < 3) return pageNum;
      int guiSize = width*height;
      int itemsPerPage = (width-2) * (height-2);
      List<T> filteredSorted = objects.stream().filter(filter.getPredicate()).sorted(sort.getComparator()).toList();
      int numPages = (int) Math.ceil((float) filteredSorted.size() / itemsPerPage);
      pageNum = Math.clamp(pageNum,1,Math.max(1,numPages));
      List<T> pageItems = MiscUtils.listToPage(filteredSorted,pageNum,itemsPerPage);
      
      GuiElementBuilder nextPage = GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.GraphicItems.RIGHT_ARROW));
      nextPage.setName(Text.translatable("gui.nations.next_page_title",pageNum,numPages).formatted(Formatting.DARK_PURPLE));
      nextPage.addLoreLine((Text.literal("")
            .append(Text.translatable("gui.nations.click").formatted(Formatting.AQUA))
            .append(Text.translatable("gui.nations.next_page_sub").formatted(Formatting.LIGHT_PURPLE))));
      gui.setSlot(guiSize-1, nextPage);
      
      GuiElementBuilder prevPage = GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.GraphicItems.LEFT_ARROW));
      prevPage.setName(Text.translatable("gui.nations.prev_page_title",pageNum,numPages).formatted(Formatting.DARK_PURPLE));
      prevPage.addLoreLine((Text.literal("")
            .append(Text.translatable("gui.nations.click").formatted(Formatting.AQUA))
            .append(Text.translatable("gui.nations.prev_page_sub").formatted(Formatting.LIGHT_PURPLE))));
      gui.setSlot(guiSize-width, prevPage);
      
      GuiElementBuilder filterBuilt = GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.GraphicItems.FILTER)).hideDefaultTooltip();
      filterBuilt.setName(Text.translatable("text.nations.filter").formatted(Formatting.DARK_PURPLE));
      filterBuilt.addLoreLine((Text.literal("").append(Text.translatable("gui.nations.click").formatted(Formatting.AQUA)).append(Text.translatable("gui.nations.change_filter").formatted(Formatting.LIGHT_PURPLE))));
      filterBuilt.addLoreLine((Text.literal("").append(Text.translatable("gui.nations.right_click").formatted(Formatting.GREEN)).append(Text.translatable("gui.nations.change_filter_back").formatted(Formatting.LIGHT_PURPLE))));
      filterBuilt.addLoreLine((Text.literal("").append(Text.translatable("gui.nations.shift_click").formatted(Formatting.YELLOW)).append(Text.translatable("gui.nations.reset_filter").formatted(Formatting.LIGHT_PURPLE))));
      filterBuilt.addLoreLine((Text.literal("")));
      filterBuilt.addLoreLine((Text.literal("").append(Text.translatable("text.nations.filtering_by").formatted(Formatting.AQUA)).append(filter.getColoredLabel())));
      gui.setSlot(width-1,filterBuilt);
      
      GuiElementBuilder sortBuilt = GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.GraphicItems.SORT)).hideDefaultTooltip();
      sortBuilt.setName(Text.translatable("text.nations.sort").formatted(Formatting.DARK_PURPLE));
      sortBuilt.addLoreLine((Text.literal("").append(Text.translatable("gui.nations.click").formatted(Formatting.AQUA)).append(Text.translatable("gui.nations.change_sort").formatted(Formatting.LIGHT_PURPLE))));
      sortBuilt.addLoreLine((Text.literal("").append(Text.translatable("gui.nations.right_click").formatted(Formatting.GREEN)).append(Text.translatable("gui.nations.change_sort_back").formatted(Formatting.LIGHT_PURPLE))));
      sortBuilt.addLoreLine((Text.literal("").append(Text.translatable("gui.nations.shift_click").formatted(Formatting.YELLOW)).append(Text.translatable("gui.nations.reset_sort").formatted(Formatting.LIGHT_PURPLE))));
      sortBuilt.addLoreLine((Text.literal("")));
      sortBuilt.addLoreLine((Text.literal("").append(Text.translatable("text.nations.sorting_by").formatted(Formatting.AQUA)).append(sort.getColoredLabel())));
      gui.setSlot(0,sortBuilt);
      
      int k = 0;
      for(int i = 0; i < height-2; i++){
         for(int j = 0; j < width-2; j++){
            int index = width+1 + i*width + j;
            if(k < pageItems.size()){
               T item = pageItems.get(k);
               GuiElementBuilder builder = elemBuilder.apply(item);
               builder.setCallback(clickType -> callback.accept(item, clickType));
               gui.setSlot(index, builder);
            }else{
               gui.setSlot(index,blank);
            }
            k++;
         }
      }
      return pageNum;
   }
   
   public static void outlineGUI(SimpleGui gui, int color, Text borderText){
      outlineGUI(gui,color,borderText,null);
   }
   
   public static void outlineGUI(SimpleGui gui, int color, Text borderText, List<Text> lore){
      int width = gui.getWidth();
      int height = gui.getHeight();
      outlineGUI(width, height, gui,color,borderText,lore);
   }
   
   public static void outlineGUI(int width, int height, SimpleGui gui, int color, Text borderText, List<Text> lore){
      int size = width*height;
      for(int i = 0; i < size; i++){
         gui.clearSlot(i);
         GuiElementBuilder menuItem;
         boolean top = i/width == 0;
         boolean bottom = i/width == (size/width - 1);
         boolean left = i%width == 0;
         boolean right = i%width == width-1;
         
         if(top){
            if(left){
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP_LEFT,color));
            }else if(right){
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP_RIGHT,color));
            }else{
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP,color));
            }
         }else if(bottom){
            if(left){
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_BOTTOM_LEFT,color));
            }else if(right){
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_BOTTOM_RIGHT,color));
            }else{
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_BOTTOM,color));
            }
         }else if(left){
            menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_LEFT,color));
         }else if(right){
            menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_RIGHT,color));
         }else{
            menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP,color));
         }
         
         if(borderText.getString().isEmpty()){
            menuItem.hideTooltip();
         }else{
            menuItem.setName(borderText).hideDefaultTooltip();
            if(lore != null && !lore.isEmpty()){
               for(Text text : lore){
                  menuItem.addLoreLine(text);
               }
            }
         }
         
         gui.setSlot(i,menuItem);
      }
   }
}
