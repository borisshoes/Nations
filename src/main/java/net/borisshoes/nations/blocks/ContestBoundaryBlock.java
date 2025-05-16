package net.borisshoes.nations.blocks;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.packettweaker.PacketContext;

public class ContestBoundaryBlock extends Block implements PolymerBlock {
   public ContestBoundaryBlock(Settings settings){
      super(settings);
   }
   
   @Override
   public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext){
      return Blocks.BLACK_CONCRETE_POWDER.getDefaultState();
   }
   
   @Override
   public Block getPolymerReplacement(PacketContext context){
      return Blocks.BLACK_CONCRETE_POWDER;
   }
   
   @Override
   public boolean handleMiningOnServer(ItemStack tool, BlockState state, BlockPos pos, ServerPlayerEntity player){
      return true;
   }
}
