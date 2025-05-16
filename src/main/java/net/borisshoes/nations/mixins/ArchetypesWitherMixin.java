package net.borisshoes.nations.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.borisshoes.ancestralarchetypes.callbacks.EntityAttackedCallback;
import net.borisshoes.nations.land.NationsLand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(EntityAttackedCallback.class)
public class ArchetypesWitherMixin {
   
   @ModifyExpressionValue(method = "attackedEntity", at = @At(value = "INVOKE", target = "Lnet/borisshoes/ancestralarchetypes/cca/IArchetypeProfile;hasAbility(Lnet/borisshoes/ancestralarchetypes/ArchetypeAbility;)Z"))
   private static boolean nations_stopWitherGlitch(boolean original, PlayerEntity playerEntity, World world, Hand hand, Entity entity, EntityHitResult entityHitResult){
      if(!original) return false;
      if(playerEntity instanceof ServerPlayerEntity player && entity instanceof LivingEntity living){
         return NationsLand.canDamage(player,living,false);
      }
      return true;
   }
}
