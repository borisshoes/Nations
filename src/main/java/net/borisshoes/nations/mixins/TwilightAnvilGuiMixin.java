package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.arcananovum.blocks.forge.TwilightAnvilBlockEntity;
import net.borisshoes.arcananovum.gui.twilightanvil.TwilightAnvilGui;
import net.borisshoes.arcananovum.utils.ArcanaItemUtils;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.Nation;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TwilightAnvilGui.class)
public class TwilightAnvilGuiMixin {
   
   @ModifyExpressionValue(method = "onAnyClick", at = @At(value = "INVOKE", target = "Lnet/borisshoes/arcananovum/blocks/forge/TwilightAnvilBlockEntity;calculateOutput(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Lnet/borisshoes/arcananovum/blocks/forge/TwilightAnvilBlockEntity$AnvilOutputSet;"))
   private TwilightAnvilBlockEntity.AnvilOutputSet nations_stopTwilightAnvilUsage(TwilightAnvilBlockEntity.AnvilOutputSet original){
      ItemStack stack = original.output();
      PlayerEntity player = ((TwilightAnvilGui) (Object) this).getPlayer();
      ItemStack input1 = original.input1();
      ItemStack input2 = original.input2();
      
      boolean isArcane = ArcanaItemUtils.isArcane(stack);
      if(!NationsRegistry.LOCKED_ITEMS.containsKey(stack.getItem()) && !isArcane) return original;
      if(!(player instanceof ServerPlayerEntity serverPlayer)) return original;
      Nation nation = Nations.getNation(serverPlayer);
      if(nation == null) return original;
      boolean canCraft = nation.canCraft(stack.getItem());
      if(!canCraft){
         return new TwilightAnvilBlockEntity.AnvilOutputSet(input1, input2, ItemStack.EMPTY, 0, 0);
      }else{
         if(isArcane){
            if(!nation.canCraft(ArcanaItemUtils.identifyItem(stack)))
               return new TwilightAnvilBlockEntity.AnvilOutputSet(input1, input2, ItemStack.EMPTY, 0, 0);
            ItemStack arcaneStack = ArcanaItemUtils.isArcane(input1) ? input1 : input2;
            ItemEnchantmentsComponent startingEnchants = arcaneStack.getEnchantments();
            ItemEnchantmentsComponent endingEnchantments = stack.getEnchantments();
            
            for(RegistryEntry<Enchantment> enchantment : endingEnchantments.getEnchantments()){
               if(startingEnchants.getLevel(enchantment) >= endingEnchantments.getLevel(enchantment)){
                  continue;
               }
               if(!nation.canEnchant(enchantment.getKey().get(),endingEnchantments.getLevel(enchantment))){
                  return new TwilightAnvilBlockEntity.AnvilOutputSet(input1, input2, ItemStack.EMPTY, 0, 0);
               }
            }
         }else{
            ItemEnchantmentsComponent enchants = stack.getEnchantments();
            for(RegistryEntry<Enchantment> enchantment : enchants.getEnchantments()){
               if(!nation.canEnchant(enchantment.getKey().get(),enchants.getLevel(enchantment))){
                  return new TwilightAnvilBlockEntity.AnvilOutputSet(input1, input2, ItemStack.EMPTY, 0, 0);
               }
            }
         }
      }
      return original;
   }
   
   @ModifyExpressionValue(method = "redrawGui", at = @At(value = "INVOKE", target = "Lnet/borisshoes/arcananovum/blocks/forge/TwilightAnvilBlockEntity;calculateOutput(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Lnet/borisshoes/arcananovum/blocks/forge/TwilightAnvilBlockEntity$AnvilOutputSet;"))
   private TwilightAnvilBlockEntity.AnvilOutputSet nations_stopTwilightAnvilUsage2(TwilightAnvilBlockEntity.AnvilOutputSet original){
      ItemStack stack = original.output();
      PlayerEntity player = ((TwilightAnvilGui) (Object) this).getPlayer();
      ItemStack input1 = original.input1();
      ItemStack input2 = original.input2();
      
      boolean isArcane = ArcanaItemUtils.isArcane(stack);
      if(!NationsRegistry.LOCKED_ITEMS.containsKey(stack.getItem()) && !isArcane) return original;
      if(!(player instanceof ServerPlayerEntity serverPlayer)) return original;
      Nation nation = Nations.getNation(serverPlayer);
      if(nation == null) return original;
      boolean canCraft = nation.canCraft(stack.getItem());
      if(!canCraft){
         return new TwilightAnvilBlockEntity.AnvilOutputSet(input1, input2, ItemStack.EMPTY, 0, 0);
      }else{
         if(isArcane){
            if(!nation.canCraft(ArcanaItemUtils.identifyItem(stack)))
               return new TwilightAnvilBlockEntity.AnvilOutputSet(input1, input2, ItemStack.EMPTY, 0, 0);
            ItemStack arcaneStack = ArcanaItemUtils.isArcane(input1) ? input1 : input2;
            ItemEnchantmentsComponent startingEnchants = arcaneStack.getEnchantments();
            ItemEnchantmentsComponent endingEnchantments = stack.getEnchantments();
            
            for(RegistryEntry<Enchantment> enchantment : endingEnchantments.getEnchantments()){
               if(startingEnchants.getLevel(enchantment) >= endingEnchantments.getLevel(enchantment)){
                  continue;
               }
               if(!nation.canEnchant(enchantment.getKey().get(),endingEnchantments.getLevel(enchantment))){
                  return new TwilightAnvilBlockEntity.AnvilOutputSet(input1, input2, ItemStack.EMPTY, 0, 0);
               }
            }
         }else{
            ItemEnchantmentsComponent enchants = stack.getEnchantments();
            for(RegistryEntry<Enchantment> enchantment : enchants.getEnchantments()){
               if(!nation.canEnchant(enchantment.getKey().get(),enchants.getLevel(enchantment))){
                  return new TwilightAnvilBlockEntity.AnvilOutputSet(input1, input2, ItemStack.EMPTY, 0, 0);
               }
            }
         }
      }
      return original;
   }
}
