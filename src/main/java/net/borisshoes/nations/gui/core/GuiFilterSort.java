package net.borisshoes.nations.gui.core;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public abstract class GuiFilterSort<A>{
   private final String key;
   private final Formatting color;
   
   protected GuiFilterSort(String key, Formatting color){
      this.key = key;
      this.color = color;
   }
   
   public Formatting getFormatting(){
      return color;
   }
   
   public Text getColoredLabel(){
      return Text.translatable(key).formatted(color);
   }
   
   protected abstract List<? extends GuiFilterSort<A>> getList();
   
   protected GuiFilterSort<A> getDefault(){
      return getList().getFirst();
   }
   
   protected <B extends GuiFilterSort<A>> B cycle(B sort, boolean backwards){
      List<? extends GuiFilterSort<A>> list = getList();
      int idx = list.indexOf(sort);
      if (idx == -1) {
         return (B) list.getFirst();
      }
      int shift = backwards ? -1 : 1;
      int nextIndex = (idx + shift + list.size()) % list.size();
      return (B) list.get(nextIndex);
   }
}