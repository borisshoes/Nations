package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.Nation;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;
import java.util.Optional;

@Mixin(SmithingScreenHandler.class)
public class SmithingScreenHandlerMixin {
   
   @Final
   @Shadow
   private World world;
   
   @ModifyExpressionValue(method = "updateResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/recipe/ServerRecipeManager;getFirstMatch(Lnet/minecraft/recipe/RecipeType;Lnet/minecraft/recipe/input/RecipeInput;Lnet/minecraft/world/World;)Ljava/util/Optional;"))
   private Optional<RecipeEntry<SmithingRecipe>> nations_stopSmithing(Optional<RecipeEntry<SmithingRecipe>> original, @Local SmithingRecipeInput smithingRecipeInput){
      SmithingScreenHandler handler = (SmithingScreenHandler) (Object) this;
      if(original.isEmpty()) return original;
      Item item = original.get().value().craft(smithingRecipeInput, world.getRegistryManager()).getItem();
      if(!NationsRegistry.LOCKED_ITEMS.containsKey(item)) return original;
      
      PlayerEntity player = handler.player;
      if(!(player instanceof ServerPlayerEntity serverPlayer)) return Optional.empty();
      Nation nation = Nations.getNation(serverPlayer);
      if(nation == null) return Optional.empty();
      return nation.canCraft(item) ? original : Optional.empty();
   }
}
