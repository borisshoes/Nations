package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.arcananovum.ArcanaNovum;
import net.borisshoes.arcananovum.cardinalcomponents.ArcanaProfileComponent;
import net.borisshoes.arcananovum.utils.LevelUtils;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ArcanaProfileComponent.class, remap = false)
public abstract class ArcanaProfileMixin {
   
   @Shadow @Final private PlayerEntity player;
   
   @Shadow public abstract int getBonusSkillPoints();
   
   @Shadow private int xp;
   
   @ModifyReturnValue(method = "getTotalSkillPoints", at = @At("RETURN"))
   private int nations_overrideSkillPoints(int original){
      if(!(player instanceof ServerPlayerEntity serverPlayer)) return original;
      INationsProfileComponent nationsProfile = Nations.getPlayer(serverPlayer);
      if(nationsProfile.getNation() == null) return getBonusSkillPoints();
      return nationsProfile.getNation().getArcanaSkillPoints() + getBonusSkillPoints();
   }
   
   @ModifyReturnValue(method = "getLevel", at = @At("RETURN"))
   private int nations_overrideArcanaLevel(int original){
      if(!(player instanceof ServerPlayerEntity serverPlayer)) return original;
      INationsProfileComponent nationsProfile = Nations.getPlayer(serverPlayer);
      if(nationsProfile.getNation() == null) return original;
      return nationsProfile.getNation().getArcanaLevels();
   }
   
   @ModifyReturnValue(method = "getXP", at = @At("RETURN"))
   private int nations_overrideArcanaXP(int original){
      if(!(player instanceof ServerPlayerEntity serverPlayer)) return original;
      INationsProfileComponent nationsProfile = Nations.getPlayer(serverPlayer);
      if(nationsProfile.getNation() == null) return original;
      return LevelUtils.levelToTotalXp(nationsProfile.getNation().getArcanaLevels());
   }
   
   @Inject(method = "addXP", at = @At("HEAD"), cancellable = true)
   private void nations_overrideArcanaAddXP(int xp, CallbackInfoReturnable<Boolean> cir){
      if(!(player instanceof ServerPlayerEntity serverPlayer)) return;
      INationsProfileComponent nationsProfile = Nations.getPlayer(serverPlayer);
      if(nationsProfile.getNation() == null) return;
      int newXp = LevelUtils.levelToTotalXp(nationsProfile.getNation().getArcanaLevels());
      ArcanaNovum.PLAYER_XP_TRACKER.put(player.getUuid(), newXp);
      this.xp = newXp;
      cir.setReturnValue(true);
   }
}
