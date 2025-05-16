package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.nations.NationsRegistry;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeUnlocker;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RecipeUnlocker.class)
public interface RecipeUnlockerMixin {
   
   @ModifyReturnValue(method = "shouldCraftRecipe", at = @At("RETURN"))
   private boolean nations_stopCraftingRecipe(boolean original, ServerPlayerEntity player, RecipeEntry<CraftingRecipe> recipeEntry){
      if(!original) return false;
      
      return true;
   }
}
