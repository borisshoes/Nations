package net.borisshoes.nations.mixins;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessor {
   
   @Invoker("dropInventory")
   void dropPlayerInventory(ServerWorld world);
   
   @Invoker("vanishCursedItems")
   void vanishingCurse();
}
