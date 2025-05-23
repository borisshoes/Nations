package net.borisshoes.nations.gui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.arcananovum.items.normal.GraphicItems;
import net.borisshoes.arcananovum.items.normal.GraphicalItem;
import net.borisshoes.arcananovum.utils.ArcanaColors;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gui.core.GuiHelper;
import net.borisshoes.nations.items.BugVoucherItem;
import net.borisshoes.nations.utils.GenericTimer;
import net.borisshoes.nations.utils.MiscUtils;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;

import java.util.*;

public class BugVoucherGui extends SimpleGui {
   
   private boolean anim = false;
   private int animTicks = 0;
   private int endTicks = 0;
   private static final int numChoices = 3;
   private final Set<Integer> choices = new HashSet<>();
   private final HashMap<ItemStack, Integer> loot;
   private final ArrayList<Integer> nonChoices = new ArrayList<>();
   private final ArrayList<ItemStack> rewards = new ArrayList<>();
   private final ArrayList<ItemStack> display = new ArrayList<>();
   
   public BugVoucherGui(ServerPlayerEntity player){
      super(ScreenHandlerType.GENERIC_9X6,player,false);
      setTitle(Text.translatable("gui.nations.bug_voucher_title"));
      buildChoices();
      loot = BugVoucherItem.loot;
   }
   
   private int animDuration(){
      return 10*(28-numChoices) + 20 * numChoices + 40;
   }
   
   private void buildChoices(){
      GuiHelper.outlineGUI(9,6,this,0x9a2e44,Text.empty(),null);
      
      int k = 0;
      for(int i = 0; i < 4; i++){
         for(int j = 0; j < 7; j++){
            int ind = (i*9+10)+j;
            int color = k % 2 == 0 ? 0x3a3838 : 0xbf2525;
            boolean sel = choices.contains(k);
            GuiElementBuilder elem = sel ? GuiElementBuilder.from(GraphicalItem.withColor(GraphicItems.CASINO_CHIP, ArcanaColors.EQUAYUS_COLOR)) : GuiElementBuilder.from(GraphicalItem.withColor(GraphicItems.PAGE_BG, color));
            elem.setName(Text.translatable("text.nations.slot_num",k).formatted(Formatting.BOLD, (sel ? Formatting.GRAY : Formatting.AQUA)));
            elem.addLoreLine(Text.translatable("gui.nations.choose_slots",numChoices).formatted(Formatting.DARK_PURPLE));
            final int finalInd = k;
            elem.setCallback(clickType -> chooseCallback(finalInd,clickType));
            setSlot(ind,elem);
            k++;
         }
      }
      
      GuiElementBuilder elem = GuiElementBuilder.from(GraphicalItem.with(choices.size() == numChoices ? GraphicItems.CONFIRM : GraphicItems.CANCEL));
      elem.setName(Text.translatable("gui.nations.voucher_go").formatted(Formatting.BOLD,(choices.size() == numChoices ? Formatting.GREEN : Formatting.RED)));
      elem.addLoreLine(Text.translatable("gui.nations.choose_slots",numChoices).formatted(Formatting.DARK_PURPLE));
      elem.setCallback(this::activateCallback);
      setSlot(49,elem);
   }
   
   private void buildRolling(){
      GuiHelper.outlineGUI(9,6,this,animTicks % 10 < 5 ? 0x9a2e44 : 0xe9d73b,Text.empty(),null);
      GuiElementBuilder chip = GuiElementBuilder.from(GraphicalItem.withColor(GraphicItems.CASINO_CHIP, ArcanaColors.EQUAYUS_COLOR)).hideTooltip();
      
      int k = 0;
      int chosenInd = 0;
      for(int i = 0; i < 4; i++){
         for(int j = 0; j < 7; j++){
            int ind = (i*9+10)+j;
            boolean sel = choices.contains(k);
            if(sel){
               int cutoff = 20*chosenInd+1;
               if(endTicks >= cutoff){
                  if(endTicks == cutoff) player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS,1,0.6f + 1.4f*((float) chosenInd / numChoices));
                  setSlot(ind,GuiElementBuilder.from(rewards.get(chosenInd)));
               }else{
                  setSlot(ind,chip);
               }
               chosenInd++;
            }else if(!nonChoices.contains(k)){
               setSlot(ind,GuiElementBuilder.from(GraphicalItem.withColor(GraphicItems.PAGE_BG, ArcanaColors.LIGHT_COLOR)).hideTooltip());
            }else{
               if(Math.random() < 0.1 && animTicks != 0){
                  display.set(k,getWeightedRandomStack());
               }
               setSlot(ind,GuiElementBuilder.from(display.get(k)).hideTooltip());
            }
            k++;
         }
      }
   }
   
   @Override
   public void onTick(){
      if(animTicks >= animDuration()) close();
      if(anim){
         animTicks++;
         
         if(animTicks % 10 == 0 && !nonChoices.isEmpty()){
            Collections.shuffle(nonChoices);
            nonChoices.removeFirst();
         }
         if(nonChoices.isEmpty()){
            endTicks++;
         }
         
         if(animTicks % 5 == 0 && endTicks < 1){
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.PLAYERS,0.5f,1.5f);
         }
         
         buildRolling();
      }
   }
   
   
   
   private void activateCallback(ClickType clickType){
      if(!clickType.isLeft) return;
      if(choices.size() == numChoices && !anim){
         if(!MiscUtils.removeItems(player, NationsRegistry.BUG_VOUCHER_ITEM,1)) return;
         anim = true;
         for(int i = 0; i < 28; i++){
            if(!choices.contains(i)){
               nonChoices.add(i);
            }
         }
         giveRewards();
         buildRolling();
      }else{
         player.playSoundToPlayer(SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS,1,1);
      }
   }
   
   private void chooseCallback(Integer index, ClickType clickType){
      if(clickType.isLeft && !anim){
         if(choices.contains(index)){
            choices.remove(index);
         }else if(choices.size() < numChoices){
            choices.add(index);
         }
      }
      buildChoices();
   }
   
   private void giveRewards(){
      for(int i = 0; i < numChoices; i++){
         rewards.add(getWeightedRandomStack());
      }
      for(int i = 0; i < 28; i++){
         display.add(getWeightedRandomStack());
      }
      
      Nations.addTickTimerCallback(new GenericTimer(animDuration(), () -> MiscUtils.returnItems(new SimpleInventory(rewards.toArray(new ItemStack[0])), player)));
   }
   
   private ItemStack getWeightedRandomStack(){
      int totalWeight = 0;
      for (int w : loot.values()) {
         if (w > 0) totalWeight += w;
      }
      if (totalWeight == 0) {
         return ItemStack.EMPTY;
      }
      
      int pick = player.getRandom().nextInt(totalWeight);
      for (Map.Entry<ItemStack, Integer> entry : loot.entrySet()) {
         int w = entry.getValue();
         if (w <= 0) continue;
         pick -= w;
         if (pick < 0) {
            return entry.getKey().copy();
         }
      }
      
      return ItemStack.EMPTY;
   }
}
