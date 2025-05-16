package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.borisshoes.nations.land.BlockBreakAttemptHandler;
import net.borisshoes.nations.land.ItemUseBlockFlags;
import net.borisshoes.nations.land.NationsLand;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin implements ItemUseBlockFlags, BlockBreakAttemptHandler {
   
   @Unique
   private boolean nations_stopInteractBlock;
   @Unique
   private boolean nations_stopInteractItemBlock;
   @Final
   @Shadow
   protected ServerPlayerEntity player;
   @Unique
   private BlockPos nations_blockBreakFail;
   @Unique
   private boolean nations_was_insta_break;
   
   @ModifyVariable(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;copy()Lnet/minecraft/item/ItemStack;"), ordinal = 1)
   private boolean nations_stopBlockUse(boolean orig) {
      if (this.nations_stopInteractBlock)
         return true;
      return orig;
   }
   
   @Inject(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;isCreative()Z"), cancellable = true)
   private void nations_stopItemOnBlock(ServerPlayerEntity serverPlayer, World level, ItemStack itemStack, Hand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> info) {
      if (this.nations_stopInteractItemBlock) {
         info.setReturnValue(ActionResult.PASS);
         info.cancel();
      }
   }
   
   @Inject(method = "update", at = @At("RETURN"))
   public void onTick(CallbackInfo info) {
      if (this.nations_blockBreakFail != null) {
         // All hail 1.20.5 with mining speed attribute!
         if (!this.nations_was_insta_break) {
            this.player.getAttributeInstance(EntityAttributes.BLOCK_BREAK_SPEED).updateModifier(new EntityAttributeModifier(NationsLand.MINING_SPEED_MOD, -1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
         }
      }
   }
   
   @Inject(method = "processBlockBreakingAction", at = @At("HEAD"), cancellable = true)
   private void onBlockBreakAction(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, int sequence, CallbackInfo ci) {
      if (action == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK && pos.equals(this.nations_blockBreakFail)) {
         this.nations_blockBreakFail = null;
         this.player.getAttributeInstance(EntityAttributes.BLOCK_BREAK_SPEED).removeModifier(NationsLand.MINING_SPEED_MOD);
         ci.cancel();
      }
   }
   
   @Override
   public void stopCanUseBlocks(boolean flag) {
      this.nations_stopInteractBlock = flag;
   }
   
   @Override
   public void stopCanUseItems(boolean flag) {
      this.nations_stopInteractItemBlock = flag;
   }
   
   @Override
   public boolean allowUseBlocks() {
      return !this.nations_stopInteractBlock;
   }
   
   @Override
   public boolean allowUseItems() {
      return !this.nations_stopInteractItemBlock;
   }
   
   /**
    * Disable mismatched block warning if the cause was due to claim prevention
    */
   @WrapWithCondition(
         method = "processBlockBreakingAction",
         at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false)
   )
   private boolean shouldWarn(Logger logger, String warn, Object obj, Object obj2) {
      return !this.wasInstabreak() || this.failedPos() == null;
   }
   
   @Override
   public void setBlockBreakAttemptFail(BlockPos pos, boolean instaBreak) {
      this.nations_was_insta_break = instaBreak;
      this.nations_blockBreakFail = pos;
      if (this.nations_blockBreakFail == null) {
         this.player.getAttributeInstance(EntityAttributes.BLOCK_BREAK_SPEED).removeModifier(NationsLand.MINING_SPEED_MOD);
      }
   }
   
   @Override
   public BlockPos failedPos() {
      return this.nations_blockBreakFail;
   }
   
   @Override
   public boolean wasInstabreak() {
      return this.nations_was_insta_break;
   }
}
