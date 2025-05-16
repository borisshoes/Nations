package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.NationsRegistry;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.NationChunk;
import net.minecraft.block.BlockState;
import net.minecraft.block.CrafterBlock;
import net.minecraft.block.SmithingTableBlock;
import net.minecraft.item.Item;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(CrafterBlock.class)
public class CrafterBlockMixin {
   
   @ModifyExpressionValue(method = "craft", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/CrafterBlock;getCraftingRecipe(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/recipe/input/CraftingRecipeInput;)Ljava/util/Optional;"))
   private Optional<RecipeEntry<CraftingRecipe>> nations_stopCrafter(Optional<RecipeEntry<CraftingRecipe>> original, BlockState state, ServerWorld world, BlockPos pos, @Local CraftingRecipeInput input){
      if(original.isEmpty()) return original;
      Item item = original.get().value().craft(input, world.getRegistryManager()).getItem();
      if(!NationsRegistry.LOCKED_ITEMS.containsKey(item)) return original;
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return Optional.empty();
      ChunkPos chunkPos = new ChunkPos(pos);
      NationChunk nationChunk = Nations.getChunk(chunkPos);
      if(nationChunk == null) return Optional.empty();
      Nation nation = nationChunk.getControllingNation();
      if(nation == null) return Optional.empty();
      return nation.canCraft(item) ? original : Optional.empty();
   }
   
}
