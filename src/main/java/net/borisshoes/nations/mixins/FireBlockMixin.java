package net.borisshoes.nations.mixins;

import net.borisshoes.nations.land.WorldEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireBlock.class)
public class FireBlockMixin {
   
   @Inject(method = "trySpreadingFire", at = @At(value = "HEAD"), cancellable = true)
   private void spread(World world, BlockPos pos, int spreadFactor, Random rand, int currentAge, CallbackInfo info) {
      if (!world.isClient && !WorldEvents.canFireSpread((ServerWorld) world, pos)) {
         info.cancel();
      }
   }
   
   @Inject(method = "scheduledTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;getGameRules()Lnet/minecraft/world/GameRules;"), cancellable = true)
   private void tick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
      if (!WorldEvents.canFireSpread(world, pos)) {
         ci.cancel();
      }
   }
   
   @Inject(method = "getBurnChance(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;)I", at = @At(value = "HEAD"), cancellable = true)
   private void burn(WorldView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
      if (world instanceof ServerWorld serverWorld && !WorldEvents.canFireSpread(serverWorld, pos)) {
         cir.setReturnValue(0);
         cir.cancel();
      }
   }
}
