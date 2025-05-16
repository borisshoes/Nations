package net.borisshoes.nations.gameplay;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.items.GraphicalItem;
import net.borisshoes.nations.items.ResourceCoinItem;
import net.minecraft.item.Item;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

import static net.borisshoes.nations.Nations.MOD_ID;

public enum ResourceType implements StringIdentifiable{
   GROWTH(0,"growth",Formatting.GREEN, (ResourceCoinItem) NationsRegistry.GROWTH_COIN_ITEM, GraphicalItem.GraphicItems.GROWTH_CAPTURE_POINT),
   MATERIAL(1,"material",Formatting.GOLD, (ResourceCoinItem) NationsRegistry.MATERIAL_COIN_ITEM, GraphicalItem.GraphicItems.MATERIAL_CAPTURE_POINT),
   RESEARCH(2,"research",Formatting.AQUA, (ResourceCoinItem) NationsRegistry.RESEARCH_COIN_ITEM, GraphicalItem.GraphicItems.RESEARCH_CAPTURE_POINT);
   
   private static final IntFunction<ResourceType> BY_ID;
   public static final StringIdentifiable.EnumCodec<ResourceType> CODEC;
   
   private final int id;
   private final String name;
   private final Formatting textColor;
   private final ResourceCoinItem coin;
   private final GraphicalItem.GraphicItems graphicItem;
   
   ResourceType(int id, String name, Formatting textColor, ResourceCoinItem coin, GraphicalItem.GraphicItems graphicItem){
      this.id = id;
      this.name = name;
      this.textColor = textColor;
      this.coin = coin;
      this.graphicItem = graphicItem;
   }
   
   public GraphicalItem.GraphicItems getGraphicItem(){
      return graphicItem;
   }
   
   public int getId(){
      return id;
   }
   
   public String getTranslation(){
      return "text.nations."+name;
   }
   
   public MutableText getText(){
      return Text.translatable(getTranslation()).formatted(textColor);
   }
   
   public Formatting getTextColor(){
      return textColor;
   }
   
   public ResourceCoinItem getCoin(){
      return coin;
   }
   
   @Nullable
   @Contract(value="_,!null->!null;_,null->_")
   public static ResourceType byName(String name, @Nullable ResourceType defaultType) {
      ResourceType type = CODEC.byId(name);
      return type != null ? type : defaultType;
   }
   
   static {
      BY_ID = ValueLists.createIdToValueFunction(ResourceType::getId, ResourceType.values(), ValueLists.OutOfBoundsHandling.ZERO);
      CODEC = StringIdentifiable.createCodec(ResourceType::values);
   }
   
   @Override
   public String asString(){
      return name;
   }
   
   @Override
   public String toString(){
      return name;
   }
}
