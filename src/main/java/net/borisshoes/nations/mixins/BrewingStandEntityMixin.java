package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.gameplay.NationChunk;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;
import java.util.Optional;

@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandEntityMixin {
   
   @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BrewingStandBlockEntity;canCraft(Lnet/minecraft/recipe/BrewingRecipeRegistry;Lnet/minecraft/util/collection/DefaultedList;)Z"))
   private static boolean nations_stopBrew(boolean original, World world, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity){
      if(!original) return false;
      if(!world.getRegistryKey().equals(ServerWorld.OVERWORLD)) return false;
      ChunkPos chunkPos = new ChunkPos(pos);
      NationChunk nationChunk = Nations.getChunk(chunkPos);
      if(nationChunk == null) return false;
      Nation nation = nationChunk.getControllingNation();
      if(nation == null) return false;
      
      DefaultedList<ItemStack> inventory = blockEntity.inventory;
      BrewingRecipeRegistry brewingRecipeRegistry = world.getBrewingRecipeRegistry();
      for(int i = 0; i < 3; i++){
         ItemStack newPotion = brewingRecipeRegistry.craft(inventory.get(3), inventory.get(i));
         if(newPotion.isEmpty()){
            continue;
         }
         Potion potion = null;
         if(newPotion.contains(DataComponentTypes.POTION_CONTENTS)){
            PotionContentsComponent potionComp = newPotion.get(DataComponentTypes.POTION_CONTENTS);
            potion = potionComp.potion().map(RegistryEntry::value).orElse(null);
         }
         if(potion == null) continue;
         if(!nation.canBrew(newPotion.getItem(),potion)) return false;
      }
      return true;
   }
}
