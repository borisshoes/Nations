package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.Nation;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
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
      
      if(!NationsRegistry.LOCKED_ITEMS.containsKey(stack.getItem())) return;
      if(!(player instanceof ServerPlayerEntity serverPlayer)) return;
      Nation nation = Nations.getNation(serverPlayer);
      if(nation == null) return;
      boolean canCraft = nation.canCraft(stack.getItem());
      if(!canCraft){
         accessor.getOutput().setStack(0,ItemStack.EMPTY);
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
