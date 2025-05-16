package net.borisshoes.nations.datagen;

import net.borisshoes.nations.NationsRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Blocks;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;

import java.util.concurrent.CompletableFuture;

public class ItemTagGenerator  extends FabricTagProvider<Item> {
   public ItemTagGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture){
      super(output, RegistryKeys.ITEM, registriesFuture);
   }
   
   @Override
   protected void configure(RegistryWrapper.WrapperLookup lookup){
      getOrCreateTagBuilder(NationsRegistry.INFLUENCE_PROTECTED_ITEMS)
            .add(Items.END_CRYSTAL)
      ;
      
      FabricTagProvider<Item>.FabricTagBuilder claimItemBuilder = getOrCreateTagBuilder(NationsRegistry.CLAIM_PROTECTED_ITEMS)
            .add(Items.LILY_PAD)
      ;
      Registries.ITEM.forEach(item -> {
         if(item instanceof BucketItem || item instanceof BoneMealItem || item instanceof BrushItem || item instanceof BoatItem){
            claimItemBuilder.add(item);
         }
      });
   }
   
   @Override
   public String getName(){
      return "Nations - Item Tag Generator";
   }
}
