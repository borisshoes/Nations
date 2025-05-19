package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.arcananovum.ArcanaRegistry;
import net.borisshoes.arcananovum.items.AquaticEversource;
import net.borisshoes.nations.land.NationsLand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AquaticEversource.AquaticEversourceItem.class)
public class AquaticEversourceMixin {
   
   @Inject(method = "placeFluid", at = @At("HEAD"), cancellable = true)
   private void nations_stopAquaticEversource(Fluid fluid, PlayerEntity playerEntity, World world, BlockPos pos, BlockHitResult hitResult, boolean flood, CallbackInfoReturnable<Integer> cir){
      if(!(playerEntity instanceof ServerPlayerEntity player)) return;
      if(!NationsLand.canBreakBlock(world,player,pos,true) || !NationsLand.canUseItemOnBlock(world,player,pos,new ItemStack(ArcanaRegistry.AQUATIC_EVERSOURCE.getItem()),true)){
         cir.setReturnValue(0);
      }
   }
   
   @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;canPlayerModifyAt(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;)Z"), cancellable = true)
   private void nations_stopAquaticEversource2(World world, PlayerEntity playerEntity, Hand hand, CallbackInfoReturnable<ActionResult> cir, @Local BlockHitResult bhr){
      if(!(playerEntity instanceof ServerPlayerEntity player)) return;
      if(!NationsLand.canBreakBlock(world,player,bhr.getBlockPos(),true) || !NationsLand.canUseItemOnBlock(world,player,bhr.getBlockPos().offset(bhr.getSide()),playerEntity.getStackInHand(hand),true)){
         cir.setReturnValue(ActionResult.FAIL);
      }
   }
}
