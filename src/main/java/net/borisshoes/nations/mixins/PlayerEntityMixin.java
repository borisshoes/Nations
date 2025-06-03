package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.borisshoes.nations.Nations;
import net.borisshoes.nations.cca.INationsProfileComponent;
import net.borisshoes.nations.gameplay.Nation;
import net.borisshoes.nations.land.NationsLand;
import net.borisshoes.nations.utils.MiscUtils;
import net.borisshoes.nations.utils.NationsColors;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.borisshoes.nations.cca.PlayerComponentInitializer.PLAYER_DATA;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
   
   @ModifyExpressionValue(method = "getDisplayName", at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/Team;decorateName(Lnet/minecraft/scoreboard/AbstractTeam;Lnet/minecraft/text/Text;)Lnet/minecraft/text/MutableText;"))
   private MutableText nations_replaceName(MutableText original){
      PlayerEntity player = (PlayerEntity) (Object) this;
      if(player instanceof ServerPlayerEntity serverPlayer){
         INationsProfileComponent data = Nations.getPlayer(serverPlayer);
         Nation nation = data.getNation();
         String nick = data.getNickname();
         nick = nick.isBlank() ? player.getName().getString() : nick;
         
         if(nation == null){
            return Text.literal(nick).setStyle(original.getStyle());
         }else{
            return nation.getFormattedNameTag(true).append(Text.literal(nick).withColor(nation.getTextColorSub()));
         }
      }
      return original;
   }
   
   @ModifyExpressionValue(method = "dropInventory", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameRules;getBoolean(Lnet/minecraft/world/GameRules$Key;)Z"))
   private boolean nations_keepInventoryDropInventory(boolean original, ServerWorld world){
      if(original) return true;
      PlayerEntity p = (PlayerEntity) (Object) this;
      if(!(p instanceof ServerPlayerEntity player)) return original;
      return NationsLand.shouldKeepInventory(world.getRegistryKey(),new ChunkPos(player.getBlockPos()),player);
   }
   
   @ModifyExpressionValue(method = "getExperienceToDrop", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameRules;getBoolean(Lnet/minecraft/world/GameRules$Key;)Z"))
   private boolean nations_keepInventoryDropExperience(boolean original, ServerWorld world){
      if(original) return true;
      PlayerEntity p = (PlayerEntity) (Object) this;
      if(!(p instanceof ServerPlayerEntity player)) return original;
      return NationsLand.shouldKeepInventory(world.getRegistryKey(),new ChunkPos(player.getBlockPos()),player);
   }
   
   @Inject(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;modifyAppliedDamage(Lnet/minecraft/entity/damage/DamageSource;F)F", shift = At.Shift.AFTER))
   private void nations_applyCombatLog(ServerWorld world, DamageSource source, float amount, CallbackInfo ci){
      PlayerEntity p = (PlayerEntity) (Object) this;
      if(!(p instanceof ServerPlayerEntity player)) return;
      if(source.getAttacker() instanceof ServerPlayerEntity attacker){
         Nations.getPlayer(player).resetCombatLog(attacker);
         Nations.getPlayer(attacker).resetCombatLog(player);
      }
   }
}
