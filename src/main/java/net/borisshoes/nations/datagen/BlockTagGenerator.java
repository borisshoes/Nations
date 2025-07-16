package net.borisshoes.nations.datagen;

import net.borisshoes.nations.NationsRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class BlockTagGenerator  extends FabricTagProvider<Block> {
   public BlockTagGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture){
      super(output, RegistryKeys.BLOCK, registriesFuture);
   }
   
   @Override
   protected void configure(RegistryWrapper.WrapperLookup lookup){
      getOrCreateTagBuilder(NationsRegistry.INFLUENCE_PROTECTED_BLOCKS);
      
      FabricTagProvider<Block>.FabricTagBuilder claimBlockBuilder = getOrCreateTagBuilder(NationsRegistry.CLAIM_PROTECTED_BLOCKS);
      Registries.BLOCK.forEach(block -> {
         if(block instanceof AnvilBlock || block instanceof BedBlock || block instanceof BeaconBlock || block instanceof DoorBlock ||
            block instanceof FenceGateBlock || block instanceof TrapdoorBlock || block instanceof LeverBlock || block instanceof NoteBlock ||
            block instanceof RepeaterBlock || block instanceof JukeboxBlock || block instanceof AbstractPressurePlateBlock ||
            block instanceof TurtleEggBlock || block instanceof BellBlock || block instanceof CampfireBlock || block instanceof TntBlock ||
            block instanceof ChorusFlowerBlock || block instanceof DecoratedPotBlock || block instanceof FarmlandBlock ||
            block instanceof DaylightDetectorBlock || block instanceof RedstoneWireBlock || block instanceof ButtonBlock){
            claimBlockBuilder.add(block);
         }
      });
      
      getOrCreateTagBuilder(NationsRegistry.DUEL_NO_COPY_BLOCKS)
            .addOptionalTag(BlockTags.AIR)
            .add(Blocks.RESPAWN_ANCHOR)
            .addOptionalTag(BlockTags.BEACON_BASE_BLOCKS)
      ;
   }
   
   @Override
   public String getName(){
      return "Nations - Block Tag Generator";
   }
}
