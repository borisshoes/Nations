package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.arcananovum.ArcanaNovum;
import net.borisshoes.arcananovum.ArcanaRegistry;
import net.borisshoes.arcananovum.cardinalcomponents.IArcanaProfileComponent;
import net.borisshoes.arcananovum.core.ArcanaItem;
import net.borisshoes.arcananovum.items.normal.ArcaneNotesItem;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.borisshoes.nations.gameplay.Nation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArcaneNotesItem.class)
public class ArcaneNotesMixin {

   @Inject(method = "finishUsing", at = @At(value = "INVOKE", target = "Lnet/borisshoes/arcananovum/utils/ParticleEffectUtils;arcaneNotesFinish(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/borisshoes/arcananovum/core/ArcanaItem;)V"))
   private void nations_stopNotesUnlock(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir, @Local ServerPlayerEntity player){
      IArcanaProfileComponent arcanaProfile = ArcanaNovum.data(player);
      INationsProfileComponent profile = Nations.getPlayer(player);
      Nation playerNation = profile.getNation();
      if(playerNation != null){
         for(ArcanaItem arcanaItem : ArcanaRegistry.ARCANA_ITEMS){
            if(playerNation.canCraft(arcanaItem) || arcanaItem.getId().equals(ArcanaRegistry.ARCANE_TOME.getId())){
               arcanaProfile.addResearchedItem(arcanaItem.getId());
            }else{
               arcanaProfile.removeResearchedItem(arcanaItem.getId());
            }
         }
      }
   }
}
