package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.arcananovum.blocks.forge.TwilightAnvilBlockEntity;
import net.borisshoes.arcananovum.utils.ArcanaItemUtils;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.Nation;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.ForgingSlotsManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public class AnvilScreenHandlerMixin {
   
   @Inject(method = "updateResult", at = @At("RETURN"))
   private void nations_stopAnvilUsage(CallbackInfo ci){
      AnvilScreenHandler handler = (AnvilScreenHandler) (Object) this;
      ForgingScreenHandlerAccessor accessor = (ForgingScreenHandlerAccessor) handler;
      ItemStack stack = accessor.getOutput().getStack(0);
      PlayerEntity player = accessor.getPlayer();
      
      boolean isArcane = ArcanaItemUtils.isArcane(stack);
      if(!NationsRegistry.LOCKED_ITEMS.containsKey(stack.getItem()) && !isArcane) return;
      if(!(player instanceof ServerPlayerEntity serverPlayer)) return;
      Nation nation = Nations.getNation(serverPlayer);
      if(nation == null) return;
      boolean canCraft = nation.canCraft(stack.getItem());
      if(!canCraft){
         accessor.getOutput().setStack(0,ItemStack.EMPTY);
      }else{
         if(isArcane){
            if(!nation.canCraft(ArcanaItemUtils.identifyItem(stack))){
               accessor.getOutput().setStack(0,ItemStack.EMPTY);
               return;
            }
            ItemStack arcaneStack = ArcanaItemUtils.isArcane(accessor.getInput().getStack(0)) ? accessor.getInput().getStack(0) : accessor.getInput().getStack(1);
            ItemEnchantmentsComponent startingEnchants = arcaneStack.getEnchantments();
            ItemEnchantmentsComponent endingEnchantments = stack.getEnchantments();
            
            for(RegistryEntry<Enchantment> enchantment : endingEnchantments.getEnchantments()){
               if(startingEnchants.getLevel(enchantment) >= endingEnchantments.getLevel(enchantment)) continue;
               if(!nation.canEnchant(enchantment.getKey().get(),endingEnchantments.getLevel(enchantment))){
                  accessor.getOutput().setStack(0,ItemStack.EMPTY);
                  return;
               }
            }
         }else{
            ItemEnchantmentsComponent enchants = stack.getEnchantments();
            for(RegistryEntry<Enchantment> enchantment : enchants.getEnchantments()){
               if(!nation.canEnchant(enchantment.getKey().get(),enchants.getLevel(enchantment))){
                  accessor.getOutput().setStack(0,ItemStack.EMPTY);
                  return;
               }
            }
         }
      }
   }
}
