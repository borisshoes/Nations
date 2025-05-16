package net.borisshoes.nations.mixins;

import net.borisshoes.nations.Nations;
import net.borisshoes.nations.land.NationsLand;
import net.minecraft.block.BlockState;
import net.minecraft.block.DragonEggBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DragonEggBlock.class)
public abstract class DragonEggBlockMixin {
   
   @Unique
   private PlayerEntity tempPlayer;
   
   @Inject(method = "onUse", at = @At("HEAD"))
   private void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
      this.tempPlayer = player;
   }
   
   @Inject(method = "onUse", at = @At("RETURN"))
   private void onUseReturn(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
      this.tempPlayer = null;
   }
   
   @Inject(method = "onBlockBreakStart", at = @At("HEAD"))
   private void onAttack(BlockState state, World world, BlockPos pos, PlayerEntity player, CallbackInfo ci) {
      this.tempPlayer = player;
   }
   
   @Inject(method = "onBlockBreakStart", at = @At("RETURN"))
   private void onAttackReturn(BlockState state, World world, BlockPos pos, PlayerEntity player, CallbackInfo ci) {
      this.tempPlayer = null;
   }
   
   @Inject(method = "teleport", at = @At("HEAD"), cancellable = true)
   private void onTeleport(BlockState state, World world, BlockPos pos, CallbackInfo ci) {
      if (this.tempPlayer instanceof ServerPlayerEntity player) {
         if(!NationsLand.canTeleportDragonEgg(world,player,pos, true)){
            ci.cancel();
         }
      }
   }
}
