package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.arcananovum.gui.midnightenchanter.MidnightEnchanterGui;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.gameplay.Nation;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(MidnightEnchanterGui.class)
public class MidnightEnchanterGuiMixin{
   
   
   @ModifyExpressionValue(method = "generateEnchantments", at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/entry/RegistryEntryList$Named;stream()Ljava/util/stream/Stream;"))
   private Stream<RegistryEntry<Enchantment>> nations_modifyEnchanterRandomEnchants(Stream<RegistryEntry<Enchantment>> original){
      MidnightEnchanterGui gui = (MidnightEnchanterGui) (Object) this;
      ServerPlayerEntity player = gui.getPlayer();
      Nation nation = Nations.getNation(player);
      if(nation == null) return new ArrayList<RegistryEntry<Enchantment>>().stream();
      return original.filter(entry -> nation.canEnchant(entry.getKey().get(), 1));
   }
   
   @ModifyExpressionValue(method = "generateEnchantments", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;generateEnchantments(Lnet/minecraft/util/math/random/Random;Lnet/minecraft/item/ItemStack;ILjava/util/stream/Stream;)Ljava/util/List;"))
   private List<EnchantmentLevelEntry> nations_modifyEnchanterRandomEnchantLevels(List<EnchantmentLevelEntry> original){
      MidnightEnchanterGui gui = (MidnightEnchanterGui) (Object) this;
      ServerPlayerEntity player = gui.getPlayer();
      Nation nation = Nations.getNation(player);
      if(nation == null) return new ArrayList<>();;
      List<EnchantmentLevelEntry> newList = new ArrayList<>();
      
      for(EnchantmentLevelEntry entry : original){
         for(int i = entry.level; i >= 1; i--){
            if(nation.canEnchant(entry.enchantment.getKey().get(),i)){
               newList.add(new EnchantmentLevelEntry(entry.enchantment,i));
               break;
            }
         }
      }
      return newList;
   }
   
   @ModifyReturnValue(method = "getEnchantsForItem", at = @At("RETURN"))
   private List<EnchantEntryAccessor> nations_modifyEnchanterOptions(List<EnchantEntryAccessor> original){
      MidnightEnchanterGui gui = (MidnightEnchanterGui) (Object) this;
      ServerPlayerEntity player = gui.getPlayer();
      Nation nation = Nations.getNation(player);
      if(nation == null) return new ArrayList<>();
      return original.stream().filter(entry -> nation.canEnchant(entry.getEnchantment().getKey().get(),entry.getLevel())).collect(Collectors.toCollection(ArrayList::new));
   }
}
