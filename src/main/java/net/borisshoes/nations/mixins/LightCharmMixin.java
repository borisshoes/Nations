package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.borisshoes.arcananovum.ArcanaRegistry;
import net.borisshoes.arcananovum.items.charms.LightCharm;
import net.borisshoes.nations.land.NationsLand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightCharm.LightCharmItem.class)
public class LightCharmMixin {
   
   @Inject(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"), cancellable = true)
   private void nations_stopLightCharm1(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir){
      if(!(context.getPlayer() instanceof ServerPlayerEntity player)) return;
      if(!NationsLand.canBreakBlock(context.getWorld(),player,context.getBlockPos(),true) || !NationsLand.canUseItemOnBlock(context.getWorld(),player,context.getBlockPos(),context.getStack(),true)){
         cir.setReturnValue(ActionResult.PASS);
      }
   }
   
   
   @ModifyExpressionValue(method = "inventoryTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isAir()Z"))
   private boolean nations_stopLightCharm2(boolean original, ItemStack stack, World world, Entity entity, int slot, boolean selected){
      if(!original) return false;
      if(!(entity instanceof ServerPlayerEntity player)) return true;
      if(!NationsLand.canBreakBlock(world,player,player.getBlockPos(),true) || !NationsLand.canUseItemOnBlock(world,player,player.getBlockPos(),stack,true)){
         return false;
      }
      return true;
   }
   
   
   @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/borisshoes/arcananovum/items/charms/LightCharm;changeSetting(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/item/ItemStack;)V"), cancellable = true)
   private void nations_stopLightCharm3(World world, PlayerEntity playerEntity, Hand hand, CallbackInfoReturnable<ActionResult> cir){
      if(!(playerEntity instanceof ServerPlayerEntity player)) return;
      if(!NationsLand.canBreakBlock(world,player,player.getBlockPos(),true) || !NationsLand.canUseItemOnBlock(world,player,player.getBlockPos(),player.getStackInHand(hand),true)){
         cir.setReturnValue(ActionResult.PASS);
      }
   }
}
