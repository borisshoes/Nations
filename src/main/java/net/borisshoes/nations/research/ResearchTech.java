package net.borisshoes.nations.research;

import net.borisshoes.arcananovum.core.ArcanaItem;
import net.borisshoes.arcananovum.gui.midnightenchanter.MidnightEnchanterGui;
import net.borisshoes.arcananovum.utils.ArcanaItemUtils;
import net.borisshoes.nations.NationsConfig;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.utils.MiscUtils;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;

import java.util.*;
import java.util.function.Supplier;

public class ResearchTech {
   
   public static final Comparator<ResearchTech> COMPARATOR =
         Comparator.comparingInt((ResearchTech r) -> {
            if (r.isMainTree() && !r.isBuff()) return 0;
            if (r.isBuff())                    return 1;
            if (r.isArcanaItem())              return 2;
            if (r.isPotion())                  return 3;
            if (r.isEnchant())                 return 4;
            return 5;
         }).thenComparing(ResearchTech::getTier).thenComparing(r -> r.getName().getString(), String.CASE_INSENSITIVE_ORDER);
   
   private final RegistryKey<ResearchTech> key;
   private final int tier;
   private final RegistryKey<ResearchTech>[] prereqs;
   private final Supplier<Integer> costGetter;
   private final Supplier<Integer> rateGetter;
   private final ArrayList<Item> craftLocked = new ArrayList<>();
   private final ArrayList<ArcanaItem> arcanaLock = new ArrayList<>();
   private final ArrayList<Pair<RegistryKey<Enchantment>, Integer>> enchantLocked = new ArrayList<>();
   private final ArrayList<RegistryEntry<Potion>> potionLocked = new ArrayList<>();
   private RegistryKey<ResearchTech> buff = null;
   private ItemStack showStack;
   
   public ResearchTech(RegistryKey<ResearchTech> key, int tier, RegistryKey<ResearchTech>[] prereqs, Supplier<Integer> costGetter, Supplier<Integer> rateGetter){
      this.key = key;
      this.tier = tier;
      this.prereqs = prereqs;
      this.costGetter = costGetter;
      this.rateGetter = rateGetter;
   }
   
   public ResearchTech(RegistryKey<ResearchTech> key, int tier, RegistryKey<ResearchTech>[] prereqs, NationsConfig.ConfigSetting<?> costConfig, NationsConfig.ConfigSetting<?> rateConfig){
      this.key = key;
      this.tier = tier;
      this.prereqs = prereqs;
      this.costGetter = () -> NationsConfig.getInt(costConfig);
      this.rateGetter = () -> NationsConfig.getInt(rateConfig);
   }
   
   public ResearchTech addArcanaLock(ArcanaItem... items){
      arcanaLock.addAll(Arrays.asList(items));
      return this;
   }
   
   public ResearchTech addCraftLock(Item... items){
      craftLocked.addAll(Arrays.asList(items));
      return this;
   }
   
   public ResearchTech addEnchantLock(Pair<RegistryKey<Enchantment>, Integer>... enchantments){
      enchantLocked.addAll(Arrays.asList(enchantments));
      return this;
   }
   
   public ResearchTech addPotionLock(RegistryEntry<Potion>... potions){
      potionLocked.addAll(Arrays.asList(potions));
      return this;
   }
   
   public ResearchTech setBuff(RegistryKey<ResearchTech> buff){
      this.buff = buff;
      return this;
   }
   
   public ResearchTech withShowStack(ItemStack stack){
      this.showStack = stack;
      return this;
   }
   
   public ResearchTech withShowStack(Item item){
      this.showStack = new ItemStack(item);
      return this;
   }
   
   public RegistryKey<ResearchTech> getKey(){
      return key;
   }
   
   public int getConsumptionRate(){
      return rateGetter.get();
   }
   
   public int getCost(){
      return costGetter.get();
   }
   
   public RegistryKey<ResearchTech>[] getPrereqs(){
      return prereqs;
   }
   
   public boolean hasPrereq(ResearchTech tech){
      for(RegistryKey<ResearchTech> prereq : prereqs){
         if(prereq.equals(tech.key)){
            return true;
         }
      }
      return false;
   }
   
   public int getTier(){
      if(tier != -1){
         return tier;
      }
      
      int maxFound = 0;
      for(RegistryKey<ResearchTech> preKey : prereqs){
         ResearchTech prereq = NationsRegistry.RESEARCH.get(preKey);
         if(prereq != null){
            int preTier = prereq.getTier();
            if(preTier > maxFound){
               maxFound = preTier;
            }
         }
      }
      return maxFound;
   }
   
   public int getRawTier(){
      return tier;
   }
   
   public List<ResearchTech> missingPrereqs(List<ResearchTech> researched){
      List<ResearchTech> missing = new ArrayList<>();
      for(RegistryKey<ResearchTech> prereq : prereqs){
         boolean found = false;
         for(ResearchTech researchTech : researched){
            if(researchTech.key.equals(prereq)){
               found = true;
               break;
            }
         }
         if(!found){
            ResearchTech tech = NationsRegistry.RESEARCH.get(prereq);
            if(tech != null) missing.add(tech);
         }
      }
      return missing;
   }
   
   public MutableText getName(){
      try{
         if(NationsRegistry.ENCHANT_TECHS.containsValue(key)){
            Optional<Map.Entry<Pair<RegistryKey<Enchantment>, Integer>, RegistryKey<ResearchTech>>> opt = NationsRegistry.ENCHANT_TECHS.entrySet().stream().filter(entry -> entry.getValue().equals(key)).findFirst();
            RegistryEntry<Enchantment> enchant = MiscUtils.getEnchantment(opt.get().getKey().getLeft());
            int level = opt.get().getKey().getRight();
            return Text.literal("").append(Enchantment.getName(enchant,level));
         }
         if(NationsRegistry.ARCANA_TECHS.containsValue(key)){
            return Text.translatable(ArcanaItemUtils.getItemFromId(key.getValue().getPath()).getItem().getTranslationKey());
         }
         if(NationsRegistry.POTION_TECHS.containsValue(key)){
            Optional<Map.Entry<Potion, RegistryKey<ResearchTech>>> opt = NationsRegistry.POTION_TECHS.entrySet().stream().filter(entry -> entry.getValue().equals(key)).findFirst();
            return Text.translatable("item.minecraft.potion.effect."+opt.get().getKey().getBaseName());
         }
      }catch(Exception e){
         return Text.translatable("research.nations."+key.getValue().getPath());
      }
      return Text.translatable("research.nations."+key.getValue().getPath());
   }
   
   public boolean isMainTree(){
      return !isEnchant() && !isArcanaItem() && !isPotion();
   }
   
   public boolean isEnchant(){
      return NationsRegistry.ENCHANT_TECHS.containsValue(key);
   }
   
   public boolean isArcanaItem(){
      return NationsRegistry.ARCANA_TECHS.containsValue(key);
   }
   
   public boolean isPotion(){
      return NationsRegistry.POTION_TECHS.containsValue(key);
   }
   
   public boolean isBuff(){
      return buff != null;
   }
   
   public RegistryKey<ResearchTech> getBuff(){
      return buff;
   }
   
   public String getId(){
      return key.getValue().getPath();
   }
   
   public ArrayList<Item> getCraftLocked(){
      return craftLocked;
   }
   
   public ArrayList<RegistryEntry<Potion>> getPotionLocked(){
      return potionLocked;
   }
   
   public ArrayList<ArcanaItem> getArcanaLocked(){
      return arcanaLock;
   }
   
   public ArrayList<Pair<RegistryKey<Enchantment>, Integer>> getEnchantLocked(){
      return enchantLocked;
   }
   
   public ItemStack getShowItem(){
      if(showStack != null) return showStack;
      if(isArcanaItem()){
         ArcanaItem item = NationsRegistry.ARCANA_TECHS.entrySet().stream().filter(entry -> entry.getValue().equals(key)).findFirst().get().getKey();
         return item.getPrefItemNoLore();
      }else if(isEnchant()){
         RegistryKey<Enchantment> enchKey = NationsRegistry.ENCHANT_TECHS.entrySet().stream().filter(entry -> entry.getValue().equals(key)).findFirst().get().getKey().getLeft();
         return new ItemStack(NationsRegistry.ENCHANT_ITEM_MAP.get(enchKey));
      }else if(isPotion()){
         Potion potion = NationsRegistry.POTION_TECHS.entrySet().stream().filter(entry -> entry.getValue().equals(key)).findFirst().get().getKey();
         return PotionContentsComponent.createStack(Items.POTION,Registries.POTION.getEntry(potion));
      }
      return new ItemStack(Items.LAPIS_LAZULI);
   }
   
   @Override
   public boolean equals(Object o){
      if(this == o) return true;
      if(!(o instanceof ResearchTech that)) return false;
      return Objects.equals(key, that.key);
   }
   
   @Override
   public int hashCode(){
      return Objects.hashCode(key);
   }
}
