package net.borisshoes.nations.gameplay;

import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public enum ChatChannel implements StringIdentifiable {
   NATION("nation",Formatting.GOLD),
   LOCAL("local",Formatting.LIGHT_PURPLE),
   GLOBAL("global",Formatting.AQUA);
   
   public static final EnumCodec<ChatChannel> CODEC;
   
   private final String name;
   private final Formatting color;
   
   ChatChannel(String name, Formatting formatting){
      this.name = name;
      this.color = formatting;
   }
   
   @Override
   public String asString(){
      return name;
   }
   
   public Formatting getColor(){
      return color;
   }
   
   public String getTranslationKey(){
      return switch(this){
         case NATION -> "text.nations.nation";
         case LOCAL -> "text.nations.local";
         case GLOBAL -> "text.nations.global";
      };
   }
   
   @Nullable
   @Contract(value="_,!null->!null;_,null->_")
   public static ChatChannel byName(String name, @Nullable ChatChannel defaultType) {
      ChatChannel type = CODEC.byId(name);
      return type != null ? type : defaultType;
   }
   
   static {
      CODEC = StringIdentifiable.createCodec(ChatChannel::values);
   }
}
