package net.borisshoes.nations.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class NationsColors {
   public static final int RESEARCH_COIN_COLOR = 0x42bff5;
   public static final int MATERIAL_COIN_COLOR = 0xe6a05e;
   public static final int GROWTH_COIN_COLOR = 0x30ba3c;
   public static final int RESEARCH_CAP_COLOR = 0x0e78a6;
   public static final int MATERIAL_CAP_COLOR = 0xb55800;
   public static final int GROWTH_CAP_COLOR = 0x077611;
   public static final int GOLD_COLOR = 0xf6ee09;
   public static final int DARK_GOLD_COLOR = 0x6c6807;
   public static final int PAGE_COLOR = 0xe6dd8e;
   public static final int DARK_COLOR = 0x474747;
   public static final int BORDER_MAP_COLOR = 0xFFFFFF;
   public static final int SPAWN_MAP_BORDER_COLOR = 0x000000;
   public static final int SPAWN_MAP_FILL_COLOR = 0xFFFFFF;
   
   public static final HashMap<DyeColor, HashMap<Item, Item>> COLORED_ITEMS = new HashMap<>(Map.ofEntries(
         Map.entry(DyeColor.BLACK, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.BLACK_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.BLACK_DYE,
               Items.WHITE_WOOL,              Items.BLACK_WOOL,
               Items.WHITE_CONCRETE,          Items.BLACK_CONCRETE,
               Items.GLASS,                   Items.BLACK_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.BLACK_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.BLACK_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.BLACK_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.BLACK_CARPET,
               Items.WHITE_BED,               Items.BLACK_BED
         ))),
         Map.entry(DyeColor.BLUE, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.BLUE_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.BLUE_DYE,
               Items.WHITE_WOOL,              Items.BLUE_WOOL,
               Items.WHITE_CONCRETE,          Items.BLUE_CONCRETE,
               Items.GLASS,                   Items.BLUE_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.BLUE_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.BLUE_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.BLUE_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.BLUE_CARPET,
               Items.WHITE_BED,               Items.BLUE_BED
         ))),
         Map.entry(DyeColor.BROWN, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.BROWN_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.BROWN_DYE,
               Items.WHITE_WOOL,              Items.BROWN_WOOL,
               Items.WHITE_CONCRETE,          Items.BROWN_CONCRETE,
               Items.GLASS,                   Items.BROWN_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.BROWN_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.BROWN_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.BROWN_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.BROWN_CARPET,
               Items.WHITE_BED,               Items.BROWN_BED
         ))),
         Map.entry(DyeColor.GRAY, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.GRAY_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.GRAY_DYE,
               Items.WHITE_WOOL,              Items.GRAY_WOOL,
               Items.WHITE_CONCRETE,          Items.GRAY_CONCRETE,
               Items.GLASS,                   Items.GRAY_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.GRAY_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.GRAY_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.GRAY_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.GRAY_CARPET,
               Items.WHITE_BED,               Items.GRAY_BED
         ))),
         Map.entry(DyeColor.CYAN, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.CYAN_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.CYAN_DYE,
               Items.WHITE_WOOL,              Items.CYAN_WOOL,
               Items.WHITE_CONCRETE,          Items.CYAN_CONCRETE,
               Items.GLASS,                   Items.CYAN_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.CYAN_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.CYAN_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.CYAN_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.CYAN_CARPET,
               Items.WHITE_BED,               Items.CYAN_BED
         ))),
         Map.entry(DyeColor.GREEN, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.GREEN_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.GREEN_DYE,
               Items.WHITE_WOOL,              Items.GREEN_WOOL,
               Items.WHITE_CONCRETE,          Items.GREEN_CONCRETE,
               Items.GLASS,                   Items.GREEN_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.GREEN_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.GREEN_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.GREEN_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.GREEN_CARPET,
               Items.WHITE_BED,               Items.GREEN_BED
         ))),
         Map.entry(DyeColor.LIGHT_BLUE, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.LIGHT_BLUE_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.LIGHT_BLUE_DYE,
               Items.WHITE_WOOL,              Items.LIGHT_BLUE_WOOL,
               Items.WHITE_CONCRETE,          Items.LIGHT_BLUE_CONCRETE,
               Items.GLASS,                   Items.LIGHT_BLUE_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.LIGHT_BLUE_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.LIGHT_BLUE_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.LIGHT_BLUE_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.LIGHT_BLUE_CARPET,
               Items.WHITE_BED,               Items.LIGHT_BLUE_BED
         ))),
         Map.entry(DyeColor.LIGHT_GRAY, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.LIGHT_GRAY_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.LIGHT_GRAY_DYE,
               Items.WHITE_WOOL,              Items.LIGHT_GRAY_WOOL,
               Items.WHITE_CONCRETE,          Items.LIGHT_GRAY_CONCRETE,
               Items.GLASS,                   Items.LIGHT_GRAY_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.LIGHT_GRAY_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.LIGHT_GRAY_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.LIGHT_GRAY_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.LIGHT_GRAY_CARPET,
               Items.WHITE_BED,               Items.LIGHT_GRAY_BED
         ))),
         Map.entry(DyeColor.LIME, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.LIME_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.LIME_DYE,
               Items.WHITE_WOOL,              Items.LIME_WOOL,
               Items.WHITE_CONCRETE,          Items.LIME_CONCRETE,
               Items.GLASS,                   Items.LIME_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.LIME_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.LIME_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.LIME_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.LIME_CARPET,
               Items.WHITE_BED,               Items.LIME_BED
         ))),
         Map.entry(DyeColor.MAGENTA, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.MAGENTA_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.MAGENTA_DYE,
               Items.WHITE_WOOL,              Items.MAGENTA_WOOL,
               Items.WHITE_CONCRETE,          Items.MAGENTA_CONCRETE,
               Items.GLASS,                   Items.MAGENTA_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.MAGENTA_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.MAGENTA_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.MAGENTA_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.MAGENTA_CARPET,
               Items.WHITE_BED,               Items.MAGENTA_BED
         ))),
         Map.entry(DyeColor.ORANGE, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.ORANGE_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.ORANGE_DYE,
               Items.WHITE_WOOL,              Items.ORANGE_WOOL,
               Items.WHITE_CONCRETE,          Items.ORANGE_CONCRETE,
               Items.GLASS,                   Items.ORANGE_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.ORANGE_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.ORANGE_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.ORANGE_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.ORANGE_CARPET,
               Items.WHITE_BED,               Items.ORANGE_BED
         ))),
         Map.entry(DyeColor.PINK, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.PINK_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.PINK_DYE,
               Items.WHITE_WOOL,              Items.PINK_WOOL,
               Items.WHITE_CONCRETE,          Items.PINK_CONCRETE,
               Items.GLASS,                   Items.PINK_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.PINK_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.PINK_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.PINK_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.PINK_CARPET,
               Items.WHITE_BED,               Items.PINK_BED
         ))),
         Map.entry(DyeColor.PURPLE, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.PURPLE_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.PURPLE_DYE,
               Items.WHITE_WOOL,              Items.PURPLE_WOOL,
               Items.WHITE_CONCRETE,          Items.PURPLE_CONCRETE,
               Items.GLASS,                   Items.PURPLE_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.PURPLE_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.PURPLE_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.PURPLE_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.PURPLE_CARPET,
               Items.WHITE_BED,               Items.PURPLE_BED
         ))),
         Map.entry(DyeColor.RED, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.RED_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.RED_DYE,
               Items.WHITE_WOOL,              Items.RED_WOOL,
               Items.WHITE_CONCRETE,          Items.RED_CONCRETE,
               Items.GLASS,                   Items.RED_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.RED_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.RED_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.RED_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.RED_CARPET,
               Items.WHITE_BED,               Items.RED_BED
         ))),
         Map.entry(DyeColor.WHITE, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.WHITE_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.WHITE_DYE,
               Items.WHITE_WOOL,              Items.WHITE_WOOL,
               Items.WHITE_CONCRETE,          Items.WHITE_CONCRETE,
               Items.GLASS,                   Items.WHITE_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.WHITE_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.WHITE_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.WHITE_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.WHITE_CARPET,
               Items.WHITE_BED,               Items.WHITE_BED
         ))),
         Map.entry(DyeColor.YELLOW, new HashMap<>(Map.of(
               Items.GLASS_PANE,              Items.YELLOW_STAINED_GLASS_PANE,
               Items.WHITE_DYE,               Items.YELLOW_DYE,
               Items.WHITE_WOOL,              Items.YELLOW_WOOL,
               Items.WHITE_CONCRETE,          Items.YELLOW_CONCRETE,
               Items.GLASS,                   Items.YELLOW_STAINED_GLASS,
               Items.WHITE_CONCRETE_POWDER,   Items.YELLOW_CONCRETE_POWDER,
               Items.TERRACOTTA,              Items.YELLOW_TERRACOTTA,
               Items.WHITE_GLAZED_TERRACOTTA, Items.YELLOW_GLAZED_TERRACOTTA,
               Items.WHITE_CARPET,            Items.YELLOW_CARPET,
               Items.WHITE_BED,               Items.YELLOW_BED
         )))
   ));
   
   public static MutableText withColor(MutableText text, int color){
      return text.setStyle(text.getStyle().withColor(color));
   }
   
   public static BlockState redyeBlock(BlockState state, DyeColor color){
      Block block = state.getBlock();
      Item item = block.asItem();
      HashMap<Item,Item> whitePalette = NationsColors.COLORED_ITEMS.get(DyeColor.WHITE);
      Item key = null;
      if(whitePalette.containsKey(item)){
         key = item;
      }else{
         search: {
            for(HashMap<Item, Item> map : COLORED_ITEMS.values()){
               if(map.containsValue(item)){
                  for(Map.Entry<Item, Item> entry : map.entrySet()){
                     if(entry.getValue().equals(item)){
                        key = entry.getKey();
                        break search;
                     }
                  }
               }
            }
         }
         if(key == null) return state;
      }
      
      HashMap<Item,Item> newPalette = NationsColors.COLORED_ITEMS.get(color);
      Item newItem = newPalette.get(key);
      if(newItem instanceof BlockItem blockItem){
         Block newBlock = blockItem.getBlock();
         return newBlock.getStateWithProperties(state);
      }
      
      return state;
   }
   
   private static final ArrayList<Pair<Formatting,Integer>> COLOR_MAP = new ArrayList<>(Arrays.asList(
         new Pair<>(Formatting.BLACK,0x000000),
         new Pair<>(Formatting.DARK_BLUE,0x0000AA),
         new Pair<>(Formatting.DARK_GREEN,0x00AA00),
         new Pair<>(Formatting.DARK_AQUA,0x00AAAA),
         new Pair<>(Formatting.DARK_RED,0xAA0000),
         new Pair<>(Formatting.DARK_PURPLE,0xAA00AA),
         new Pair<>(Formatting.GOLD,0xFFAA00),
         new Pair<>(Formatting.GRAY,0xAAAAAA),
         new Pair<>(Formatting.DARK_GRAY,0x555555),
         new Pair<>(Formatting.BLUE,0x5555FF),
         new Pair<>(Formatting.GREEN,0x55FF55),
         new Pair<>(Formatting.AQUA,0x55FFFF),
         new Pair<>(Formatting.RED,0xFF5555),
         new Pair<>(Formatting.LIGHT_PURPLE,0xFF55FF),
         new Pair<>(Formatting.YELLOW,0xFFFF55),
         new Pair<>(Formatting.WHITE,0xFFFFFF)
   ));
   
   public static Formatting getClosestFormatting(int colorRGB){
      Formatting closest = Formatting.WHITE;
      double cDist = Integer.MAX_VALUE;
      for(Pair<Formatting, Integer> pair : COLOR_MAP){
         int repColor = pair.getRight();
         double rDist = (((repColor>>16)&0xFF)-((colorRGB>>16)&0xFF))*0.30;
         double gDist = (((repColor>>8)&0xFF)-((colorRGB>>8)&0xFF))*0.59;
         double bDist = ((repColor&0xFF)-(colorRGB&0xFF))*0.11;
         double dist = rDist*rDist + gDist*gDist + bDist*bDist;
         if(dist < cDist){
            cDist = dist;
            closest = pair.getLeft();
         }
      }
      return closest;
   }
}
