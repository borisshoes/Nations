package net.borisshoes.nations.mixins;

import net.borisshoes.nations.NationsRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(Block.class)
public class BlockMixin {
   
   @Inject(method="dropStack(Lnet/minecraft/world/World;Ljava/util/function/Supplier;Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
   private static void nations_contestDimNoItemDrops(World world, Supplier<ItemEntity> itemEntitySupplier, ItemStack stack, CallbackInfo ci){
      if(world.getRegistryKey().equals(NationsRegistry.CONTEST_DIM)){
         ci.cancel();
      }
   }
   
   @Inject(method="dropExperience", at = @At("HEAD"), cancellable = true)
   private void nations_contestDimNoXpDrops(ServerWorld world, BlockPos pos, int size, CallbackInfo ci){
      if(world.getRegistryKey().equals(NationsRegistry.CONTEST_DIM)){
         ci.cancel();
      }
   }
}
