package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.Nation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(CraftingScreenHandler.class)
public class CraftingScreenHandlerMixin {
   
   @ModifyExpressionValue(method = "updateResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isItemEnabled(Lnet/minecraft/resource/featuretoggle/FeatureSet;)Z"))
   private static boolean nations_stopCraftingRecipe(boolean original, ScreenHandler handler, ServerWorld world, PlayerEntity player, RecipeInputInventory craftingInventory, CraftingResultInventory resultInventory, @Nullable RecipeEntry<CraftingRecipe> recipe, @Local(ordinal = 1) ItemStack stack){
      if(!original) return false;
      if(!NationsRegistry.LOCKED_ITEMS.containsKey(stack.getItem())) return true;
      if(!(player instanceof ServerPlayerEntity serverPlayer)) return false;
      Nation nation = Nations.getNation(serverPlayer);
      if(nation == null) return false;
      return nation.canCraft(stack.getItem());
   }
}
